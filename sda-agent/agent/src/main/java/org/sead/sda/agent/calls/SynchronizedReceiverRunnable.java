/*
 * Copyright 2015 The Trustees of Indiana University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author luoyu@indiana.edu
 * @author isuriara@indiana.edu
 */

package org.sead.sda.agent.calls;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.sead.nds.repository.C3PRPubRequestFacade;
import org.sead.sda.agent.apicalls.NewOREmap;
import org.sead.sda.agent.apicalls.Shimcalls;
import org.sead.sda.agent.driver.ROPublisher;
import org.sead.sda.agent.engine.PropertiesReader;

public class SynchronizedReceiverRunnable implements Runnable {

    private static final Logger log = Logger.getLogger(SynchronizedReceiverRunnable.class);

    public void run() {

        while (true) {
            Shimcalls call = new Shimcalls();
            JSONArray newResearchObjects = call.getResearchObjectsList();

            for (Object item : newResearchObjects.toArray()) {
                try {
                    JSONObject researchObject = (JSONObject) item;
                    call.getObjectID(researchObject, "Identifier");
                    String identifier = call.getID();

                    if (identifier == null) {
                        throw new Exception("SDA Agent : Cannot get Identifier of RO");
                    }

                    if (!isAlreadyPublished(researchObject)) {
                        log.info("New Research Object found, ID: " + identifier);
                        log.info("Starting to publish Research Object...");

                        JSONObject publicationRequest = call.getResearchObject(identifier);

                        // TODO: Finalize the policy for Azure and add it later..
                        // check whether RO adheres to the SDA policy, if not reject
//                        EnforcementResult enforcementResult = PolicyEnforcer.getInstance().isROAllowed(publicationRequest);
//                        if (!enforcementResult.isROAllowed()) {
//                            call.updateStatus(C3PRPubRequestFacade.FAILURE_STAGE, enforcementResult.getC3prUpdateMessage(), identifier);
//                            log.info("Rejected RO: " + identifier + ", message: " + enforcementResult.getC3prUpdateMessage());
//                            continue;
//                        }
//                        log.info("Policy validation passed, id: " + identifier);

                        deposit(publicationRequest, call, identifier);
                        log.info("Successfully published Research Object: " + identifier + "\n");
                    }
                } catch (Exception e) {
                    log.info("ERROR: Error while publishing Research Object...");
                    e.printStackTrace();
                }
                try {
                    // wait between 2 RO publishes
                    Thread.sleep(PropertiesReader.roPublishInterval * 1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            try {
                // wait between 2 fetches from API
                Thread.sleep(PropertiesReader.roFetchInterval * 1000);
            } catch (InterruptedException e) {
                // ignore
            }

        }
    }

    // Deposits each file in the RO into Azure after assigning a PID.
    private void deposit(JSONObject pulishObject, Shimcalls call, String identifier) throws Exception {
        call.getObjectID(pulishObject, "@id");
        String oreUrl = call.getID();

        if (oreUrl == null) {
            throw new Exception("SDA Agent : Cannot get ORE map file of RO");
        }
        log.info("Fetching ORE from: " + oreUrl);
        JSONObject originalORE = call.getResearchObjectORE(oreUrl);
        NewOREmap oreMap = new NewOREmap(originalORE);
        JSONObject newORE = oreMap.getNewOREmap();

        log.info("Downloading data files...");
//        JSONObject preferences = (JSONObject) pulishObject.get("Preferences");
//        String license = null;
//        if (preferences.containsKey("License")){
//            license = preferences.get("License").toString();
//        }
//        ROPublisher localFileCache = new ROPublisher(newOREmap, call.getJsonORE(oreUrl), doiUrl, license);
        ROPublisher roPublisher = new ROPublisher(newORE, originalORE);
        if (roPublisher.getErrorLinks().size() > 0) {
            throw new Exception("Error while downloading some/all files");
        }

        log.info("Updating status in C3P-R with the PID: " + roPublisher.getROPid());
        call.updateStatus(C3PRPubRequestFacade.SUCCESS_STAGE, roPublisher.getROPid(), identifier);
        roPublisher.cleanup();
    }

    private boolean isAlreadyPublished(JSONObject researchObject) {
        Object statusObj = researchObject.get("Status");
        if (statusObj != null) {
            JSONArray statusArray = (JSONArray) statusObj;
            for (Object status : statusArray) {
                if (status instanceof JSONObject) {
                    JSONObject statusJson = (JSONObject) status;
                    String stage = statusJson.get("stage").toString();
                    if ("Success".equals(stage)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

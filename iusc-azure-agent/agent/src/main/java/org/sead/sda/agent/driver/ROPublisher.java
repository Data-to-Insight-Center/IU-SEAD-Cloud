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

package org.sead.sda.agent.driver;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sead.sda.agent.Util;
import org.sead.sda.agent.engine.PropertiesReader;

public class ROPublisher {

    private static final Logger log = Logger.getLogger(ROPublisher.class);

    private ArrayList<String> errorLinks;
    private String rootPath = null;
    private String roPid = null;

    public String getROPid() {
        return roPid;
    }

    public ROPublisher(JSONObject newORE, JSONObject originalORE) {

        this.errorLinks = new ArrayList<String>();
        this.rootPath = createRootFolder(newORE, PropertiesReader.localFileCache);

        JSONArray aggre = (JSONArray) newORE.get("aggregates");

        UUID roId = UUID.randomUUID();
//        List<String> pidList = download(aggre, this.rootPath, roId);
//        String roDescriptionPath = createRODescription(pidList, originalORE, roId.toString());
        download(aggre, this.rootPath, roId);
        // create root pid
        UUID handleSuffix = UUID.randomUUID();
        roPid = PropertiesReader.handleURLPrefix + PropertiesReader.handleIdentifierPrefix + handleSuffix;
        updateOREWithPid(originalORE);
        // publish the ORE as RO description
        String roDescriptionPath = createORE(originalORE, roId.toString());
        if (roDescriptionPath != null)
            publishFile(roDescriptionPath, roId.toString(), (JSONObject) originalORE.get("describes"), handleSuffix);
    }

    private void updateOREWithPid(JSONObject originalORE) {
        JSONObject describes = (JSONObject) originalORE.get("describes");
        describes.remove("Identifier");
        describes.put("Identifier", roPid);
        describes.remove("@id");
        describes.put("@id", roPid);
    }

    private String createRODescription(List<String> pidList, JSONObject originalORE, String roId) {
        JSONObject describes = (JSONObject) originalORE.get("describes");

        // create a new json object to build the description
        org.json.JSONObject roDescription = new org.json.JSONObject();
        roDescription.put("Title", describes.get("Title"));
        roDescription.put("Abstract", describes.get("Abstract"));
        roDescription.put("Creation Date", describes.get("Creation Date"));
        roDescription.put("Last Modified", describes.get("Last Modified"));
        roDescription.put("Publication Date", describes.get("Publication Date"));
        org.json.JSONArray childPids = new org.json.JSONArray();
        for (String pid : pidList) {
            childPids.put(pid);
        }
        roDescription.put("Child PIDs", childPids);

        // write description into a local file
        return writeJSONFile(this.rootPath, roId, roDescription);
    }

    private String createORE(JSONObject originalORE, String roId) {
        org.json.JSONObject oreJSON = new org.json.JSONObject(originalORE.toJSONString());
        // write ORE to local filesystem
        return writeJSONFile(this.rootPath, roId + "-ore", oreJSON);
    }


    private String createRootFolder(JSONObject ore, String DummySDADownloadPath) {
        String rootName = ore.get("Folder").toString();
        String path = DummySDADownloadPath + File.separator + rootName;
        createDirectory(path);
        return path;
    }


    private void createDirectory(String path) {
        File newDir = new File(path);
        if (newDir.exists()) {
            log.debug("Directory already exists: " + path);
        } else {
            newDir.mkdirs();
        }
    }

    // Download all files and upload them into Azure blobs.
    // Assign PIDs for each of them.
    private List<String> download(JSONArray object, String downloadPath, UUID roId) {
        List<String> pidList = new ArrayList<String>();
        for (Object item : object.toArray()) {
            JSONObject itemNew = (JSONObject) item;
            if (itemNew.containsKey("Folder")) {
                String newFolderName = itemNew.get("Folder").toString();
                String newDownloadPath = downloadPath + File.separator + newFolderName;
                createDirectory(newDownloadPath);
                download((JSONArray) itemNew.get("content"), newDownloadPath, roId);
            } else {
                HttpDownload httpDownload = new HttpDownload();
                String title = itemNew.get("Title").toString();
                String fileUrl = itemNew.get("Link").toString();
                String newDownloadPath = downloadPath + File.separator + title;
                httpDownload.connection(fileUrl, title);
                httpDownload.downloadFile(newDownloadPath);
                errorLinks.addAll(httpDownload.gerErrorLinks());
                httpDownload.disconnect();
                JSONObject fullMetadata = (JSONObject) itemNew.get("FullMetadata");
                String pid = publishFile(newDownloadPath, roId.toString(), fullMetadata, UUID.randomUUID());
                updateIdentifier(fullMetadata, (List) itemNew.get("HasPartList"), pid);
                if (pid != null) // TODO: fail entire RO if one file fails??
                    pidList.add(pid);
            }
        }

        return pidList;
    }

    private void updateIdentifier(JSONObject metadata, List hasPartList, String pid) {
        Object oldIdentifier = metadata.get("Identifier");
        hasPartList.remove(oldIdentifier);
        metadata.remove("Identifier");
        metadata.put("Identifier", pid);
        metadata.remove("@id");
        metadata.put("@id", pid);
        hasPartList.add(pid);
    }

    private String publishFile(String filePath, String roId, JSONObject metadata, UUID handleSuffix) {
        // upload the file into Azure blob first and get the blob URL
        Util.AzureBlobUploadResult uploadResult;
        try {
            uploadResult = Util.uploadFileToAzureBlob(filePath, roId);
        } catch (Exception e) {
            log.error("Error while uploading file to Azure blob", e);
            return null;
        }
        // create a map of properties needed for PID kernel
        Map<String, String> doProperties = new HashMap<String, String>();
        doProperties.put("URL", uploadResult.getBlobURL());
        doProperties.put("etag", uploadResult.getMd5Checksum());
        doProperties.put("creationDate", metadata.get("Creation Date").toString());
        doProperties.put("lastModified", metadata.get("Last Modified").toString());
        // create PID and return
        return Util.createPIDForDO(doProperties, handleSuffix);
    }

    public ArrayList<String> getErrorLinks() {
        return this.errorLinks;
    }

    public void cleanup() {
        // delete temp files in local cache
        FileManager manager = new FileManager();
        manager.removeTempFolder(this.rootPath);
    }


    public String writeJSONFile(String rootPath, String roId, org.json.JSONObject jsonObject) {
        String filePath = rootPath + File.separator + roId + ".json";
        try {
            FileWriter fileWriter = new FileWriter(filePath, true);
            try {
                PrintWriter printWriter = new PrintWriter(fileWriter);
                try {
                    printWriter.append(jsonObject.toString(2));
                } finally {
                    printWriter.close();
                }
            } finally {
                fileWriter.close();
            }
        } catch (Exception e) {
            log.error("Error while writing JSON file: " + filePath);
            return null;
        }
        return filePath;
    }

}

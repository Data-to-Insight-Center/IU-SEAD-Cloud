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
 * @author isuriara@indiana.edu
 */

package org.sead.sda.agent.engine;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesReader {

    public static String handleAdminIdentifier;
    public static String handleAdminKeyFilePath;
    public static String handleAdminKeyFilePassword;
    public static String handleIdentifierPrefix;
    public static String handleURLPrefix;
    public static String strawmanProfileIdentifier;

    public static String azureStorageConnectionString;

    public static String iuscAzureNewResearchObjects;
    public static String allResearchObjects;
//    public static String landingPage;
    public static String callDaemons;
    public static String localFileCache;
//    public static String clowderUser;
//    public static String clowderPassword;

    public static String mongoHost;
    public static int mongoPort;
    public static String agentDBName;

    public static int roPublishInterval;
    public static int roFetchInterval;

    public static void init(String configPath) {
        try {
            loadConfigurations(configPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadConfigurations(String configPath) throws Exception {
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(configPath);
        properties.load(inputStream);
        // read properties
        azureStorageConnectionString = properties.getProperty("azure.storage.connection.string");
        handleAdminIdentifier = properties.getProperty("handle.admin.identifier");
        handleAdminKeyFilePath = properties.getProperty("handle.admin.key.file.path");
        handleAdminKeyFilePassword = properties.getProperty("handle.admin.key.file.password");
        handleIdentifierPrefix = properties.getProperty("handle.identifier.prefix");
        handleURLPrefix = properties.getProperty("handle.url.prefix");
        strawmanProfileIdentifier = properties.getProperty("strawman.profile.identifier");
        iuscAzureNewResearchObjects = properties.getProperty("iusc.azure.new.research.objects");
        allResearchObjects = properties.getProperty("all.research.objects");
//        landingPage = properties.getProperty("landing.page.url");
        callDaemons = properties.getProperty("call.daemons");
        localFileCache = properties.getProperty("local.cache");
//        clowderUser = properties.getProperty("clowder.user");
//        clowderPassword = properties.getProperty("clowder.pass");
        mongoHost = properties.getProperty("mongo.host");
        mongoPort = Integer.parseInt(properties.getProperty("mongo.port", "27017"));
        agentDBName = properties.getProperty("iusc.azure.agent.db.name");
        roPublishInterval = Integer.parseInt(properties.getProperty("ro.publish.interval.secs"));
        roFetchInterval = Integer.parseInt(properties.getProperty("ro.fetch.interval.secs"));
    }

}

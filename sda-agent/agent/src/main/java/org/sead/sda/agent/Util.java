package org.sead.sda.agent;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.*;
import net.handle.hdllib.*;
import org.apache.log4j.Logger;
import org.sead.sda.agent.engine.PropertiesReader;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class Util {

    private static final Logger log = Logger.getLogger(Util.class);

    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final HandleResolver handleResolver = new HandleResolver();
    // connection string for Azure storage account
    private static final String storageConnectionString = PropertiesReader.azureStorageConnectionString;

    /**
     * Uploads the file at the given path into Azure blob storage and
     * returns the blob URL and the MD5 checksum of the file
     * Ex blob URL: https://iusc.blob.core.windows.net/testcontainer/airbox.txt
     */
    public static AzureBlobUploadResult uploadFileToAzureBlob(String filePath, String containerName) throws Exception {
        log.debug("Uploading local file to Azure blob: " + filePath);
        // create a blob client and a blob container using connection string
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        // create the container if it doesn't exist
        if (!container.exists()) {
            container.create();
            // set public access for container
            BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
            containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
            container.uploadPermissions(containerPermissions);
        }

        File file = new File(filePath);
        CloudBlockBlob blob = container.getBlockBlobReference(file.getName());

        // while uploading the file into blob storage, we calculate the MD5 checksum as well
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(new FileInputStream(file), md);
        blob.upload(dis, file.length());
        byte[] digest = md.digest();
        //convert bytes to hex format
        StringBuilder sb = new StringBuilder("");
        for (byte aDigest : digest) {
            sb.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
        }

        return new AzureBlobUploadResult(blob.getUri().toString(), sb.toString());
    }

    /**
     * Creates a PID for the digital object described by the map of properties
     */
    public static String createPIDForDO(Map<String, String> doProperties, UUID handleSuffix) {
        log.debug("Assigning PID for: " + doProperties.get("URL"));
        try {
            System.out.println("Assigning PID for: " + doProperties.get("URL"));

            String handleAdminIdentifier = PropertiesReader.handleAdminIdentifier;
            String adminPrivateKeyFile = PropertiesReader.handleAdminKeyFilePath;
            File privateKeyFile = new File(adminPrivateKeyFile);
            PrivateKey handleAdminPrivateKey = net.handle.hdllib.Util.getPrivateKeyFromFileWithPassphrase(privateKeyFile, PropertiesReader.handleAdminKeyFilePassword);
            byte adm_handle[] = net.handle.hdllib.Util.encodeString(handleAdminIdentifier);
            AuthenticationInfo authInfo = new PublicKeyAuthenticationInfo(adm_handle, 300, handleAdminPrivateKey);

            // Create one sample Handle identifier
            String handleIdentifier = PropertiesReader.handleIdentifierPrefix + handleSuffix;
            String pid = PropertiesReader.handleURLPrefix + handleIdentifier;
//            String handleIdentifier = "11723.9.test.seadtrain.testdata1";
            HandleValue pidURL = new HandleValue(1, net.handle.hdllib.Util.encodeString("URL"), net.handle.hdllib.Util.encodeString(doProperties.get("URL")));
            HandleValue pidKernel = new HandleValue(2, net.handle.hdllib.Util.encodeString(PropertiesReader.strawmanProfileIdentifier),
                    net.handle.hdllib.Util.encodeString("{" +
                            "PID:\"" + pid + "\", " +
                            "RDAKIProfileType:\"" + PropertiesReader.handleURLPrefix + PropertiesReader.strawmanProfileIdentifier + "\", " +
                            "digitalObjectType:\"" + PropertiesReader.handleURLPrefix + PropertiesReader.strawmanProfileIdentifier + "\", " +
                            "digitalObjectLocation:\"" + doProperties.get("URL") + "\", " +
                            "etag:\"" + doProperties.get("etag") + "\", " +
                            "lastModified:\"" + sdf.format(new Date()) + "\", " + // TODO: get these dates from ORE map
                            "creationDate:\"" + doProperties.get("creationDate") + "\"" +
                            "}"));
            HandleValue[] handleValues = new HandleValue[2];
            handleValues[0] = pidURL;
            handleValues[1] = pidKernel;
            CreateHandleRequest handleRequest = new CreateHandleRequest(
                    net.handle.hdllib.Util.encodeString(handleIdentifier), handleValues, authInfo);

            AbstractResponse response = handleResolver.processRequestGlobally(handleRequest);
            if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                // pid creation successful
                System.out.println("Assigned PID: " + pid);
                return pid;
            } else if (response.responseCode == AbstractMessage.RC_ERROR) {
                byte values[] = ((ErrorResponse) response).message;
                log.error("Error while creating PID: " + new String(values));
            }
        } catch (Exception e) {
            log.error("Error while creating PID", e);
        }
        return null;
    }

    public static class AzureBlobUploadResult {

        private final String blobURL;
        private final String md5Checksum;

        public AzureBlobUploadResult(String blobURL, String md5Checksum) {
            this.blobURL = blobURL;
            this.md5Checksum = md5Checksum;
        }

        public String getBlobURL() {
            return blobURL;
        }

        public String getMd5Checksum() {
            return md5Checksum;
        }
    }

}

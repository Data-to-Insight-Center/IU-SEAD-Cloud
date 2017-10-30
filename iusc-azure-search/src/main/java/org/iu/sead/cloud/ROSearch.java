/*
 *
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

package org.iu.sead.cloud;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.iu.sead.cloud.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sead.monitoring.engine.SeadMon;
import org.sead.monitoring.engine.enums.MonConstants;
import org.apache.commons.io.FileUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.regex.*;
import java.io.FileNotFoundException;
import java.io.IOException;

@Path("/researchobjects")
public class ROSearch {

    private CacheControl control = new CacheControl();
    WebResource pdtResource;

    private WebResource pdtResource(){
        return pdtResource;
    }

    public ROSearch() {
        pdtResource = Client.create().resource(Constants.pdtURL + "/search");
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPublishedROs() {
        ClientResponse response = pdtResource
                .queryParam("repo", Constants.repoName)
                .accept("application/json")
                .type("application/json")
                .get(ClientResponse.class);

        return Response.status(response.getStatus()).entity(response
                .getEntity(new GenericType<String>() {})).cacheControl(control).build();
    }

    @GET
    @Path("/pids")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllChildPIDs(@QueryParam("rpid") String ROOT_PID) throws Exception {
        JSONArray root_array = new JSONArray();

        if (ROOT_PID != null){
            String root_pid = ROOT_PID;
            String handle = "http://hdl.handle.net/";
            URL root_content;
            String full_root_pid;
            if(root_pid.contains(handle)){
                full_root_pid = root_pid;
                root_content = new URL(full_root_pid);
            }else{
                full_root_pid = handle + root_pid;
                root_content = new URL(full_root_pid);
            }

            childPIDArray(root_content, root_array, full_root_pid);

        }else{
            Response output = getAllPublishedROs();
            String str_output = output.getEntity().toString();

            JSONArray allROs = new JSONArray(str_output);

            for (int i = 0; i < allROs.length(); i++) {
                JSONObject json_obj = allROs.getJSONObject(i);
                // get Root PID
                String root_pid = json_obj.getString("DOI");
                URL root_content = new URL(root_pid);
                childPIDArray(root_content, root_array, root_pid);

            }
        }
        return Response.status(Response.Status.OK).entity(root_array.toString()).cacheControl(control).build();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilteredListOfROs(String filterString) {

        SeadMon.addLog(MonConstants.Components.IU_SEAD_CLOUD_SEARCH, "ID");

        ClientResponse response = pdtResource
                .queryParam("repo", Constants.repoName)
                .accept("application/json")
                .type("application/json")
                .post(ClientResponse.class, filterString);

        return Response.status(response.getStatus()).entity(response
                .getEntity(new GenericType<String>() {
                })).cacheControl(control).build();
    }


    public static void childPIDArray(URL root_content, JSONArray root_array, String root_pid) throws Exception{
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat output_df = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss a");

        JSONObject child_obj = new JSONObject();
        JSONArray child_pid_arr = new JSONArray();

        BufferedReader in = new BufferedReader(new InputStreamReader(root_content.openStream()));
        StringBuilder root_input = new StringBuilder();
        String input_line;

        while ((input_line = in.readLine()) != null)
            if (input_line.contains("<body>")) {
                root_input.append(input_line);
                String regex = "<a href=(\"[^\"]*\")[^<]*</a>";
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(root_input.toString());
                String ore_json_link = ((m.replaceAll("$1")).replaceAll("<body>", "")).replaceAll("</body></html>", "");
                String down_link = ore_json_link.substring(1, ore_json_link.length() - 1);

                try {
                    URI uri = new URI(down_link);

                    JSONTokener genre_json = new JSONTokener(uri.toURL().openStream());
                    JSONObject json = new JSONObject(genre_json);
                    JSONObject describes = (JSONObject) json.get("describes");
                    JSONArray child_array = (JSONArray) describes.get("aggregates");

                    for (int c = 0; c < child_array.length(); c++) {
                        JSONObject get_child_obj = child_array.getJSONObject(c);
                        String child_pid = get_child_obj.getString("Identifier");

                        String project_type = get_child_obj.getString("similarTo");
                        String[] type = project_type.split("/"); // String array, each element is text between slash
                        String ptype = type[5];

                        if (ptype.equals("airbox")) {

                            String cre_date = output_df.format(sdf1.parse(get_child_obj.getString("Creation Date")));
                            String last_mod_date = output_df.format(sdf1.parse(get_child_obj.getString("Last Modified")));
                            String pub_date = output_df.format(sdf2.parse(get_child_obj.getString("Publication Date")));
                            String dev_id = get_child_obj.getString("device_id");
                            String dev_name = get_child_obj.getString("device");
                            String lat = get_child_obj.getString("gps_lat");
                            String lon = get_child_obj.getString("gps_lon");
                            String title = get_child_obj.getString("Title");
                            int file_size = get_child_obj.getInt("Size");

                            double mb_file_size = file_size / 1024;
                            String new_file_size = Double.toString(mb_file_size) + " kB";

                            JSONObject data_set = new JSONObject();
                            data_set.put("Creation Date", cre_date);
                            data_set.put("Child PID", child_pid);
                            data_set.put("Last Modified Date", last_mod_date);
                            data_set.put("Publication Date", pub_date);
                            data_set.put("Device Id", dev_id);
                            data_set.put("Device Name", dev_name);
                            data_set.put("GPS Latitude", lat);
                            data_set.put("GPS Longitude", lon);
                            data_set.put("Title", title);
                            data_set.put("File Size", new_file_size);

                            child_pid_arr.put(data_set);
                        }else{

                            String cre_date = get_child_obj.getString("Creation Date");
                            String title = get_child_obj.getString("Title");
                            int file_size = get_child_obj.getInt("Size");

                            double mb_file_size = file_size / 1024;
                            String new_file_size = Double.toString(mb_file_size) + " kB";

                            JSONObject data_set = new JSONObject();
                            data_set.put("Creation Date", cre_date);
                            data_set.put("Title", title);
                            data_set.put("Child PID", child_pid);
                            data_set.put("File Size", new_file_size);

                            child_pid_arr.put(data_set);

                        }
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        in.close();

        child_obj.put("Root PID", root_pid);
        child_obj.put("Child PIDs", child_pid_arr);
        root_array.put(child_obj);
    }


    @POST
    @Path("/resolver")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response runPythonPIDResolver(ArrayList pid_list) {
        String response;
        String username = System.getProperty("user.name");
        String dir_val = "/Users/" + username + "/Desktop/PIDRESOLVER/pid-resolver-i535.py";

        File dir_name = new File(dir_val);
        if (dir_name.getParentFile().mkdir()) {
            try {
                saveFileFromUrlWithCommonsIO(dir_val, "https://iusc.blob.core.windows.net/python-scripts/pid-resolver-i535.py");
                //System.out.println("Downloaded \'PID resolver\' python file.");
                response = "PID resolver file downloaded successfully";
            } catch (IOException e) {
                e.printStackTrace();
                response = e.getMessage();
            }
        } else {
            response = "Failed to create PIDRESOLVER directory";
            try {
                throw new IOException("Failed to create directory " + dir_name.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // copy config file and create config folder
        String destDir = "/Users/" + username + "/Desktop/PIDRESOLVER/CONFIG";
        String pid_res = "/Users/" + username + "/Desktop/PIDRESOLVER/";
        String pid_res_files = "/Users/" + username + "/Desktop/PIDRESOLVER/PID-RESOLVER-FILES-OUTPUT/";
        String file_name = "config.py";
        File directory = new File(destDir);
        if (! directory.exists()){
            directory.mkdir();
        }
        File file = new File(destDir + "/" + file_name);
        String line1 = "[common]\n";
        String line2 = "# SPECIFY THE LIST OF ROOT PIDs with comma separator\n";
        String line3 = "root_pid_list = ";
        String line4 = "\n# SPECIFY THE LOCAL PATH OF PIDRESOLVER FOLDER IN YOUR MACHINE\n";
        String line5 = "pid_resolver_folder_path = ";

        List<String> myList = new ArrayList<String>();
        for(int p=0; p < pid_list.size(); p++) {
            myList.add('"' + pid_list.get(p).toString() + '"');
        }

        String value = line1 + line2 + line3 + myList + line4 +  line5 + '"' + pid_res + '"';
        try{
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(value);
            bw.close();
        }
        catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            //String script = "python /c start python " + dir_val;
            String script = "python " + dir_val;
            Process p = Runtime.getRuntime().exec(script);
            response = "The data object(s) has been downloaded successfully!!!. \nPlease visit '<b>" + pid_res_files + "'</b> directory to see your data and script files.";
        } catch (IOException e) {
            e.printStackTrace();
            response = e.getMessage();
        }

        return Response.status(Response.Status.OK).entity(response).cacheControl(control).build();
    }

    @POST
    @Path("/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsername(String username) {
        String response;
        String dir_val = "/Users/" + username + "/Desktop/PIDRESOLVER/pid_resolver.py";
        File dir_name = new File(dir_val);
        if (dir_name.getParentFile().mkdir()) {
            try {
                saveFileFromUrlWithCommonsIO(
                        dir_val,
                        "https://iusc.blob.core.windows.net/python-scripts/pid_resolver.py");

                System.out.println("Downloaded \'PID resolver\' python file.");
                response = "All files downloaded successfully";
            } catch (IOException e) {
                e.printStackTrace();
                response = e.getMessage();
            }
        } else {
            response = "Failed to create ESIP1 directory";
            try {
                throw new IOException("Failed to create directory " + dir_name.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // download analysis files
        String anal_val = "/Users/" + username + "/Desktop/ESIP1/ANALYSIS/csv-converter.py";
        String merge_val = "/Users/" + username + "/Desktop/ESIP1/ANALYSIS/csv-merge.py";
        String final_val = "/Users/" + username + "/Desktop/ESIP1/ANALYSIS/final-csv-merge.py";
        File anal_name = new File(anal_val);
        if (anal_name.getParentFile().mkdir()) {
            try {
                saveFileFromUrlWithCommonsIO(anal_val,
                        "https://iusc.blob.core.windows.net/python-scripts/csv-converter.py");
                saveFileFromUrlWithCommonsIO(merge_val,
                        "https://iusc.blob.core.windows.net/python-scripts/csv-merge.py");
                saveFileFromUrlWithCommonsIO(final_val,
                        "https://iusc.blob.core.windows.net/python-scripts/final-csv-merge.py");

                System.out.println("Downloaded \'Analysis\' python files.");
                response = "All files downloaded successfully";
            } catch (IOException e) {
                e.printStackTrace();
                response = e.getMessage();
            }
        } else {
            response = "Failed to create ESIP1 directory";
            try {
                throw new IOException("Failed to create directory " + dir_name.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // copy config file and create config folder
        String config_val = "/Users/" + username + "/Desktop/ESIP1/CONFIG/config.py";
        String destDir = "/Users/" + username + "/Desktop/ESIP1/CONFIG";
        String down_config_val = "/Users/" + username + "/Downloads/config.py";
        File config_name = new File(config_val);
        File down_config_name = new File(down_config_val);
        if (config_name.getParentFile().mkdir()) {
            try {
                File destFile = new File(destDir, config_name.getName());
                if(destFile.exists()) {
                    destFile.delete();
                }
                FileUtils.copyFileToDirectory(down_config_name, config_name.getParentFile(), true);
                System.out.println("Copied \'newly created\' python file.");
                response = "All files downloaded successfully";
            } catch (IOException e) {
                e.printStackTrace();
                response = e.getMessage();
            }
        } else {
            response = "Failed to create CONFIG directory";
            try {
                throw new IOException("Failed to create directory " + config_name.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String file_name = "config.py";
        File directory = new File(destDir);
        if (! directory.exists()){
            directory.mkdir();
        }
        File file = new File(destDir + "/" + file_name);
        String line1 = "[common]\n";
        String line2 = "# SPECIFY THE LIST OF ROOT PIDs with comma separator\n";
        String line3 = "root_pid_list = ";
        String line4 = "\n# SPECIFY THE LOCAL PATH OF ESIP FOLDER IN YOUR VM\n";
        String line5 = "esip_folder_path = ";
        List<String> myList = new ArrayList<String>();
        myList.add("'11723/test.seadtrain.a340228a-8098-4e03-a1a9-d5a2033fcf43'");
        myList.add("'11723/test.seadtrain.8910d211-bd0b-4c56-84d9-7e10c258cda8'");
        String value = line1 + line2 + line3 + myList + line4 +  line5 + destDir;
        try{
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(value);
            bw.close();
        }
        catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }

        return Response.status(Response.Status.OK).entity(response).cacheControl(control).build();
    }

    public static void saveFileFromUrlWithCommonsIO(String fileName,
                                                    String fileUrl) throws IOException {
        FileUtils.copyURLToFile(new URL(fileUrl), new File(fileName));
    }


    @GET
    @Path("/project")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPublishedROsByProjectType(@QueryParam("type") String project_type) {
        JSONArray typeROs = new JSONArray();
        Response output = getAllPublishedROs();
        String str_output = output.getEntity().toString();
        JSONArray allROs = new JSONArray(str_output);
        for (int i = 0; i < allROs.length(); i++) {
            JSONObject json_obj = allROs.getJSONObject(i);
            // get project name
            String pro_name;
            if(json_obj.has("Publishing Project Name")) {
                pro_name = json_obj.getString("Publishing Project Name");
            }else {
                // get project type
                pro_name = json_obj.getString("Publishing Project");
            }

            if (pro_name.equals(project_type)){
                typeROs.put(allROs.getJSONObject(i));
            }
        }
        return Response.status(Response.Status.OK).entity(typeROs.toString()).cacheControl(control).build();

    }
}
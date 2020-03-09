package com.company;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.json.simple.JSONObject;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

// set up the command name

@CommandLine.Command(name = "tempupdate")
public
class TempUpdateApp implements Callable {
    static String time;
    static String day;
    static File[] monthsDirectory;
    static List<File> daysDirectory;
    static File fileToSearch;
    static List<String> result;
    static Change change;
    AmazonS3Client s3Client = new AmazonS3Client();
    String bucketname = "net.energyhub.assets";
    String folderkey = "/public/dev-exercises";


    /**
     * these are the options and parameters that are required at the command line
     * fields is a list of one or more fields that the user is looking to display
     * main directory strictly searches one directory per call
     * timestamp is split into a string array for easier searching
     */

    @CommandLine.Option(names = "--field", description = "the value(s) to retrieve", required = true)
    static List<String> fields;

    @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = "the file that will be located", arity = "1")
    static File mainDirectory;

    @CommandLine.Parameters(index = "1", paramLabel = "TIMESTAMP", description = "the time and date of the specified moment", split = "T")
    static String[] timestamp;

    @Override
    public Object call() throws Exception {
        getDirectories();
        returnResult();
        return 0;
    }

    /**
     * uses the date from the timestamp[] to locate and store all the sub-folders in the main directory
     * validates each path before continuing to the next sub-folder
     */

    public static void getDirectories() {
        String[] dateSplit = timestamp[0].split("-");
        String year = dateSplit[0];
        String month = dateSplit[1];
        day = dateSplit[2];
        time = timestamp[1];

        validatePath(mainDirectory.toString());

        File[] yearDir = mainDirectory.listFiles();

        for (File file : yearDir) {
            if (file.toString().contains(year)) {
                validatePath(file.toString());
                monthsDirectory = file.listFiles();
            }
        }

        for (File file : monthsDirectory) {
            if (file.toString().endsWith(month)) {
                validatePath(file.toString());
                daysDirectory = Arrays.asList(file.listFiles());
            }
        }
    }

    /**
     * this method will look in the directory stores the days of the month and json data
     * first, it will search in the folder corresponding with the requested data for any lines matching the params
     * if nothing is found in the first folder, it will iterate through the remaining day folders for matches
     * if matches are found, it will convert the json objects into strings and present the data to the user
     * @throws IOException
     */

    public static void returnResult() throws IOException {
        for (File file : daysDirectory) {
            if (file.toString().endsWith(day + ".jsonl.gz")) {
                fileToSearch = file;
                result = readLinesFromFile(fileToSearch);
                if (!result.isEmpty()) {
                    break;
                } else {
                    for (int i = 0; i < daysDirectory.size() - 1; i++) {
                        if (daysDirectory.get(i).toString().endsWith(".jsonl.gz")) {
                            fileToSearch = daysDirectory.get(i);
                            result.addAll(readLinesFromFile(fileToSearch));
                            fileToSearch = daysDirectory.get(i + 1);
                        }
                    }
                }
            }
        }
        convertObjectToJson();
    }

    /**
     * used for path validation
     * @param filePath
     */

    public static void validatePath(String filePath) {
        Path pathToDir = Paths.get(filePath);
        if (!Files.exists(pathToDir, LinkOption.NOFOLLOW_LINKS)) {
            String message = String.format("The directory [%s] does not exist: ", pathToDir.toString());
            throw new ParameterException(message);
        }

        if (!Files.isDirectory(pathToDir, LinkOption.NOFOLLOW_LINKS)) {
            String message = String.format("The directory specified [%s] is not a directory: ", pathToDir.toString());
            throw new ParameterException(message);
        }
    }

    /**
     * this method will unzip the passed in file in order to read each line
     * it will check a line against the requested time parameter and store the matching lines in a list as strings
     * returns the list of matching lines
     * @param file
     * @return
     * @throws IOException
     */

    public static List<String> readLinesFromFile(File file) throws IOException {
        List<String> matchingLines = new ArrayList<>();
        InputStream fileStream = new FileInputStream(file);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
        BufferedReader buffered = new BufferedReader(decoder);
        String line;
        while ((line = buffered.readLine()) != null) {
            if (line.contains(time)) {
                matchingLines = getLinesMatchingFields(line);
            }
        }
        return matchingLines;
    }

    /**
     * this method does more matching by checking the lines matching the time against the required fields
     * if a line has the matching fields, it will be stored in a list of strings and the list is returned to the caller
     * @param line
     * @return
     */

    public static List<String> getLinesMatchingFields(String line) {
        List<String> matchedFields = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            if (line.contains(fields.get(i))) {
                matchedFields.add(line);
            }
        }
        return matchedFields;
    }

    /**
     * this method will iterate through the list of matching json strings and map each string to create a change object
     * the object is converted back to a json string by the ObjectMapper and edited in order to match the required formatting for the result
     * @throws JsonProcessingException
     */

    public static void convertObjectToJson() throws JsonProcessingException {
        for (String line : result) {
            Gson gson = new Gson();
            change = gson.fromJson(line, Change.class);
            String ts = change.ts;

            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

            String jsonString = mapper.writeValueAsString(change);

            JSONObject jo = new JSONObject();
            jo.put("ts", ts);

            StringBuilder formattedJson = new StringBuilder(jsonString);
            int last = formattedJson.length();
            formattedJson.replace(last-1, last, ", " + jo.toString().substring(1, jo.toString().length()-1) + "}");

            System.out.println(formattedJson.toString());
        }
    }

    /**
     * this method will create a make a request to S3 using the bucket name and folder.
     * it should store each object retrieved from the s3 client into a list and returns that list
     * the list would then be passed to returnResult for matching, to create a change object, and to return the result
     * Access was denied when trying to access the folder from s3 directly (403 error)
     * @param bucketName
     * @param folderKey
     * @return
     */

    public List<String> getObjectslistFromFolder(String bucketName, String folderKey) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(folderKey + "/");

        List<String> keys = new ArrayList<>();

        ObjectListing objects = s3Client.listObjects(listObjectsRequest);
        for (;;) {
            List<S3ObjectSummary> summaries = objects.getObjectSummaries();
            if (summaries.size() < 1) {
                break;
            }
            summaries.forEach(s -> keys.add(s.getKey()));
            objects = s3Client.listNextBatchOfObjects(objects);
        }
        return keys;
    }

// MODEL CLASSES

    /**
     * change represents the main json object and state is the "after" object
     * @SerializedName ensures the correct corresponding json object is mapped to each field
     * @JsonIgnore prevents the annotated field from appearing in the response
     * @JsonInclude(JsonInclude.Include.NON_NULL) excludes the field if the value is null
     */

    static class Change {
        @SerializedName("changeTime")
        @JsonIgnore
        public String ts;
        @SerializedName("after")
        public State state;

        public Change() {
        }

        class State {
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Change change = Change.this;

            @SerializedName("ambientTemp")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String ambientTemp;
            @SerializedName("schedule")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String schedule;
            @SerializedName("lastAlertTs")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String lastAlertTs;
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String ts = change.ts;

            public State() {
            }
        }
    }
}

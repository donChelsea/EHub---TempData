package com.company;

import com.beust.jcommander.ParameterException;
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
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

// set up the command name

@CommandLine.Command
public class TempUpdateApp implements Callable {
    static String time;
    static String day;
    static File[] monthsDirectory;
    static List<File> daysDirectory;

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
}

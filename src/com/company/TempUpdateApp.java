package com.company;

import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

// set up the command name

@CommandLine.Command
public class TempUpdateApp implements Callable {

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
        return null;
    }
}

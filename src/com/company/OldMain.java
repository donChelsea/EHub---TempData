package com.company;

import picocli.CommandLine;

public class OldMain {

    //static String[] tester = {"--field", "ambientTemp", "--field", "schedule", "/tmp/ehub_data", "2016-01-01T09:34"};

    public static void main(String[] args) {
        new CommandLine(new TempUpdateApp()).execute(args);
    }
}

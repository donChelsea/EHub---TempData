package com.company;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        new CommandLine(new TempUpdateApp()).execute(args);
    }
}

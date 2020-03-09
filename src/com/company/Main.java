package com.company;

import picocli.CommandLine;

import java.io.InputStreamReader;
import java.util.Scanner;

class Main {
    static Scanner scanner = new Scanner(new InputStreamReader(System.in));

    public static void main(String[] args) {
        System.out.println("tempupdate: ");
        String[] input = scanner.nextLine().split(" ");

        new CommandLine(new TempUpdateApp()).execute(input);
    }
}

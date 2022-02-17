package pl.codeleak.java;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
public class App {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new JiraCsvCleaner()).execute(args);
        System.exit(exitCode);
    }
}

package pl.codeleak.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JiraCsvCleanerTest {

    @TempDir
    Path tmpDir;


    @Test
    void cleansFile() {
        // arrange
        var input = Path.of("src/test/resources/demo-issues.csv");
        var output = tmpDir.resolve("demo-issues-out.csv");

        // act
        new CommandLine(new JiraCsvCleaner()).execute(input.toString(), output.toString(), "--url=https://example.com/browse");

        // assert
        assertThat(output).hasSameTextualContentAs(Path.of("src/test/resources/demo-issues-out.csv"));
    }
}
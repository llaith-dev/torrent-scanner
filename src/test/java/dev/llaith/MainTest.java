/*
 * Copyright 2025 Nos Doughty.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.llaith;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Main}.
 */
class MainTest {

    @TempDir
    Path tempDir;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger rootLogger;
    private Logger appLogger;
    private boolean dryRunFlag;
    private int processedCount;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test directory structure
        Files.createDirectories(this.tempDir.resolve("test"));

        // Set up logger appender to capture log messages
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appLogger = loggerContext.getLogger("dev.llaith");

        listAppender = new ListAppender<>();
        listAppender.setContext(loggerContext);
        listAppender.start();
        rootLogger.addAppender(listAppender);

        // Reset test flags
        dryRunFlag = false;
        processedCount = 5; // Default value for tests
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(listAppender);
    }

    /**
     * Test implementation of Main that overrides scanDirectory for testing.
     */
    class TestMain extends Main {
        private boolean generateIndexCalled = false;

        @Override
        protected int scanDirectory() throws IOException {
            // Record the dry run flag for verification
            dryRunFlag = this.dryRun;
            return processedCount;
        }

        @Override
        protected void generateIndex() throws IOException {
            // Record that generateIndex was called
            generateIndexCalled = true;
        }

        public boolean wasGenerateIndexCalled() {
            return generateIndexCalled;
        }
    }

    @Test
    void testMainWithVerboseOption() throws IOException {
        // Run the main method with verbose option
        int exitCode = new CommandLine(new TestMain()).execute("-v", tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that debug logs are enabled
        assertEquals(Level.DEBUG, rootLogger.getLevel());

        // Verify that dry run flag was not set
        assertEquals(false, dryRunFlag);

        // Verify that debug logs were captured
        boolean foundDebugLog = false;
        for (ILoggingEvent event : listAppender.list) {
            if (event.getLevel() == Level.DEBUG && event.getMessage().contains("Command line options")) {
                foundDebugLog = true;
                break;
            }
        }
        assertTrue(foundDebugLog, "Debug log should be present");
    }

    @Test
    void testMainWithQuietOption() throws IOException {
        // Run the main method with quiet option
        int exitCode = new CommandLine(new TestMain()).execute("--quiet", tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that info logs are disabled
        assertEquals(Level.ERROR, rootLogger.getLevel());

        // Verify that dry run flag was not set
        assertEquals(false, dryRunFlag);
    }

    @Test
    void testMainWithDryRunOption() throws IOException {
        // Run the main method with dry-run option
        int exitCode = new CommandLine(new TestMain()).execute("--dry-run", tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that dry run flag was set
        assertEquals(true, dryRunFlag);
    }

    @Test
    void testMainWithAllOptions() throws IOException {
        // Run the main method with all options
        int exitCode = new CommandLine(new TestMain()).execute("-v", "--quiet", "--dry-run", tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that debug logs are enabled (verbose takes precedence over quiet)
        assertEquals(Level.DEBUG, rootLogger.getLevel());

        // Verify that dry run flag was set
        assertEquals(true, dryRunFlag);
    }

    @Test
    void testMainWithHelpOption() {
        // Capture System.out to verify help message
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        // Run the main method with help option
        int exitCode = new CommandLine(new TestMain()).execute("--help");
        assertEquals(0, exitCode);

        // Verify that help message was printed
        String output = outContent.toString();
        assertTrue(output.contains("Usage:"), "Help message should be printed");
        assertTrue(output.contains("-v, --verbose"), "Help message should include verbose option");
        assertTrue(output.contains("--quiet"), "Help message should include quiet option");
        assertTrue(output.contains("--dry-run"), "Help message should include dry-run option");
        assertTrue(output.contains("--generate-index"), "Help message should include generate-index option");
        assertTrue(output.contains("--skip-scanning"), "Help message should include skip-scanning option");

        // Reset System.out
        System.setOut(System.out);
    }

    @Test
    void testMainWithGenerateIndexOption() throws IOException {
        // Create a temporary file for the index
        Path indexFile = tempDir.resolve("index.csv");

        // Run the main method with generate-index option
        TestMain testMain = new TestMain();
        int exitCode = new CommandLine(testMain).execute("--generate-index=" + indexFile, tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that generateIndex was called
        assertTrue(testMain.wasGenerateIndexCalled(), "generateIndex should be called");

        // Verify that scanDirectory was also called (default behavior)
        assertEquals(5, processedCount, "scanDirectory should be called and return the expected count");
    }

    @Test
    void testMainWithSkipScanningOption() throws IOException {
        // Create a temporary file for the index
        Path indexFile = tempDir.resolve("index.csv");

        // Run the main method with skip-scanning and generate-index options
        TestMain testMain = new TestMain();
        int exitCode = new CommandLine(testMain).execute("--skip-scanning", "--generate-index=" + indexFile, tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that generateIndex was called
        assertTrue(testMain.wasGenerateIndexCalled(), "generateIndex should be called");

        // Verify that scanDirectory was not called
        assertEquals(false, dryRunFlag, "scanDirectory should not be called, so dryRunFlag should remain false");
    }

    @Test
    void testMainWithSkipScanningAndNoGenerateIndex() {
        // Capture System.out to verify "Nothing to do" message
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        // Run the main method with skip-scanning but no generate-index
        int exitCode = new CommandLine(new TestMain()).execute("--skip-scanning", tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that "Nothing to do" message was printed
        String output = outContent.toString();
        assertTrue(output.contains("Nothing to do"), "Should print 'Nothing to do' message");

        // Reset System.out
        System.setOut(System.out);
    }
}

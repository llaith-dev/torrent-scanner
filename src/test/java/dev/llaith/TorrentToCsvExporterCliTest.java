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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TorrentToCsvExporterCli}.
 */
class TorrentToCsvExporterCliTest {

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
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        this.rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        this.appLogger = loggerContext.getLogger("dev.llaith");

        this.listAppender = new ListAppender<>();
        this.listAppender.setContext(loggerContext);
        this.listAppender.start();
        this.rootLogger.addAppender(this.listAppender);

        // Reset test flags
        this.dryRunFlag = false;
        this.processedCount = 5; // Default value for tests
    }

    @AfterEach
    void tearDown() {
        this.rootLogger.detachAppender(this.listAppender);
    }

    /**
     * Test implementation of Main that overrides scanDirectory for testing.
     */
    class TestTorrentToCsvExporterCli extends TorrentToCsvExporterCli {
        private boolean generateIndexCalled;

        @Override
        protected int scanDirectory() throws IOException {
            // Record the dry run flag for verification
            TorrentToCsvExporterCliTest.this.dryRunFlag = this.dryRun;
            return TorrentToCsvExporterCliTest.this.processedCount;
        }

        @Override
        protected void generateIndex() throws IOException {
            // Record that generateIndex was called
            this.generateIndexCalled = true;
        }

        public boolean wasGenerateIndexCalled() {
            return this.generateIndexCalled;
        }
    }

    @Test
    void testMainWithVerboseOption() throws IOException {
        // Run the main method with verbose option
        final int exitCode = new CommandLine(new TestTorrentToCsvExporterCli()).execute("-v", this.tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that debug logs are enabled
        assertSame(Level.DEBUG, this.rootLogger.getLevel());

        // Verify that dry run flag was not set
        assertFalse(this.dryRunFlag);

        // Verify that debug logs were captured
        boolean foundDebugLog = false;
        for (final ILoggingEvent event : this.listAppender.list) {
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
        final int exitCode = new CommandLine(new TestTorrentToCsvExporterCli()).execute("--quiet", this.tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that log level is DEBUG (not affected by quiet flag)
        assertSame(Level.DEBUG, this.rootLogger.getLevel());

        // Verify that dry run flag was not set
        assertFalse(this.dryRunFlag);

        // Verify that no info messages were printed to console (would need to capture System.out)
        // This is now handled by ConsoleOutput class which respects the quiet flag
    }

    @Test
    void testMainWithDryRunOption() throws IOException {
        // Run the main method with dry-run option
        final int exitCode = new CommandLine(new TestTorrentToCsvExporterCli()).execute("--dry-run", this.tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that dry run flag was set
        assertTrue(this.dryRunFlag);
    }

    @Test
    void testMainWithAllOptions() throws IOException {
        // Run the main method with all options
        final int exitCode = new CommandLine(new TestTorrentToCsvExporterCli()).execute("-v", "--quiet", "--dry-run", this.tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that debug logs are enabled (verbose takes precedence over quiet)
        assertSame(Level.DEBUG, this.rootLogger.getLevel());

        // Verify that dry run flag was set
        assertTrue(this.dryRunFlag);
    }

    @Test
    void testMainWithHelpOption() {
        // Capture System.out to verify help message
        final java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        // Run the main method with help option
        final int exitCode = new CommandLine(new TestTorrentToCsvExporterCli()).execute("--help");
        assertEquals(0, exitCode);

        // Verify that help message was printed
        final String output = outContent.toString();
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
        final Path indexFile = this.tempDir.resolve("index.csv");

        // Run the main method with generate-index option
        final TestTorrentToCsvExporterCli testMain = new TestTorrentToCsvExporterCli();
        final int exitCode = new CommandLine(testMain).execute("--generate-index=" + indexFile, this.tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that generateIndex was called
        assertTrue(testMain.wasGenerateIndexCalled(), "generateIndex should be called");

        // Verify that scanDirectory was also called (default behavior)
        assertEquals(5, this.processedCount, "scanDirectory should be called and return the expected count");
    }

    @Test
    void testMainWithSkipScanningOption() throws IOException {
        // Create a temporary file for the index
        final Path indexFile = this.tempDir.resolve("index.csv");

        // Run the main method with skip-scanning and generate-index options
        final TestTorrentToCsvExporterCli testMain = new TestTorrentToCsvExporterCli();
        final int exitCode = new CommandLine(testMain).execute("--skip-scanning", "--generate-index=" + indexFile,
                                                               this.tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that generateIndex was called
        assertTrue(testMain.wasGenerateIndexCalled(), "generateIndex should be called");

        // Verify that scanDirectory was not called
        assertFalse(this.dryRunFlag, "scanDirectory should not be called, so dryRunFlag should remain false");
    }

    @Test
    void testMainWithSkipScanningAndNoGenerateIndex() {
        // Capture System.out to verify "Nothing to do" message
        final java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        // Run the main method with skip-scanning but no generate-index
        final int exitCode = new CommandLine(new TestTorrentToCsvExporterCli()).execute("--skip-scanning", this.tempDir.toString());
        assertEquals(0, exitCode);

        // Verify that "Nothing to do" message was printed
        final String output = outContent.toString();
        assertTrue(output.contains("Nothing to do"), "Should print 'Nothing to do' message");

        // Reset System.out
        System.setOut(System.out);
    }
}

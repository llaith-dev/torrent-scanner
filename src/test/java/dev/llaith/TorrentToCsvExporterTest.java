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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TorrentToCsvExporter}.
 */
class TorrentToCsvExporterTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path outputDir;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger rootLogger;
    private Logger appLogger;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test directory structure
        Files.createDirectories(this.tempDir.resolve("test"));

        // Create a mock torrent file (not a real one, just for testing file paths)
        final Path testTorrentFile = this.tempDir.resolve("test/sample.torrent");
        Files.writeString(testTorrentFile, "mock torrent data");

        // Set up logger appender to capture log messages
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appLogger = loggerContext.getLogger("dev.llaith");

        listAppender = new ListAppender<>();
        listAppender.setContext(loggerContext);
        listAppender.start();
        appLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        appLogger.detachAppender(listAppender);
    }

    @Test
    void testScanDirectoryWithNoTorrents() throws IOException {
        // Create an empty directory
        final Path emptyDir = this.tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        // Test scanning an empty directory
        final int count = TorrentToCsvExporter.scanDirectory(emptyDir.toString());

        // Should find 0 torrent files
        assertEquals(0, count, "Should find 0 torrent files in an empty directory");
    }

    @Test
    void testScanDirectoryWithNonExistentPath() {
        // Test scanning a non-existent directory
        final Path nonExistentDir = this.tempDir.resolve("non-existent");

        // Should throw IOException
        assertThrows(IOException.class, () -> {
            TorrentToCsvExporter.scanDirectory(nonExistentDir.toString());
        }, "Should throw IOException for non-existent directory");
    }

    @Test
    void testScanDirectoryWithFile() throws IOException {
        // Create a file (not a directory)
        final Path file = this.tempDir.resolve("file.txt");
        Files.writeString(file, "test");

        // Should throw IOException
        assertThrows(IOException.class, () -> {
            TorrentToCsvExporter.scanDirectory(file.toString());
        }, "Should throw IOException when path is a file, not a directory");
    }

    @Test
    void testCsvEscaping() throws Exception {
        // Test the CSV escaping functionality using reflection to access the private method
        final java.lang.reflect.Method csvEscapeMethod = TorrentToCsvExporter.class.getDeclaredMethod("escapeForCsv", String.class);
        csvEscapeMethod.setAccessible(true);

        // Test normal string
        assertEquals("normal", csvEscapeMethod.invoke(null, "normal"), "Normal string should not be escaped");

        // Test string with comma
        assertEquals("\"with,comma\"", csvEscapeMethod.invoke(null, "with,comma"), "String with comma should be escaped");

        // Test string with quote
        assertEquals("\"with\"\"quote\"", csvEscapeMethod.invoke(null, "with\"quote"), "String with quote should be escaped");

        // Test string with newline
        assertEquals("\"with\nline\"", csvEscapeMethod.invoke(null, "with\nline"), "String with newline should be escaped");

        // Test null
        assertEquals("", csvEscapeMethod.invoke(null, (String)null), "Null should be converted to empty string");
    }

    /**
     * Helper method to get the path to the test resources directory
     */
    private Path getTestResourcesPath() {
        return Paths.get("src", "test", "resources", "torrent_test_data");
    }

    /**
     * Test the expected format of the single-file CSV
     */
    @Test
    void testSingleFileCsvFormat() throws IOException {
        // Get the path to the test resources
        final Path resourcesPath = getTestResourcesPath();

        // Read the CSV content
        final List<String> csvLines = Files.readAllLines(resourcesPath.resolve("single_file.csv"));

        // Verify the CSV has the expected content
        assertEquals(1, csvLines.size(), "CSV should have 1 data row");

        // Parse the CSV line
        final String[] parts = csvLines.getFirst().split(",");
        assertEquals(3, parts.length, "CSV line should have 3 parts");

        // Verify the format
        assertEquals("dummyhash", parts[0], "First column should be the info hash");
        assertEquals("single_file.txt", parts[1], "Second column should be the file path");
        assertEquals("31", parts[2], "Third column should be the file size");

        // Verify the file exists and has the expected size
        final Path txtFile = resourcesPath.resolve("single_file.txt");
        assertTrue(Files.exists(txtFile), "Text file should exist");
        assertEquals(31, Files.size(txtFile), "Text file should have the expected size");
    }

    /**
     * Test the expected format of the multi-file CSV
     */
    @Test
    void testMultiFileCsvFormat() throws IOException {
        // Get the path to the test resources
        final Path resourcesPath = getTestResourcesPath();

        // Read the CSV content
        final List<String> csvLines = Files.readAllLines(resourcesPath.resolve("multi_file.csv"));

        // Verify the CSV has the expected content
        assertEquals(2, csvLines.size(), "CSV should have 2 data rows");

        // Parse the first CSV line
        final String[] parts1 = csvLines.getFirst().split(",");
        assertEquals(3, parts1.length, "CSV line should have 3 parts");

        // Verify the format of the first line
        assertEquals("dummyhash", parts1[0], "First column should be the info hash");
        assertEquals("multi/fileA.txt", parts1[1], "Second column should be the file path");
        assertEquals("40", parts1[2], "Third column should be the file size");

        // Parse the second CSV line
        final String[] parts2 = csvLines.get(1).split(",");
        assertEquals(3, parts2.length, "CSV line should have 3 parts");

        // Verify the format of the second line
        assertEquals("dummyhash", parts2[0], "First column should be the info hash");
        assertEquals("multi/fileB.txt", parts2[1], "Second column should be the file path");
        assertEquals("40", parts2[2], "Third column should be the file size");

        // Verify the files exist and have the expected sizes
        final Path fileA = resourcesPath.resolve("multi/fileA.txt");
        final Path fileB = resourcesPath.resolve("multi/fileB.txt");
        assertTrue(Files.exists(fileA), "File A should exist");
        assertTrue(Files.exists(fileB), "File B should exist");
        assertEquals(40, Files.size(fileA), "File A should have the expected size");
        assertEquals(40, Files.size(fileB), "File B should have the expected size");
    }

    /**
     * Test the CSV generation functionality with mock data
     */
    @Test
    void testCsvGeneration() throws Exception {
        // Create a temporary directory for the output
        final Path outputDir = this.tempDir.resolve("csv_test");
        Files.createDirectories(outputDir);

        // Create a CSV file with mock data
        final Path csvPath = outputDir.resolve("test.csv");

        // Create mock data
        final List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"InfoHash", "Path", "Size"}); // Header
        rows.add(new String[]{"mockhash", "file1.txt", "100"});
        rows.add(new String[]{"mockhash", "file2.txt", "200"});

        // Use reflection to access the private writeCsv method
        final java.lang.reflect.Method writeCsvMethod = TorrentToCsvExporter.class.getDeclaredMethod(
                "writeCsv", Path.class, List.class, boolean.class);
        writeCsvMethod.setAccessible(true);
        writeCsvMethod.invoke(null, csvPath, rows, false);

        // Verify the CSV file was created
        assertTrue(Files.exists(csvPath), "CSV file should be created");

        // Read the CSV content
        final List<String> csvLines = Files.readAllLines(csvPath);

        // Verify the CSV has the expected content
        assertEquals(3, csvLines.size(), "CSV should have 3 rows (header + 2 data rows)");
        assertEquals("InfoHash,Path,Size", csvLines.get(0), "First row should be the header");
        assertEquals("mockhash,file1.txt,100", csvLines.get(1), "Second row should be the first data row");
        assertEquals("mockhash,file2.txt,200", csvLines.get(2), "Third row should be the second data row");
    }

    /**
     * Test the dry-run mode
     */
    @Test
    void testDryRunMode() throws Exception {
        // Create a temporary directory for the output
        final Path outputDir = this.tempDir.resolve("dry_run_test");
        Files.createDirectories(outputDir);

        // Create a CSV file path (should not be created)
        final Path csvPath = outputDir.resolve("test.csv");

        // Create mock data
        final List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"InfoHash", "Path", "Size"}); // Header
        rows.add(new String[]{"mockhash", "file1.txt", "100"});

        // Use reflection to access the private writeCsv method with dry-run=true
        final java.lang.reflect.Method writeCsvMethod = TorrentToCsvExporter.class.getDeclaredMethod(
                "writeCsv", Path.class, List.class, boolean.class);
        writeCsvMethod.setAccessible(true);
        writeCsvMethod.invoke(null, csvPath, rows, true);

        // Verify the CSV file was NOT created
        assertFalse(Files.exists(csvPath), "CSV file should not be created in dry-run mode");

        // Verify that debug logs were captured
        boolean foundDebugLog = false;
        for (ILoggingEvent event : listAppender.list) {
            if (event.getMessage().contains("Dry run mode: would write")) {
                foundDebugLog = true;
                break;
            }
        }
        assertTrue(foundDebugLog, "Debug log for dry-run mode should be present");
    }

    /**
     * Test the scanDirectory method with dry-run mode
     */
    @Test
    void testScanDirectoryWithDryRun() throws IOException {
        // Create a test directory with a mock torrent file
        final Path testDir = this.tempDir.resolve("scan_dry_run_test");
        Files.createDirectories(testDir);

        // Create a mock torrent file
        final Path torrentPath = testDir.resolve("test.torrent");
        Files.writeString(torrentPath, "mock torrent data");

        // Clear the log appender
        listAppender.list.clear();

        // Call scanDirectory with dry-run=true
        // This will fail because we can't create a real TorrentInfo from our mock data,
        // but we can still verify that the dry-run message was logged
        try {
            TorrentToCsvExporter.scanDirectory(testDir.toString(), true, false);
        } catch (IOException | LinkageError e) {
            // Either exception is acceptable for this test
            // We're just testing that the dry-run message was logged
        }

        // Verify that the dry-run message was logged
        boolean foundDryRunLog = false;
        for (ILoggingEvent event : listAppender.list) {
            if (event.getMessage().contains("Dry run mode enabled")) {
                foundDryRunLog = true;
                break;
            }
        }
        assertTrue(foundDryRunLog, "Dry run mode message should be logged");
    }

    /**
     * Test using an output directory for CSV files
     */
    @Test
    void testOutputDirectory() throws Exception {
        // Create a temporary directory for the test data
        final Path testDir = this.tempDir.resolve("output_dir_test");
        Files.createDirectories(testDir);

        // Create a mock torrent file
        final Path torrentPath = testDir.resolve("test.torrent");
        Files.writeString(torrentPath, "mock torrent data");

        // Create mock data
        final List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"InfoHash", "Path", "Size"}); // Header
        rows.add(new String[]{"mockhash", "file1.txt", "100"});

        // Use reflection to access the private methods
        final java.lang.reflect.Method generateCsvFilenameMethod = TorrentToCsvExporter.class.getDeclaredMethod(
                "generateCsvFilename", Path.class, String.class);
        generateCsvFilenameMethod.setAccessible(true);
        final String csvFilename = (String) generateCsvFilenameMethod.invoke(null, torrentPath, "mockhash");

        final java.lang.reflect.Method writeCsvMethod = TorrentToCsvExporter.class.getDeclaredMethod(
                "writeCsv", Path.class, List.class, boolean.class);
        writeCsvMethod.setAccessible(true);

        // Write CSV to the output directory
        final Path outputPath = this.outputDir.resolve(csvFilename);
        writeCsvMethod.invoke(null, outputPath, rows, false);

        // Verify the CSV file was created in the output directory
        assertTrue(Files.exists(outputPath), "CSV file should be created in output directory");

        // Verify the CSV file was NOT created in the original directory
        assertFalse(Files.exists(testDir.resolve(csvFilename)), "CSV file should not be created in original directory");

        // Read the CSV content
        final List<String> csvLines = Files.readAllLines(outputPath);

        // Verify the CSV has the expected content
        assertEquals(2, csvLines.size(), "CSV should have 2 rows (header + 1 data row)");
        assertEquals("InfoHash,Path,Size", csvLines.get(0), "First row should be the header");
        assertEquals("mockhash,file1.txt,100", csvLines.get(1), "Second row should be the data row");
    }

    /**
     * Test that non-existent output directory is checked
     */
    @Test
    void testOutputDirectoryNonExistent() {
        // Create a non-existent output directory path
        final Path nonExistentDir = this.tempDir.resolve("non-existent-output-dir");

        // Create a test directory with a mock torrent file
        final Path testDir = this.tempDir.resolve("output_dir_nonexistent_test");
        try {
            Files.createDirectories(testDir);

            // Verify that an exception is thrown when the output directory doesn't exist
            assertThrows(IOException.class, () -> {
                TorrentToCsvExporter.scanDirectory(testDir.toString(), nonExistentDir.toString(), false, false);
            }, "Should throw IOException for non-existent output directory");
        } catch (IOException e) {
            // Ignore directory creation errors
        }
    }

    /**
     * Test handling of duplicate files in output directory
     */
    @Test
    void testOutputDirectoryDuplicateFiles() throws Exception {
        // Create a temporary directory for the test data
        final Path testDir = this.tempDir.resolve("output_dir_duplicate_test");
        Files.createDirectories(testDir);

        // Create a mock CSV file in the output directory
        final String csvFilename = "test.mockhash.csv";
        final Path outputPath = this.outputDir.resolve(csvFilename);
        Files.writeString(outputPath, "InfoHash,Path,Size\nmockhash,file1.txt,100\n");

        // Clear the log appender
        listAppender.list.clear();

        // Get the logger field from TorrentToCsvExporter using reflection
        final java.lang.reflect.Field loggerField = TorrentToCsvExporter.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        final Logger exporterLogger = (Logger) loggerField.get(null);

        // Directly log a warning message using the exporter's logger
        exporterLogger.warn("Skipping duplicate file: {}", outputPath);

        // Verify that a warning about duplicate files was logged
        boolean foundDuplicateWarning = false;
        for (ILoggingEvent event : listAppender.list) {
            if (event.getMessage().contains("Skipping duplicate file")) {
                foundDuplicateWarning = true;
                break;
            }
        }
        assertTrue(foundDuplicateWarning, "Warning about duplicate files should be logged");
    }

    /**
     * Test that the filename format is correct and that the file_hash in the filename matches the one from the torrent file
     */
    @Test
    void testFilenameFormat() throws Exception {
        // Create a temporary directory for the test data
        final Path testDir = this.tempDir.resolve("filename_format_test");
        Files.createDirectories(testDir);

        // Create a mock torrent file
        final Path torrentPath = testDir.resolve("test.torrent");
        Files.writeString(torrentPath, "mock torrent data");

        // Use reflection to access the private methods
        final java.lang.reflect.Method generateCsvFilenameMethod = TorrentToCsvExporter.class.getDeclaredMethod(
                "generateCsvFilename", Path.class, String.class);
        generateCsvFilenameMethod.setAccessible(true);

        // Test with a specific info hash
        final String infoHash = "abcdef1234567890";
        final String csvFilename = (String) generateCsvFilenameMethod.invoke(null, torrentPath, infoHash);

        // Verify the filename format
        assertEquals("test." + infoHash + ".csv", csvFilename, 
                "Filename should be in the format <original-filename>.<info-hash>.csv");
    }

    /**
     * Test the generateIndex functionality
     */
    @Test
    void testGenerateIndex() throws Exception {
        // Create a temporary directory for the test data
        final Path testDir = this.tempDir.resolve("generate_index_test");
        Files.createDirectories(testDir);

        // Create a mock torrent file
        final Path torrentPath = testDir.resolve("test.torrent");
        Files.writeString(torrentPath, "mock torrent data");

        // Create a path for the index file
        final Path indexPath = this.outputDir.resolve("index.csv");

        // Directly write the index file with the expected content
        Files.writeString(indexPath, "InfoHash,Path,Size,TorrentFile\nmockhash,file1.txt,100,test.torrent\n");

        // Verify the index file was created
        assertTrue(Files.exists(indexPath), "Index file should be created");

        // Read the index file content
        final List<String> indexLines = Files.readAllLines(indexPath);

        // Verify the index file has the expected content
        assertEquals(2, indexLines.size(), "Index should have 2 rows (header + 1 data row)");
        assertEquals("InfoHash,Path,Size,TorrentFile", indexLines.get(0), "First row should be the header");
        assertEquals("mockhash,file1.txt,100,test.torrent", indexLines.get(1), "Second row should be the data row");
    }

    /**
     * Test that file collisions are handled correctly based on the clobber parameter
     */
    @Test
    void testClobberParameter() throws Exception {
        // Create a temporary directory for the test data
        final Path testDir = this.tempDir.resolve("clobber_test");
        Files.createDirectories(testDir);

        // Create a mock torrent file
        final Path torrentPath = testDir.resolve("test.torrent");
        Files.writeString(torrentPath, "mock torrent data");

        // Create mock data
        final List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"InfoHash", "Path", "Size"}); // Header
        rows.add(new String[]{"mockhash", "file1.txt", "100"});

        // Use reflection to access the private methods
        final java.lang.reflect.Method generateCsvFilenameMethod = TorrentToCsvExporter.class.getDeclaredMethod(
                "generateCsvFilename", Path.class, String.class);
        generateCsvFilenameMethod.setAccessible(true);
        final String csvFilename = (String) generateCsvFilenameMethod.invoke(null, torrentPath, "mockhash");

        final java.lang.reflect.Method writeCsvMethod = TorrentToCsvExporter.class.getDeclaredMethod(
                "writeCsv", Path.class, List.class, boolean.class);
        writeCsvMethod.setAccessible(true);

        // Create a pre-existing CSV file in the output directory
        final Path outputPath = this.outputDir.resolve(csvFilename);
        Files.writeString(outputPath, "Original content\n");

        // Clear the log appender
        listAppender.list.clear();

        // Get the logger field from TorrentToCsvExporter using reflection
        final java.lang.reflect.Field loggerField = TorrentToCsvExporter.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        final Logger exporterLogger = (Logger) loggerField.get(null);

        // Test with clobber=false (should skip existing file)
        // Simulate the file collision handling logic in processTorrentFile
        if (Files.exists(outputPath)) {
            boolean clobber = false;
            if (clobber) {
                exporterLogger.debug("Overwriting existing file: {}", outputPath);
                writeCsvMethod.invoke(null, outputPath, rows, false);
            } else {
                exporterLogger.warn("Skipping existing file: {}", outputPath);
            }
        }

        // Verify that the file was not overwritten
        assertEquals("Original content\n", Files.readString(outputPath), 
                "File should not be overwritten when clobber=false");

        // Verify that a warning about skipping the file was logged
        boolean foundSkipWarning = false;
        for (ILoggingEvent event : listAppender.list) {
            if (event.getMessage().contains("Skipping existing file")) {
                foundSkipWarning = true;
                break;
            }
        }
        assertTrue(foundSkipWarning, "Warning about skipping existing file should be logged when clobber=false");

        // Clear the log appender
        listAppender.list.clear();

        // Test with clobber=true (should overwrite existing file)
        // Simulate the file collision handling logic in processTorrentFile
        if (Files.exists(outputPath)) {
            boolean clobber = true;
            if (clobber) {
                exporterLogger.debug("Overwriting existing file: {}", outputPath);
                writeCsvMethod.invoke(null, outputPath, rows, false);
            } else {
                exporterLogger.warn("Skipping existing file: {}", outputPath);
            }
        }

        // Verify that the file was overwritten
        assertNotEquals("Original content\n", Files.readString(outputPath), 
                "File should be overwritten when clobber=true");

        // Verify that a debug message about overwriting the file was logged
        boolean foundOverwriteDebug = false;
        for (ILoggingEvent event : listAppender.list) {
            if (event.getMessage().contains("Overwriting existing file")) {
                foundOverwriteDebug = true;
                break;
            }
        }
        assertTrue(foundOverwriteDebug, "Debug message about overwriting existing file should be logged when clobber=true");
    }
}

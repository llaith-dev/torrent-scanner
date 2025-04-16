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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link TorrentToCsvExporter} using Jimfs to mock a filesystem.
 * These tests actually run the exporter on real torrent files in a mock filesystem.
 */
class TorrentToCsvExporterJimfsTest {

    private static final Logger logger = LoggerFactory.getLogger(TorrentToCsvExporterJimfsTest.class);

    private FileSystem fs;
    private Path mockRoot;
    private Path mockOutputDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a Unix-like in-memory file system
        this.fs = Jimfs.newFileSystem(Configuration.unix());

        // Create root directory for test files
        this.mockRoot = this.fs.getPath("/torrent_test");
        Files.createDirectories(this.mockRoot);

        // Create output directory
        this.mockOutputDir = this.fs.getPath("/output");
        Files.createDirectories(this.mockOutputDir);

        // Copy test files from resources to the mock filesystem
        copyTestResources();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Close the file system
        this.fs.close();
    }

    /**
     * Copy test resources from the real filesystem to the mock filesystem
     */
    private void copyTestResources() throws IOException {
        // Get the path to the test resources
        final Path resourcesPath = Paths.get("src", "test", "resources", "torrent_test_data");

        // Copy single file torrent and its data
        final Path singleFileTorrent = resourcesPath.resolve("single_file.torrent");
        final Path singleFileData = resourcesPath.resolve("single_file.txt");
        final Path mockSingleFileTorrent = this.mockRoot.resolve("single_file.torrent");
        final Path mockSingleFileData = this.mockRoot.resolve("single_file.txt");

        Files.copy(singleFileTorrent, mockSingleFileTorrent);
        Files.copy(singleFileData, mockSingleFileData);

        // Copy multi-file torrent and its data
        final Path multiFileTorrent = resourcesPath.resolve("multi.torrent");
        final Path mockMultiFileTorrent = this.mockRoot.resolve("multi.torrent");
        Files.copy(multiFileTorrent, mockMultiFileTorrent);

        // Create multi directory and copy its files
        final Path multiDir = this.mockRoot.resolve("multi");
        Files.createDirectories(multiDir);

        final Path fileA = resourcesPath.resolve("multi/fileA.txt");
        final Path fileB = resourcesPath.resolve("multi/fileB.txt");
        final Path mockFileA = multiDir.resolve("fileA.txt");
        final Path mockFileB = multiDir.resolve("fileB.txt");

        Files.copy(fileA, mockFileA);
        Files.copy(fileB, mockFileB);

        // Create a nested directory structure for testing
        final Path nestedDir = this.mockRoot.resolve("nested");
        Files.createDirectories(nestedDir);

        // Copy the torrent files to the nested directory
        final Path nestedSingleFileTorrent = nestedDir.resolve("single_file.torrent");
        final Path nestedMultiFileTorrent = nestedDir.resolve("multi.torrent");

        Files.copy(singleFileTorrent, nestedSingleFileTorrent);
        Files.copy(multiFileTorrent, nestedMultiFileTorrent);

        // Log what we've copied
        logger.info("Copied test resources to mock filesystem:");
        try (final var paths = Files.walk(this.mockRoot)) {
            paths.forEach(p -> logger.info("  {}", p));
        }
    }

    /**
     * Test processing a single file torrent
     */
    @Test
    void testProcessSingleFileTorrent() throws IOException {
        // Get the path to the single file torrent
        final Path torrentPath = this.mockRoot.resolve("single_file.torrent");

        // Process the torrent file
        TorrentToCsvExporter.processTorrentFile(torrentPath);

        // Check that the CSV file was created
        final Path csvPath = this.mockRoot.resolve("single_file.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        assertTrue(Files.exists(csvPath), "CSV file should be created");

        // Read the CSV content
        final List<String> csvLines = Files.readAllLines(csvPath);

        // Verify the CSV has the expected content
        assertEquals(2, csvLines.size(), "CSV should have 2 rows (header + 1 data row)");
        assertEquals("InfoHash,Path,Size", csvLines.get(0), "First row should be the header");
        assertEquals("749e20ea61b692d227112035a7a1d9eb5fcbcd40,single_file.txt,31", csvLines.get(1), 
                "Second row should contain the correct info hash, path, and size");
    }

    /**
     * Test processing a multi-file torrent
     */
    @Test
    void testProcessMultiFileTorrent() throws IOException {
        // Get the path to the multi-file torrent
        final Path torrentPath = this.mockRoot.resolve("multi.torrent");

        // Process the torrent file
        TorrentToCsvExporter.processTorrentFile(torrentPath);

        // Check that the CSV file was created
        final Path csvPath = this.mockRoot.resolve("multi.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");
        assertTrue(Files.exists(csvPath), "CSV file should be created");

        // Read the CSV content
        final List<String> csvLines = Files.readAllLines(csvPath);

        // Verify the CSV has the expected content
        assertEquals(3, csvLines.size(), "CSV should have 3 rows (header + 2 data rows)");
        assertEquals("InfoHash,Path,Size", csvLines.get(0), "First row should be the header");

        // The order of the files might vary, so we'll check that both files are present
        final List<String> dataRows = csvLines.subList(1, csvLines.size());
        assertTrue(dataRows.stream().anyMatch(row -> row.contains("multi/fileA.txt")), 
                "CSV should contain fileA.txt");
        assertTrue(dataRows.stream().anyMatch(row -> row.contains("multi/fileB.txt")), 
                "CSV should contain fileB.txt");

        // Check that all rows have the correct info hash
        for (final String row : dataRows) {
            assertTrue(row.startsWith("fa125f37aafa395e133c46d9aefe30e0dc66ce03,"), 
                    "Each data row should start with the correct info hash");
        }
    }

    /**
     * Test scanning a directory with torrent files
     */
    @Test
    void testScanDirectory() throws IOException {
        // Scan the root directory
        final int count = TorrentToCsvExporter.scanDirectory(this.mockRoot);

        // Should find and process 4 torrent files (2 in root, 2 in nested)
        assertEquals(4, count, "Should find and process 4 torrent files");

        // Check that the CSV files were created
        final Path singleFileCsv = this.mockRoot.resolve("single_file.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path multiFileCsv = this.mockRoot.resolve("multi.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");
        final Path nestedSingleFileCsv = this.mockRoot.resolve("nested/single_file.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path nestedMultiFileCsv = this.mockRoot.resolve("nested/multi.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");

        assertTrue(Files.exists(singleFileCsv), "Single file CSV should be created");
        assertTrue(Files.exists(multiFileCsv), "Multi-file CSV should be created");
        assertTrue(Files.exists(nestedSingleFileCsv), "Nested single file CSV should be created");
        assertTrue(Files.exists(nestedMultiFileCsv), "Nested multi-file CSV should be created");
    }

    /**
     * Test scanning a directory with nested directories containing torrent files
     */
    @Test
    void testScanDirectoryWithNestedDirectories() throws IOException {
        // Scan the root directory recursively
        final int count = TorrentToCsvExporter.scanDirectory(this.mockRoot);

        // Should find and process 4 torrent files (2 in root, 2 in nested)
        assertEquals(4, count, "Should find and process 4 torrent files");

        // Check that the CSV files were created in the correct locations
        final Path rootSingleFileCsv = this.mockRoot.resolve("single_file.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path rootMultiFileCsv = this.mockRoot.resolve("multi.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");
        final Path nestedSingleFileCsv = this.mockRoot.resolve("nested/single_file.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path nestedMultiFileCsv = this.mockRoot.resolve("nested/multi.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");

        assertTrue(Files.exists(rootSingleFileCsv), "Root single file CSV should be created");
        assertTrue(Files.exists(rootMultiFileCsv), "Root multi-file CSV should be created");
        assertTrue(Files.exists(nestedSingleFileCsv), "Nested single file CSV should be created");
        assertTrue(Files.exists(nestedMultiFileCsv), "Nested multi-file CSV should be created");
    }

    /**
     * Test scanning a directory with an output directory specified
     */
    @Test
    void testScanDirectoryWithOutputDir() throws IOException {
        // Scan the root directory with output directory, using clobber=true to overwrite existing files
        final int count = TorrentToCsvExporter.scanDirectory(this.mockRoot, this.mockOutputDir, false, true);

        // Should find and process 4 torrent files (2 in root, 2 in nested)
        assertEquals(4, count, "Should find and process 4 torrent files");

        // Check that the CSV files were created in the output directory
        try (final var paths = Files.list(this.mockOutputDir)) {
            final List<Path> csvFiles = paths.collect(Collectors.toList());
            assertEquals(2, csvFiles.size(), "Should create 2 CSV files in the output directory (unique by info hash)");

            // Check that the CSV files have the expected names
            final List<String> fileNames = csvFiles.stream()
                                                   .map(p -> p.getFileName().toString())
                                                   .collect(Collectors.toList());

            assertTrue(fileNames.contains("single_file.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv"), 
                    "Output directory should contain single_file CSV");
            assertTrue(fileNames.contains("multi.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv"), 
                    "Output directory should contain multi CSV");
        }
    }

    /**
     * Test generating an index file
     */
    @Test
    void testGenerateIndex() throws IOException {
        // Generate an index file
        final Path indexPath = this.mockOutputDir.resolve("index.csv");
        final int count = TorrentToCsvExporter.generateIndex(this.mockRoot, indexPath, false, false);

        // Should find and process 4 torrent files (2 in root, 2 in nested)
        assertEquals(4, count, "Should find and process 4 torrent files");

        // Check that the index file was created
        assertTrue(Files.exists(indexPath), "Index file should be created");

        // Read the index file content
        final List<String> indexLines = Files.readAllLines(indexPath);

        // Verify the index file has the expected content
        assertTrue(indexLines.size() > 1, "Index should have at least a header row and data rows");
        assertEquals("InfoHash,Path,Size,TorrentFile", indexLines.get(0), "First row should be the header");

        // Check that the index contains entries for both single and multi-file torrents
        boolean hasSingleFile = false;
        boolean hasMultiFile = false;

        for (final String line : indexLines.subList(1, indexLines.size())) {
            if (line.contains("single_file.txt")) {
                hasSingleFile = true;
            }
            if (line.contains("multi/fileA.txt") || line.contains("multi/fileB.txt")) {
                hasMultiFile = true;
            }
        }

        assertTrue(hasSingleFile, "Index should contain entries for single-file torrent");
        assertTrue(hasMultiFile, "Index should contain entries for multi-file torrent");
    }
}

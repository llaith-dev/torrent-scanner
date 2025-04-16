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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the recursive scanning feature of {@link TorrentToCsvExporter}.
 * Uses Jimfs to create a mock filesystem for testing.
 */
class RecursiveScanTest {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveScanTest.class);

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

        // Create a nested directory structure for testing recursive scanning
        final Path level1A = this.mockRoot.resolve("level1A");
        final Path level1B = this.mockRoot.resolve("level1B");
        Files.createDirectories(level1A);
        Files.createDirectories(level1B);

        final Path level2A = level1A.resolve("level2A");
        final Path level2B = level1B.resolve("level2B");
        Files.createDirectories(level2A);
        Files.createDirectories(level2B);

        // Copy test resources from the real filesystem to the mock filesystem
        copyTestResources(level1A, level1B, level2A, level2B);

        // Reset statistics
        TorrentToCsvExporter.resetScanStats();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Close the file system
        this.fs.close();
    }

    /**
     * Copy test resources from the real filesystem to the mock filesystem
     */
    private void copyTestResources(final Path level1A, final Path level1B, final Path level2A, final Path level2B) throws IOException {
        // Get the path to the test resources
        final Path resourcesPath = Paths.get("src", "test", "resources", "torrent_test_data");

        // Copy single file torrent to root directory
        final Path singleFileTorrent = resourcesPath.resolve("single_file.torrent");
        final Path mockRootSingleFileTorrent = this.mockRoot.resolve("root.torrent");
        Files.copy(singleFileTorrent, mockRootSingleFileTorrent);

        // Copy multi-file torrent to level1A directory
        final Path multiFileTorrent = resourcesPath.resolve("multi.torrent");
        final Path mockLevel1ASingleFileTorrent = level1A.resolve("level1A.torrent");
        Files.copy(singleFileTorrent, mockLevel1ASingleFileTorrent);

        // Copy single file torrent to level1B directory
        final Path mockLevel1BMultiFileTorrent = level1B.resolve("level1B.torrent");
        Files.copy(multiFileTorrent, mockLevel1BMultiFileTorrent);

        // Copy single file torrent to level2A directory
        final Path mockLevel2ASingleFileTorrent = level2A.resolve("level2A.torrent");
        Files.copy(singleFileTorrent, mockLevel2ASingleFileTorrent);

        // Copy multi-file torrent to level2B directory
        final Path mockLevel2BMultiFileTorrent = level2B.resolve("level2B.torrent");
        Files.copy(multiFileTorrent, mockLevel2BMultiFileTorrent);

        // Create some non-torrent files
        Files.writeString(this.mockRoot.resolve("root.txt"), "text file");
        Files.writeString(level1A.resolve("level1A.txt"), "text file");
        Files.writeString(level2A.resolve("level2A.txt"), "text file");

        // Log what we've copied
        logger.info("Copied test resources to mock filesystem:");
        try (final var paths = Files.walk(this.mockRoot)) {
            paths.forEach(p -> logger.info("  {}", p));
        }
    }

    /**
     * Test recursive scanning (recursive=true)
     */
    @Test
    void testRecursiveScanningEnabled() throws IOException {
        // Scan the root directory with recursive=true
        final int count = TorrentToCsvExporter.scanDirectory(this.mockRoot, false, false, true);

        // Should find and process 5 torrent files (1 in root, 2 in level 1, 2 in level 2)
        assertEquals(5, count, "Should find and process 5 torrent files with recursive scanning");

        // Check that the CSV files were created in the correct locations
        final Path rootTorrentCsv = this.mockRoot.resolve("root.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path level1ATorrentCsv = this.mockRoot.resolve("level1A/level1A.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path level1BTorrentCsv = this.mockRoot.resolve("level1B/level1B.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");
        final Path level2ATorrentCsv = this.mockRoot.resolve("level1A/level2A/level2A.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path level2BTorrentCsv = this.mockRoot.resolve("level1B/level2B/level2B.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");

        assertTrue(Files.exists(rootTorrentCsv), "Root torrent CSV should be created");
        assertTrue(Files.exists(level1ATorrentCsv), "Level 1A torrent CSV should be created");
        assertTrue(Files.exists(level1BTorrentCsv), "Level 1B torrent CSV should be created");
        assertTrue(Files.exists(level2ATorrentCsv), "Level 2A torrent CSV should be created");
        assertTrue(Files.exists(level2BTorrentCsv), "Level 2B torrent CSV should be created");

        // Verify statistics
        final TorrentToCsvExporter.ScanStats stats = TorrentToCsvExporter.getScanStats();
        assertEquals(5, stats.getDirectoriesScanned(), "Should count all 5 directories");
        assertEquals(8, stats.getFilesScanned(), "Should count all 8 files");
        assertEquals(5, stats.getTorrentsProcessed(), "Should process 5 torrent files");
        assertEquals(3, stats.getSingleTorrents(), "Should count 3 single-file torrents");
        assertEquals(2, stats.getMultiTorrents(), "Should count 2 multi-file torrents");
        assertEquals(7, stats.getTotalFilesInTorrents(), "Should count 7 files in torrents (3 single + 2*2 multi)");
    }

    /**
     * Test non-recursive scanning (recursive=false)
     */
    @Test
    void testRecursiveScanningDisabled() throws IOException {
        // Scan the root directory with recursive=false
        final int count = TorrentToCsvExporter.scanDirectory(this.mockRoot, false, false, false);

        // Should find and process only 1 torrent file (the one in the root directory)
        assertEquals(1, count, "Should find and process only 1 torrent file with non-recursive scanning");

        // Check that only the root CSV file was created
        final Path rootTorrentCsv = this.mockRoot.resolve("root.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        assertTrue(Files.exists(rootTorrentCsv), "Root torrent CSV should be created");

        // Verify that the nested CSV files were not created
        final Path level1ATorrentCsv = this.mockRoot.resolve("level1A/level1A.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path level1BTorrentCsv = this.mockRoot.resolve("level1B/level1B.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");
        final Path level2ATorrentCsv = this.mockRoot.resolve("level1A/level2A/level2A.749e20ea61b692d227112035a7a1d9eb5fcbcd40.csv");
        final Path level2BTorrentCsv = this.mockRoot.resolve("level1B/level2B/level2B.fa125f37aafa395e133c46d9aefe30e0dc66ce03.csv");

        assertFalse(Files.exists(level1ATorrentCsv), "Level 1A torrent CSV should not be created");
        assertFalse(Files.exists(level1BTorrentCsv), "Level 1B torrent CSV should not be created");
        assertFalse(Files.exists(level2ATorrentCsv), "Level 2A torrent CSV should not be created");
        assertFalse(Files.exists(level2BTorrentCsv), "Level 2B torrent CSV should not be created");

        // Verify statistics
        final TorrentToCsvExporter.ScanStats stats = TorrentToCsvExporter.getScanStats();
        assertEquals(3, stats.getDirectoriesScanned(), "Should count root directory plus 2 level 1 directories");
        assertEquals(2, stats.getFilesScanned(), "Should count only files in root directory");
        assertEquals(1, stats.getTorrentsProcessed(), "Should process 1 torrent file");
        assertEquals(1, stats.getSingleTorrents(), "Should count 1 single-file torrent");
        assertEquals(0, stats.getMultiTorrents(), "Should count 0 multi-file torrents");
        assertEquals(1, stats.getTotalFilesInTorrents(), "Should count 1 file in torrents");
    }

    /**
     * Test statistics tracking
     */
    @Test
    void testStatisticsTracking() throws IOException {
        // Scan the root directory with recursive=true
        TorrentToCsvExporter.scanDirectory(this.mockRoot, false, false, true);

        // Verify statistics summary is generated
        final String summary = TorrentToCsvExporter.generateStatsSummary();
        assertTrue(summary.contains("Directories scanned: 5"), "Summary should include correct directories scanned count");
        assertTrue(summary.contains("Files scanned: 8"), "Summary should include correct files scanned count");
        assertTrue(summary.contains("Torrent files processed: 5"), "Summary should include correct torrent files processed count");
        assertTrue(summary.contains("Single-file torrents: 3"), "Summary should include correct single-file torrents count");
        assertTrue(summary.contains("Multi-file torrents: 2"), "Summary should include correct multi-file torrents count");
        assertTrue(summary.contains("Total files in torrents: 7"), "Summary should include correct total files in torrents count");
    }
}

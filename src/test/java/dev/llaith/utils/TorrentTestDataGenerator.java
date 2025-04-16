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

package dev.llaith.utils;

import bt.metainfo.MetadataService;
import bt.metainfo.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A utility class for generating test data for torrent tests.
 * <p>
 * This class provides functionality to:
 * <ul>
 *   <li>Create test files and directories for torrent testing</li>
 *   <li>Generate actual torrent files using the mktorrent command</li>
 *   <li>Generate dummy CSV files that would be expected output</li>
 * </ul>
 * <p>
 * Note: This requires the mktorrent package to be installed on the system.
 * On Fedora, you can install it with: sudo dnf install mktorrent
 */
public final class TorrentTestDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TorrentTestDataGenerator.class);

    private static final String SINGLE_FILE_CONTENT = "This is a single file torrent.\n";
    private static final String MULTI_FILE_A_CONTENT = "This is file A of a multi-file torrent.\n";
    private static final String MULTI_FILE_B_CONTENT = "This is file B of a multi-file torrent.\n";

    private final Path baseDir;

    /**
     * Creates a new TorrentTestDataGenerator with the specified base directory.
     *
     * @param baseDir the base directory where test data will be created
     */
    public TorrentTestDataGenerator(final Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Creates a new TorrentTestDataGenerator with the specified base directory.
     *
     * @param baseDir the base directory where test data will be created
     */
    public TorrentTestDataGenerator(final String baseDir) {
        this(Paths.get(baseDir));
    }

    /**
     * Creates the test data structure.
     *
     * @throws IOException if an I/O error occurs
     */
    public void createTestData() throws IOException {
        // Create base directory if it doesn't exist
        Files.createDirectories(this.baseDir);

        // Create single file
        final Path singleFile = this.baseDir.resolve("single_file.txt");
        Files.writeString(singleFile, SINGLE_FILE_CONTENT);

        // Create multi-file directory and files
        final Path multiFileDir = this.baseDir.resolve("multi");
        Files.createDirectories(multiFileDir);

        final Path fileA = multiFileDir.resolve("fileA.txt");
        final Path fileB = multiFileDir.resolve("fileB.txt");

        Files.writeString(fileA, MULTI_FILE_A_CONTENT);
        Files.writeString(fileB, MULTI_FILE_B_CONTENT);

        // We'll generate CSV files after creating torrent files
    }

    /**
     * Reads the info hash from a torrent file.
     *
     * @param torrentPath the path to the torrent file
     * @return the info hash as a hex string
     * @throws IOException if an I/O error occurs
     */
    private String getInfoHash(final Path torrentPath) throws IOException {
        try {
            // Create a MetadataService to parse the torrent file
            final MetadataService metadataService = new MetadataService();

            // Parse the torrent file using an InputStream
            try (final java.io.InputStream is = new java.io.FileInputStream(torrentPath.toFile())) {
                final Torrent torrent = metadataService.fromInputStream(is);
                return torrent.getTorrentId().toString();
            }
        } catch (final Exception e) {
            logger.error("Failed to load torrent file: {}", torrentPath, e);
            throw new IOException("Failed to load torrent file: " + e.getMessage(), e);
        }
    }

    /**
     * Generates CSV files that would be expected output, using the actual info hash from the torrent files.
     *
     * @throws IOException if an I/O error occurs
     */
    private void generateCsvFiles() throws IOException {
        // Get info hashes from torrent files
        final String singleFileInfoHash = getInfoHash(this.baseDir.resolve("single_file.torrent"));
        final String multiFileInfoHash = getInfoHash(this.baseDir.resolve("multi.torrent"));

        logger.debug("Single file info hash: {}", singleFileInfoHash);
        logger.debug("Multi file info hash: {}", multiFileInfoHash);

        // Generate CSVs with actual info hashes
        final Path csvSingle = this.baseDir.resolve("single_file." + singleFileInfoHash + ".csv");
        final Path csvMulti = this.baseDir.resolve("multi_file." + multiFileInfoHash + ".csv");

        try (final BufferedWriter writer = Files.newBufferedWriter(csvSingle, StandardCharsets.UTF_8)) {
            writer.write(singleFileInfoHash + ",single_file.txt," + SINGLE_FILE_CONTENT.length() + "\n");
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(csvMulti, StandardCharsets.UTF_8)) {
            writer.write(multiFileInfoHash + ",multi/fileA.txt," + MULTI_FILE_A_CONTENT.length() + "\n");
            writer.write(multiFileInfoHash + ",multi/fileB.txt," + MULTI_FILE_B_CONTENT.length() + "\n");
        }

        logger.info("Generated CSV files with info hashes: {} and {}", csvSingle, csvMulti);
    }

    /**
     * Creates torrent files from the test data using mktorrent.
     *
     * @throws IOException if an I/O error occurs or if mktorrent is not installed
     */
    public void createTorrentFiles() throws IOException {
        // Check if mktorrent is installed
        if (!isMktorrentInstalled()) {
            throw new IOException("mktorrent is not installed. Please install it using 'sudo dnf install mktorrent' on Fedora.");
        }

        // Create single file torrent
        createTorrent(this.baseDir.resolve("single_file.txt"), this.baseDir.resolve("single_file.torrent"));

        // Create multi-file torrent
        createTorrent(this.baseDir.resolve("multi"), this.baseDir.resolve("multi.torrent"));
    }

    /**
     * Checks if mktorrent is installed on the system.
     *
     * @return true if mktorrent is installed, false otherwise
     */
    private boolean isMktorrentInstalled() {
        try {
            final Process process = new ProcessBuilder("which", "mktorrent").start();
            final int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (final IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Creates a torrent file from the specified source using mktorrent.
     *
     * @param source the source file or directory
     * @param output the output torrent file
     * @throws IOException if an I/O error occurs
     */
    private void createTorrent(final Path source, final Path output) throws IOException {
        try {
            // Delete the output file if it already exists
            if (Files.exists(output)) {
                logger.debug("Deleting existing torrent file: {}", output);
                Files.delete(output);
            }

            // Build the mktorrent command
            final ProcessBuilder processBuilder = new ProcessBuilder(
                    "mktorrent",
                    "-o", output.toString(),
                    "-a", "http://example.com/announce",
                    source.toString()
            );

            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);

            // Start the process
            final Process process = processBuilder.start();

            // Capture the output
            final StringBuilder processOutput = new StringBuilder();
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append("\n");
                    logger.debug("mktorrent output: {}", line);
                }
            }

            // Wait for the process to complete
            final int exitCode = process.waitFor();

            // Check if the process completed successfully
            if (exitCode != 0) {
                String errorMessage = "Failed to create torrent file. mktorrent exited with code " + exitCode;
                if (processOutput.length() > 0) {
                    errorMessage += "\nProcess output:\n" + processOutput;
                }
                logger.error(errorMessage);
                throw new IOException(errorMessage);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while creating torrent file", e);
        }
    }

    /**
     * Main method for running the generator from the command line.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        try {
            final String baseDir = args.length > 0 ? args[0] : "/tmp/torrent_test_data";
            final TorrentTestDataGenerator generator = new TorrentTestDataGenerator(baseDir);

            logger.info("Creating test data in {}", baseDir);
            generator.createTestData();

            logger.info("Creating torrent files...");
            generator.createTorrentFiles();

            logger.info("Generating CSV files with info hashes...");
            generator.generateCsvFiles();

            logger.info("Done! Test data, torrent files, and CSV files created in {}", baseDir);
        } catch (final IOException e) {
            logger.error("Error: {}", e.getMessage());
            logger.error("Exception details:", e);
        }
    }
}

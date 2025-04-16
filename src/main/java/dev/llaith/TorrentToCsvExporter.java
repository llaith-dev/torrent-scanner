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

import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.TorrentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A utility class for scanning directories for torrent files and exporting their information to CSV format.
 * <p>
 * This class provides functionality to:
 * <ul>
 *   <li>Scan a directory recursively for .torrent files</li>
 *   <li>Extract information from each torrent file (info hash, name, files, sizes)</li>
 *   <li>Export this information to CSV files</li>
 * </ul>
 */
public final class TorrentToCsvExporter {
    private static final Logger logger = LoggerFactory.getLogger(TorrentToCsvExporter.class);

    /**
     * CSV header for the output files
     */
    private static final String[] CSV_HEADER = {"InfoHash", "Path", "Size"};

    /**
     * CSV header for the index file
     */
    private static final String[] INDEX_CSV_HEADER = {"InfoHash", "Path", "Size", "TorrentFile"};
    private static final Pattern TOREENT_FILENAME_PATTERN = Pattern.compile("\\.torrent$");
    private static final String ESCAPE_CHAR = "\"";


    /**
     * Private constructor to prevent instantiation of utility class
     */
    private TorrentToCsvExporter() {
        // Utility class, no instantiation
    }

    /**
     * Scans a directory for torrent files and exports their information to CSV files.
     *
     * @param directoryPath the path to the directory to scan
     * @param outputDir the directory to write CSV files to (null to write next to torrent files)
     * @param dryRun if true, no files will be written
     * @param clobber if true, existing files will be overwritten; if false, they will be skipped
     * @return the number of torrent files processed
     * @throws IOException if an I/O error occurs
     */
    public static int scanDirectory(final String directoryPath, final String outputDir, final boolean dryRun, final boolean clobber) throws IOException {

        logger.info("Scanning directory: {}", directoryPath);
        if (dryRun) {
            logger.info("Dry run mode enabled - no files will be written");
        }

        final Path root = Paths.get(directoryPath);

        if (!Files.exists(root)) {
            logger.error("Directory does not exist: {}", directoryPath);
            throw new IOException("Directory does not exist: " + directoryPath);
        }

        if (!Files.isDirectory(root)) {
            logger.error("Path is not a directory: {}", directoryPath);
            throw new IOException("Path is not a directory: " + directoryPath);
        }

        // Check if output directory exists if specified
        Path outputPath = null;
        if (outputDir != null && !outputDir.isEmpty()) {
            outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                logger.error("Output directory does not exist: {}", outputDir);
                throw new IOException("Output directory does not exist: " + outputDir);
            }
            if (!Files.isDirectory(outputPath)) {
                logger.error("Output path is not a directory: {}", outputDir);
                throw new IOException("Output path is not a directory: " + outputDir);
            }
            logger.info("Using output directory: {}", outputDir);
        }

        int processedCount = 0;

        try (final Stream<Path> pathStream = Files.walk(root)) {

            final List<Path> torrentFiles = pathStream
                    .filter(path -> path.toString().endsWith(".torrent"))
                    .toList();

            logger.info("Found {} torrent files", torrentFiles.size());

            for (final Path path : torrentFiles) {

                try {
                    processTorrentFile(path, outputPath, dryRun, clobber);
                    processedCount++;
                } catch (final IOException e) {
                    logger.error("Failed to process {}: {}", path, e.getMessage(), e);
                }

            }
        }

        logger.info("Processed {} torrent files", processedCount);

        return processedCount;

    }

    /**
     * Scans a directory for torrent files and exports their information to CSV files.
     * This is a convenience method that calls {@link #scanDirectory(String, String, boolean, boolean)} with outputDir=null.
     *
     * @param directoryPath the path to the directory to scan
     * @param dryRun if true, no files will be written
     * @param clobber if true, existing files will be overwritten; if false, they will be skipped
     * @return the number of torrent files processed
     * @throws IOException if an I/O error occurs
     */
    public static int scanDirectory(final String directoryPath, final boolean dryRun, final boolean clobber) throws IOException {
        return scanDirectory(directoryPath, null, dryRun, clobber);
    }

    /**
     * Scans a directory for torrent files and exports their information to CSV files.
     * This is a convenience method that calls {@link #scanDirectory(String, String, boolean, boolean)} with outputDir=null, dryRun=false, and clobber=false.
     *
     * @param directoryPath the path to the directory to scan
     * @return the number of torrent files processed
     * @throws IOException if an I/O error occurs
     */
    public static int scanDirectory(final String directoryPath) throws IOException {
        return scanDirectory(directoryPath, null, false, false);
    }

    /**
     * Generates an index file containing information from all processed torrent files.
     *
     * @param directoryPath the path to the directory to scan
     * @param indexFilePath the path to the index file to create
     * @param dryRun if true, no files will be written
     * @param clobber if true, existing files will be overwritten; if false, they will be skipped
     * @return the number of torrent files processed
     * @throws IOException if an I/O error occurs
     */
    public static int generateIndex(final String directoryPath, final String indexFilePath, final boolean dryRun, final boolean clobber) throws IOException {
        logger.info("Generating index file: {}", indexFilePath);

        final Path root = Paths.get(directoryPath);
        final Path indexPath = Paths.get(indexFilePath);

        if (!Files.exists(root)) {
            logger.error("Directory does not exist: {}", directoryPath);
            throw new IOException("Directory does not exist: " + directoryPath);
        }

        if (!Files.isDirectory(root)) {
            logger.error("Path is not a directory: {}", directoryPath);
            throw new IOException("Path is not a directory: " + directoryPath);
        }

        // Check if index file already exists
        if (Files.exists(indexPath) && !clobber) {
            logger.warn("Index file already exists and clobber is false: {}", indexFilePath);
            throw new IOException("Index file already exists and clobber is false: " + indexFilePath);
        }

        // Create parent directories if they don't exist
        final Path parentDir = indexPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            if (dryRun) {
                logger.info("Would create directory: {}", parentDir);
            } else {
                logger.info("Creating directory: {}", parentDir);
                Files.createDirectories(parentDir);
            }
        }

        int processedCount = 0;
        final List<String[]> indexRows = new ArrayList<>();

        // Add header row
        indexRows.add(INDEX_CSV_HEADER);

        try (final Stream<Path> pathStream = Files.walk(root)) {
            final List<Path> torrentFiles = pathStream
                    .filter(path -> path.toString().endsWith(".torrent"))
                    .toList();

            logger.info("Found {} torrent files for index", torrentFiles.size());

            for (final Path path : torrentFiles) {
                try {
                    processedCount += processTorrentFileForIndex(path, indexRows);
                } catch (final IOException e) {
                    logger.error("Failed to process {} for index: {}", path, e.getMessage(), e);
                }
            }
        }

        // Write the index file
        if (dryRun) {
            logger.info("Would write index file with {} entries to: {}", indexRows.size() - 1, indexFilePath);
            for (int i = 1; i < indexRows.size(); i++) { // Skip header
                final String[] row = indexRows.get(i);
                logger.debug("Would write index row: {},{},{},{}", 
                    escapeForCsv(row[0]), escapeForCsv(row[1]), escapeForCsv(row[2]), escapeForCsv(row[3]));
            }
        } else {
            logger.info("Writing index file with {} entries to: {}", indexRows.size() - 1, indexFilePath);
            try (final BufferedWriter writer = Files.newBufferedWriter(indexPath, StandardCharsets.UTF_8)) {
                for (final String[] row : indexRows) {
                    writer.write(escapeForCsv(row[0]) + "," + escapeForCsv(row[1]) + "," + 
                                escapeForCsv(row[2]) + "," + escapeForCsv(row[3]));
                    writer.write("\n");
                }
            }
            logger.info("Successfully wrote index file: {}", indexFilePath);
        }

        return processedCount;
    }

    /**
     * Processes a single torrent file and adds its information to the index rows.
     *
     * @param torrentPath the path to the torrent file
     * @param indexRows the list of rows to add to
     * @return the number of entries added (1 for success, 0 for failure)
     * @throws IOException if an I/O error occurs
     */
    private static int processTorrentFileForIndex(final Path torrentPath, final List<String[]> indexRows) throws IOException {
        logger.debug("Processing torrent file for index: {}", torrentPath);

        final TorrentInfo torrentInfo = getTorrentInfo(torrentPath);

        final String infoHash = torrentInfo.infoHash().toHex();
        final String name = torrentInfo.name();
        final String torrentFileName = torrentPath.getFileName().toString();

        logger.debug("Torrent info for index: hash={}, name={}, file={}", infoHash, name, torrentFileName);

        // Get file information
        final FileStorage fileStorage = torrentInfo.files();
        final int numFiles = fileStorage.numFiles();

        if (numFiles > 1) {
            // Multi-file mode
            logger.debug("Multi-file torrent with {} files for index", numFiles);

            for (int i = 0; i < numFiles; i++) {
                final String filePath = name + File.separator + fileStorage.filePath(i);
                final long fileSize = fileStorage.fileSize(i);
                indexRows.add(new String[]{infoHash, filePath, String.valueOf(fileSize), torrentFileName});
            }

        } else {
            // Single-file mode
            logger.debug("Single-file torrent for index");
            final long length = torrentInfo.totalSize();
            indexRows.add(new String[]{infoHash, name, String.valueOf(length), torrentFileName});
        }

        return 1;
    }

    /**
     * Scans a directory for torrent files and exports their information to CSV files.
     * This is a convenience method that maintains backward compatibility.
     *
     * @param directoryPath the path to the directory to scan
     * @param outputDir the directory to write CSV files to (null to write next to torrent files)
     * @param dryRun if true, no files will be written
     * @return the number of torrent files processed
     * @throws IOException if an I/O error occurs
     */
    public static int scanDirectory(final String directoryPath, final String outputDir, final boolean dryRun) throws IOException {
        return scanDirectory(directoryPath, outputDir, dryRun, false);
    }

    /**
     * Processes a single torrent file and exports its information to a CSV file.
     *
     * @param torrentPath the path to the torrent file
     * @param outputDir the directory to write CSV files to (null to write next to torrent files)
     * @param dryRun if true, no files will be written
     * @param clobber if true, existing files will be overwritten; if false, they will be skipped
     * @throws IOException if an I/O error occurs
     */
    static void processTorrentFile(final Path torrentPath, final Path outputDir, final boolean dryRun, final boolean clobber) throws IOException {

        logger.debug("Processing torrent file: {}", torrentPath);

        final TorrentInfo torrentInfo = getTorrentInfo(torrentPath);

        final String infoHash = torrentInfo.infoHash().toHex();
        final String name = torrentInfo.name();

        logger.debug("Torrent info: hash={}, name={}", infoHash, name);

        final List<String[]> rows = extractTorrentData(torrentInfo, infoHash, name);

        // Save to CSV
        final String csvFilename = generateCsvFilename(torrentPath, infoHash);
        final Path outputPath;

        if (outputDir != null) {
            outputPath = outputDir.resolve(csvFilename);

            // Check if file already exists in output directory
            if (Files.exists(outputPath) && !dryRun) {
                if (clobber) {
                    logger.debug("Overwriting existing file: {}", outputPath);
                } else {
                    logger.warn("Skipping existing file: {}", outputPath);
                    return;
                }
            }
        } else {
            outputPath = torrentPath.getParent().resolve(csvFilename);

            // Check if file already exists when writing next to torrent file
            if (Files.exists(outputPath) && !dryRun) {
                if (clobber) {
                    logger.debug("Overwriting existing file: {}", outputPath);
                } else {
                    logger.warn("Skipping existing file: {}", outputPath);
                    return;
                }
            }
        }

        writeCsv(outputPath, rows, dryRun);

        if (dryRun) {
            logger.info("Would write CSV file: {}", outputPath);
        } else {
            logger.info("Wrote CSV file: {}", outputPath);
        }
    }

    /**
     * Processes a single torrent file and exports its information to a CSV file.
     * This is a convenience method that maintains backward compatibility.
     *
     * @param torrentPath the path to the torrent file
     * @param outputDir the directory to write CSV files to (null to write next to torrent files)
     * @param dryRun if true, no files will be written
     * @throws IOException if an I/O error occurs
     */
    static void processTorrentFile(final Path torrentPath, final Path outputDir, final boolean dryRun) throws IOException {
        processTorrentFile(torrentPath, outputDir, dryRun, false);
    }

    /**
     * Processes a single torrent file and exports its information to a CSV file.
     * This is a convenience method that calls {@link #processTorrentFile(Path, Path, boolean, boolean)} with outputDir=null.
     *
     * @param torrentPath the path to the torrent file
     * @param dryRun if true, no files will be written
     * @param clobber if true, existing files will be overwritten; if false, they will be skipped
     * @throws IOException if an I/O error occurs
     */
    static void processTorrentFile(final Path torrentPath, final boolean dryRun, final boolean clobber) throws IOException {
        processTorrentFile(torrentPath, null, dryRun, clobber);
    }

    /**
     * Processes a single torrent file and exports its information to a CSV file.
     * This is a convenience method that calls {@link #processTorrentFile(Path, Path, boolean, boolean)} with outputDir=null and dryRun=false.
     *
     * @param torrentPath the path to the torrent file
     * @param clobber if true, existing files will be overwritten; if false, they will be skipped
     * @throws IOException if an I/O error occurs
     */
    static void processTorrentFile(final Path torrentPath, final boolean clobber) throws IOException {
        processTorrentFile(torrentPath, null, false, clobber);
    }

    /**
     * Processes a single torrent file and exports its information to a CSV file.
     * This is a convenience method that calls {@link #processTorrentFile(Path, Path, boolean, boolean)} with outputDir=null, dryRun=false, and clobber=false.
     *
     * @param torrentPath the path to the torrent file
     * @throws IOException if an I/O error occurs
     */
    static void processTorrentFile(final Path torrentPath) throws IOException {
        processTorrentFile(torrentPath, null, false, false);
    }

    private static TorrentInfo getTorrentInfo(final Path torrentPath) throws IOException {
        final TorrentInfo torrentInfo;
        try {

            torrentInfo = new TorrentInfo(torrentPath.toFile());

        } catch (final IllegalArgumentException e) {

            logger.error("Failed to load torrent file: {}", torrentPath, e);
            throw new IOException("Failed to load torrent file: " + e.getMessage(), e);

        }
        return torrentInfo;
    }

    /**
     * Extracts data from a torrent file into a list of rows for CSV export.
     *
     * @param torrentInfo the torrent info object
     * @param infoHash    the info hash of the torrent
     * @param name        the name of the torrent
     * @return a list of string arrays representing rows for CSV export
     */
    private static List<String[]> extractTorrentData(
            final TorrentInfo torrentInfo,
            final String infoHash,
            final String name
    ) {
        final List<String[]> rows = new ArrayList<>();

        // Add header row
        rows.add(CSV_HEADER);

        // Get file information
        final FileStorage fileStorage = torrentInfo.files();
        final int numFiles = fileStorage.numFiles();

        if (numFiles > 1) {

            // Multi-file mode
            logger.debug("Multi-file torrent with {} files", numFiles);

            for (int i = 0; i < numFiles; i++) {
                final String filePath = name + File.separator + fileStorage.filePath(i);
                final long fileSize = fileStorage.fileSize(i);
                rows.add(new String[]{infoHash, filePath, String.valueOf(fileSize)});
            }

        } else {

            // Single-file mode
            logger.debug("Single-file torrent");
            final long length = torrentInfo.totalSize();
            rows.add(new String[]{infoHash, name, String.valueOf(length)});

        }

        return rows;

    }

    /**
     * Generates a CSV filename based on the torrent file path and info hash.
     *
     * @param torrentPath the path to the torrent file
     * @param infoHash    the info hash of the torrent
     * @return the generated CSV filename
     */
    private static String generateCsvFilename(
            final Path torrentPath,
            final String infoHash
    ) {

        return TOREENT_FILENAME_PATTERN
                .matcher(torrentPath
                                 .getFileName()
                                 .toString())
                .replaceAll("") + "." + infoHash + ".csv";

    }

    /**
     * Writes data to a CSV file.
     *
     * @param path the path to the CSV file
     * @param rows the data to write
     * @param dryRun if true, no files will be written
     * @throws IOException if an I/O error occurs
     */
    private static void writeCsv(
            final Path path,
            final List<String[]> rows,
            final boolean dryRun
    ) throws IOException {
        if (dryRun) {
            logger.debug("Dry run mode: would write {} rows to {}", rows.size(), path);
            for (final String[] row : rows) {
                logger.debug("Dry run mode: would write row: {},{},{}", 
                    escapeForCsv(row[0]), escapeForCsv(row[1]), escapeForCsv(row[2]));
            }
            return;
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (final String[] row : rows) {
                writer.write(escapeForCsv(row[0]) + "," + escapeForCsv(row[1]) + "," + escapeForCsv(row[2]));
                writer.write("\n");
            }
        }
    }

    /**
     * Writes data to a CSV file.
     * This is a convenience method that calls {@link #writeCsv(Path, List, boolean)} with dryRun=false.
     *
     * @param path the path to the CSV file
     * @param rows the data to write
     * @throws IOException if an I/O error occurs
     */
    private static void writeCsv(
            final Path path,
            final List<String[]> rows
    ) throws IOException {
        writeCsv(path, rows, false);
    }

    /**
     * Escapes a string for CSV format according to RFC 4180.
     *
     * @param input the string to escape
     * @return the escaped string
     */
    private static String escapeForCsv(final String input) {

        final String string = Objects.requireNonNullElse(input, "");

        if (requiresEscaping(string))
            return ESCAPE_CHAR + string.replace(ESCAPE_CHAR, ESCAPE_CHAR + ESCAPE_CHAR) + ESCAPE_CHAR;

        return string;

    }

    /**
     * Determines if a given string requires escaping for CSV formatting.
     * A string requires escaping if it contains a comma, the escape character,
     * a newline, or a carriage return.
     *
     * @param string the string to check
     * @return true if the string requires escaping, false otherwise
     */
    private static boolean requiresEscaping(final String string) {

        return string.contains(",")
                || string.contains(ESCAPE_CHAR)
                || string.contains("\n")
                || string.contains("\r");

    }
}

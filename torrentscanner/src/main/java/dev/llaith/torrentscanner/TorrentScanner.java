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

package dev.llaith.torrentscanner;

import dev.llaith.dirscanner.core.DirScanner;
import dev.llaith.dirscanner.core.ProgressReporter;
import dev.llaith.dirscanner.core.ScanRequest;
import dev.llaith.torrentscanner.domain.TorrentHandler;

import java.io.IOException;

/**
 * High-level service class that orchestrates torrent file scanning operations.
 * 
 * <p>This class provides a simplified interface for torrent scanning by integrating
 * the generic {@link DirScanner} framework with torrent-specific processing logic.
 * It handles the coordination between file discovery, processing, and post-processing
 * operations like batch flushing.
 * 
 * <p>The TorrentScanner encapsulates the complexity of the framework integration,
 * allowing clients to perform torrent scanning with a simple method call. It
 * automatically handles the lifecycle of scanning operations including setup,
 * processing, and cleanup.
 * 
 * <h3>Key Responsibilities:</h3>\n * <ul>\n *   <li>Coordinating between DirScanner framework and TorrentHandler</li>\n *   <li>Managing the scanning lifecycle from start to completion</li>\n *   <li>Handling post-processing operations (batch flushing)</li>\n *   <li>Providing a clean, high-level API for torrent scanning</li>\n * </ul>\n * \n * <h3>Usage Example:</h3>\n * <pre>{@code\n * // Create progress reporter (CLI, programmatic, etc.)\n * ProgressReporter reporter = new ConsoleReporter(true, VerbosityLevel.NORMAL);\n * \n * // Create scanner with reporter\n * TorrentScanner scanner = new TorrentScanner(reporter);\n * \n * // Build scan request\n * ScanRequest request = new ScanRequest.Builder()\n *     .directoryPath(Paths.get(\"/path/to/torrents\"))\n *     .outputDirectory(Paths.get(\"/path/to/output\"))\n *     .verbosity(VerbosityLevel.VERBOSE)\n *     .build();\n * \n * // Execute scan\n * try {\n *     scanner.scan(request);\n *     System.out.println(\"Scan completed successfully\");\n * } catch (IOException e) {\n *     System.err.println(\"Scan failed: \" + e.getMessage());\n * }\n * }</pre>\n * \n * <h3>Index Mode Processing:</h3>\n * <pre>{@code\n * // Index mode with consolidated output\n * ScanRequest indexRequest = new ScanRequest.Builder()\n *     .directoryPath(Paths.get(\"/large/torrent/collection\"))\n *     .indexFilePath(Paths.get(\"/output/master-index.csv\"))\n *     .build();\n * \n * scanner.scan(indexRequest);\n * // Batch data is automatically flushed to the index file\n * }</pre>\n * \n * <h3>Architecture Integration:</h3>\n * <p>This class sits between the transport layer (CLI commands) and the core\n * framework, providing a domain-specific service interface. It demonstrates\n * how to build higher-level services on top of the DirScanner framework.</p>\n * \n * @see DirScanner\n * @see dev.llaith.torrentscanner.domain.TorrentHandler\n * @see dev.llaith.dirscanner.core.ScanRequest\n * @see dev.llaith.dirscanner.core.ProgressReporter\n */
public final class TorrentScanner {
    
    private final DirScanner dirScanner;
    private final TorrentHandler torrentHandler;
    
    /**
     * Creates a new TorrentScanner with the specified progress reporter.
     * 
     * <p>The progress reporter will receive all status updates, error reports,
     * and completion notifications during scanning operations. Different reporter
     * implementations can provide CLI output, programmatic callbacks, or other
     * feedback mechanisms.
     * 
     * <h3>Example reporters:</h3>
     * <pre>{@code
     * // Console output with colors and verbose logging
     * ProgressReporter cliReporter = new ConsoleReporter(true, VerbosityLevel.VERBOSE);
     * TorrentScanner scanner = new TorrentScanner(cliReporter);
     * 
     * // Custom programmatic reporter
     * ProgressReporter customReporter = new ProgressReporter() {
     *     private final ScanStatus status = new ScanStatus();
     *     
     *     @Override
     *     public Path reportProcessed(Path file, String message) {
     *         status.incrementFilesProcessed();
     *         updateUI("Processed: " + file.getFileName());
     *         return file;
     *     }
     *     // ... other methods
     * };
     * }</pre>
     * 
     * @param progressReporter the reporter for tracking scan progress and errors (must not be null)
     * @throws IllegalArgumentException if progressReporter is null
     */
    public TorrentScanner(final ProgressReporter progressReporter) {
        if (progressReporter == null) {
            throw new IllegalArgumentException("Progress reporter must not be null");
        }
        this.torrentHandler = new TorrentHandler();
        this.dirScanner = new DirScanner(this.torrentHandler, progressReporter);
    }
    
    /**
     * Scans a directory for torrent files and processes them according to the scan request.
     * 
     * <p>This method performs the complete torrent scanning operation:
     * <ol>
     *   <li>Discovers .torrent files in the specified directory</li>
     *   <li>Parses each torrent file to extract metadata</li>
     *   <li>Generates CSV output according to the request parameters</li>
     *   <li>Flushes any remaining batch data if in index mode</li>
     * </ol>
     * 
     * <p>The scan respects all options in the request including dry-run mode,
     * verbosity levels, output directories, and file overwrite behavior.
     * 
     * <h3>Operation Modes:</h3>
     * <ul>
     *   <li><strong>Individual mode:</strong> Creates separate CSV files for each torrent</li>
     *   <li><strong>Index mode:</strong> Consolidates all data into a single CSV file</li>
     * </ul>
     * 
     * <h3>Error Handling:</h3>
     * <p>Individual file processing errors are reported through the progress reporter
     * and don't stop the overall scan. IOException is thrown only for directory
     * access problems or critical I/O failures that prevent the scan from continuing.</p>
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * ScanRequest request = new ScanRequest.Builder()
     *     .directoryPath(Paths.get("/downloads/torrents"))
     *     .outputDirectory(Paths.get("/processed"))
     *     .verbosity(VerbosityLevel.NORMAL)
     *     .build();
     * 
     * try {
     *     scanner.scan(request);
     *     ScanStatus status = progressReporter.scanStatus();
     *     System.out.printf("Processed %d files, %d errors\n",
     *         status.getFilesProcessed(), status.getErrors());
     * } catch (IOException e) {
     *     System.err.println("Scan failed: " + e.getMessage());
     * }
     * }</pre>
     * 
     * @param request the scan request containing directory path and processing options (must not be null)
     * @throws IOException if the directory cannot be accessed or critical I/O operations fail
     * @throws IllegalArgumentException if request is null
     * @see ScanRequest
     * @see ScanRequest.Builder
     */
    public void scan(final ScanRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("Scan request must not be null");
        }
        
        this.dirScanner.scan(request);
        
        // Flush any remaining index batch if in index mode
        if (request.isIndexMode()) {
            this.torrentHandler.flushIndexBatch(request);
        }
    }
}
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

package dev.llaith.dirscanner.core;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Core directory scanning functionality for the DirScanner framework.
 * 
 * <p>This class provides transport-agnostic directory scanning that can be used
 * by CLI, event-based, or other transport implementations. It handles the core
 * scanning logic without any UI or transport-specific concerns, making it
 * suitable for integration into various environments.
 * 
 * <p>The scanner works by:
 * <ol>
 *   <li>Finding files that match the handler's glob pattern</li>
 *   <li>Filtering files using the handler's predicate</li>
 *   <li>Processing each matched file via the handler</li>
 *   <li>Reporting progress through the progress reporter</li>
 * </ol>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a handler for your file type
 * DirScannerHandler handler = new TorrentHandler();
 * 
 * // Create a progress reporter (CLI, event-based, etc.)
 * ProgressReporter reporter = new ConsoleReporter(true, VerbosityLevel.NORMAL);
 * 
 * // Create the scanner
 * DirScanner scanner = new DirScanner(handler, reporter);
 * 
 * // Build a scan request
 * ScanRequest request = new ScanRequest.Builder()
 *     .directoryPath(Paths.get("/path/to/scan"))
 *     .verbosity(VerbosityLevel.VERBOSE)
 *     .dryRun(false)
 *     .build();
 * 
 * // Execute the scan
 * try {
 *     scanner.scan(request);
 *     System.out.println("Scan completed successfully");
 * } catch (IOException e) {
 *     System.err.println("Scan failed: " + e.getMessage());
 * }
 * }</pre>
 * 
 * <h3>Integration with Transport Layers:</h3>
 * <pre>{@code
 * // CLI Transport
 * public class CliTransport {
 *     public void executeCommand(ScanRequest request) throws IOException {
 *         DirScanner scanner = new DirScanner(handler, consoleReporter);
 *         scanner.scan(request);
 *     }
 * }
 * 
 * // Event Transport
 * public class EventTransport {
 *     public void processEvent(ScanEvent event) throws IOException {
 *         DirScanner scanner = new DirScanner(handler, eventReporter);
 *         scanner.scan(event.toScanRequest());
 *     }
 * }
 * }</pre>
 * 
 * @see DirScannerHandler
 * @see ProgressReporter
 * @see ScanRequest
 */
public class DirScanner {
    
    /**
     * The handler responsible for processing matched files.
     * Made protected to allow subclasses to access the handler if needed.
     */
    protected final DirScannerHandler handler;
    
    /**
     * The progress reporter for tracking scan progress and errors.
     */
    private final ProgressReporter progressReporter;
    
    /**
     * Creates a new DirScanner with the specified handler and progress reporter.
     * 
     * <p>The handler defines which files to process and how to process them,
     * while the progress reporter handles status updates and error reporting.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * DirScannerHandler handler = new TorrentHandler();
     * ProgressReporter reporter = new ConsoleReporter(true, VerbosityLevel.NORMAL);
     * DirScanner scanner = new DirScanner(handler, reporter);
     * }</pre>
     * 
     * @param handler the file processing handler (must not be null)
     * @param progressReporter the progress reporter (must not be null)
     * @throws IllegalArgumentException if handler or progressReporter is null
     */
    public DirScanner(
            final DirScannerHandler handler,
            final ProgressReporter progressReporter) {
        
        if (handler == null) {
            throw new IllegalArgumentException("Handler must not be null");
        }
        if (progressReporter == null) {
            throw new IllegalArgumentException("Progress reporter must not be null");
        }
        
        this.handler = handler;
        this.progressReporter = progressReporter;
    }
    
    /**
     * Scans a directory for files and processes them according to the scan request.
     * 
     * <p>This method performs the core scanning operation by finding files that match
     * the handler's criteria and processing each matched file. The scan respects all
     * options specified in the scan request, including dry-run mode and verbosity levels.
     * 
     * <p>The scanning process:
     * <ol>
     *   <li>Validates the target directory exists and is accessible</li>
     *   <li>Finds files matching the handler's glob pattern</li>
     *   <li>Filters files using the handler's predicate</li>
     *   <li>Processes each matched file via the handler</li>
     *   <li>Reports progress and errors through the progress reporter</li>
     * </ol>
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * ScanRequest request = new ScanRequest.Builder()
     *     .directoryPath(Paths.get("/home/user/downloads"))
     *     .outputDirectory(Paths.get("/home/user/processed"))
     *     .verbosity(VerbosityLevel.VERBOSE)
     *     .dryRun(false)
     *     .overwrite(true)
     *     .build();
     * 
     * try {
     *     scanner.scan(request);
     *     System.out.println("Scan completed successfully");
     * } catch (IOException e) {
     *     System.err.println("Scan failed: " + e.getMessage());
     * }
     * }</pre>
     * 
     * @param request the scan request containing configuration and target directory (must not be null)
     * @throws IOException if the directory cannot be accessed or scanned
     * @throws IllegalArgumentException if request is null
     */
    public void scan(final ScanRequest request) throws IOException {
        
        if (request == null) {
            throw new IllegalArgumentException("Scan request must not be null");
        }
        
        final List<Path> matchedFiles = findMatchingFiles(
                request.getDirectoryPath(),
                this.handler.searchGlob(),
                this.handler.filter());
        
        for (final Path file : matchedFiles) {
            this.handler.handleMatched(
                    request,
                    this.progressReporter,
                    file);
        }
    }
    
    /**
     * Finds all files matching the handler's criteria in the specified directory.
     * 
     * <p>This method performs the file discovery phase of the scanning process.
     * It uses a {@link DirectoryStream} with a glob pattern for efficient file
     * matching, then applies an additional predicate filter for fine-grained control.
     * 
     * <p>The method validates that the target path is a directory before scanning
     * and handles resource management automatically via try-with-resources.
     * 
     * @param directoryPath the directory to scan (must exist and be a directory)
     * @param glob the glob pattern to match files against
     * @param filter additional predicate filter applied after glob matching
     * @return list of file paths that match both the glob pattern and filter
     * @throws IOException if the directory cannot be read or is not a directory
     */
    private List<Path> findMatchingFiles(
            final Path directoryPath,
            final String glob,
            final Predicate<Path> filter) throws IOException {
        
        if (!Files.isDirectory(directoryPath)) {
            throw new IOException("Path is not a directory: " + directoryPath);
        }
        
        final List<Path> paths = new ArrayList<>();
        
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, glob)) {
            for (final Path entry : stream) {
                if (filter.test(entry)) {
                    paths.add(entry);
                }
            }
        }
        
        return paths;
    }
}
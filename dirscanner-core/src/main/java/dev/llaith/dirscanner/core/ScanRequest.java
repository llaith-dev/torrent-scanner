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

import java.nio.file.Path;

/**
 * Immutable request object containing configuration for directory scanning operations.
 * 
 * <p>This class represents all the parameters and options needed to perform a directory
 * scan operation. It encapsulates the target directory, output settings, execution modes,
 * and verbosity levels in an immutable data structure.
 * 
 * <p>The scan request is transport-agnostic and can be used by CLI, event-based,
 * or programmatic interfaces. All configuration validation is performed at build time
 * to ensure requests are valid before execution begins.
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Basic scan request
 * ScanRequest request = new ScanRequest.Builder()
 *     .directoryPath(Paths.get("/home/user/downloads"))
 *     .verbosity(VerbosityLevel.NORMAL)
 *     .build();
 * 
 * // Advanced scan with output directory and dry-run
 * ScanRequest request = new ScanRequest.Builder()
 *     .directoryPath(Paths.get("/home/user/torrents"))
 *     .outputDirectory(Paths.get("/home/user/processed"))
 *     .dryRun(true)
 *     .overwrite(false)
 *     .verbosity(VerbosityLevel.VERBOSE)
 *     .build();
 * 
 * // Index mode scan (consolidated output)
 * ScanRequest request = new ScanRequest.Builder()
 *     .directoryPath(Paths.get("/media/torrents"))
 *     .indexFilePath(Paths.get("/output/master-index.csv"))
 *     .verbosity(VerbosityLevel.QUIET)
 *     .build();
 * 
 * // Use with scanner
 * DirScanner scanner = new DirScanner(handler, reporter);
 * scanner.scan(request);
 * }</pre>
 * 
 * @see ScanRequest.Builder
 * @see DirScanner#scan(ScanRequest)
 * @see VerbosityLevel
 */
public final class ScanRequest {
    
    private final Path directoryPath;
    private final Path outputDirectory;
    private final Path indexFilePath;
    private final boolean dryRun;
    private final boolean overwrite;
    private final VerbosityLevel verbosity;
    
    public ScanRequest(
            final Path directoryPath,
            final Path outputDirectory,
            final Path indexFilePath,
            final boolean dryRun,
            final boolean overwrite,
            final VerbosityLevel verbosity) {
        
        this.directoryPath = directoryPath;
        this.outputDirectory = outputDirectory;
        this.indexFilePath = indexFilePath;
        this.dryRun = dryRun;
        this.overwrite = overwrite;
        this.verbosity = verbosity;
    }
    
    /**
     * Gets the directory path to scan for files.
     * 
     * <p>This is the root directory where the scanning operation will begin.
     * The scanner will look for files matching the handler's criteria within
     * this directory.
     * 
     * @return the directory path to scan (never null)
     */
    public Path getDirectoryPath() {
        return this.directoryPath;
    }
    
    /**
     * Gets the output directory for processed files.
     * 
     * <p>When set, processed files will be written to this directory instead
     * of the default location. If null, the handler determines the output location.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * Path outputDir = request.getOutputDirectory();
     * if (outputDir != null) {
     *     Path outputFile = outputDir.resolve(processedFileName);
     *     writeProcessedData(outputFile, data);
     * }
     * }</pre>
     * 
     * @return the output directory path, or null if not specified
     */
    public Path getOutputDirectory() {
        return this.outputDirectory;
    }
    
    /**
     * Gets the index file path for consolidated output.
     * 
     * <p>When set, indicates that all processed data should be written to a single
     * consolidated index file rather than individual files per input. This is useful
     * for creating master catalogs or unified reports.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * if (request.isIndexMode()) {
     *     Path indexFile = request.getIndexFilePath();
     *     try (FileWriter writer = new FileWriter(indexFile.toFile())) {
     *         // Write consolidated data
     *     }
     * }
     * }</pre>
     * 
     * @return the index file path, or null if not in index mode
     * @see #isIndexMode()
     */
    public Path getIndexFilePath() {
        return this.indexFilePath;
    }
    
    /**
     * Checks if this is a dry-run operation.
     * 
     * <p>In dry-run mode, handlers should simulate all operations without
     * actually writing files or making persistent changes. This is useful
     * for testing and validation.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * if (!request.isDryRun()) {
     *     Files.write(outputPath, processedData);
     *     reporter.reportProcessed(file, "wrote " + data.size() + " bytes");
     * } else {
     *     reporter.reportProcessed(file, "would write " + data.size() + " bytes (dry-run)");
     * }
     * }</pre>
     * 
     * @return true if this is a dry-run operation, false otherwise
     */
    public boolean isDryRun() {
        return this.dryRun;
    }
    
    /**
     * Checks if existing files should be overwritten.
     * 
     * <p>When true, handlers should overwrite existing output files without
     * prompting. When false, existing files should be skipped or renamed
     * to avoid conflicts.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * if (Files.exists(outputPath) && !request.shouldOverwrite()) {
     *     reporter.reportSkipped(file, "output file exists (no-clobber mode)");
     *     return;
     * }
     * Files.write(outputPath, processedData);
     * }</pre>
     * 
     * @return true if existing files should be overwritten, false otherwise
     */
    public boolean shouldOverwrite() {
        return this.overwrite;
    }
    
    /**
     * Checks if the scan should operate in index mode.
     * 
     * <p>Index mode consolidates all processed data into a single output file
     * rather than creating individual files for each input. This is determined
     * by whether an index file path has been specified.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * if (request.isIndexMode()) {
     *     // Batch processing mode - accumulate data
     *     batchData.add(processedRecord);
     *     if (batchData.size() >= BATCH_SIZE) {
     *         flushBatchToIndex(request.getIndexFilePath(), batchData);
     *     }
     * } else {
     *     // Individual file mode
     *     writeIndividualFile(outputPath, processedData);
     * }
     * }</pre>
     * 
     * @return true if operating in index mode, false for individual file mode
     * @see #getIndexFilePath()
     */
    public boolean isIndexMode() {
        return this.indexFilePath != null;
    }
    
    /**
     * Gets the verbosity level for output and logging.
     * 
     * <p>The verbosity level controls how much detail is included in progress
     * reporting and logging output. Handlers and reporters should respect this
     * setting when determining what information to display.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * if (request.getVerbosity().ordinal() >= VerbosityLevel.VERBOSE.ordinal()) {
     *     reporter.reportProcessed(file, "extracted " + recordCount + " records, " +
     *                             "processed in " + elapsedMs + "ms");
     * } else {
     *     reporter.reportProcessed(file, "processed");
     * }
     * }</pre>
     * 
     * @return the verbosity level (never null, defaults to NORMAL)
     * @see VerbosityLevel
     */
    public VerbosityLevel getVerbosity() {
        return this.verbosity;
    }
    
    /**
     * Builder for creating immutable ScanRequest instances.
     * 
     * <p>This builder provides a fluent interface for constructing scan requests
     * with validation. All parameters have sensible defaults, and only the directory
     * path is required.
     * 
     * <h3>Usage Example:</h3>
     * <pre>{@code
     * // Minimal request
     * ScanRequest request = new ScanRequest.Builder()
     *     .directoryPath(Paths.get("/path/to/scan"))
     *     .build();
     * 
     * // Full configuration
     * ScanRequest request = new ScanRequest.Builder()
     *     .directoryPath(Paths.get("/input"))
     *     .outputDirectory(Paths.get("/output"))
     *     .indexFilePath(Paths.get("/index.csv"))
     *     .dryRun(true)
     *     .overwrite(false)
     *     .verbosity(VerbosityLevel.VERBOSE)
     *     .build();
     * }</pre>
     * 
     * @see ScanRequest
     */
    public static final class Builder {
        private Path directoryPath = null;
        private Path outputDirectory = null;
        private Path indexFilePath = null;
        private boolean dryRun = false;
        private boolean overwrite = false;
        private VerbosityLevel verbosity = VerbosityLevel.NORMAL;
        
        /**
         * Sets the directory path to scan.
         * 
         * <p>This is the only required parameter for building a scan request.
         * The directory must exist and be readable when the scan is executed.
         * 
         * @param directoryPath the directory to scan (must not be null)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if directoryPath is null
         */
        public Builder directoryPath(final Path directoryPath) {
            this.directoryPath = directoryPath;
            return this;
        }
        
        /**
         * Sets the output directory for processed files.
         * 
         * <p>When set, all output files will be written to this directory.
         * If not set, handlers will use their default output location logic.
         * 
         * @param outputDirectory the directory for output files (can be null)
         * @return this builder for method chaining
         */
        public Builder outputDirectory(final Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }
        
        /**
         * Sets the index file path for consolidated output.
         * 
         * <p>When set, enables index mode where all processed data is written
         * to a single consolidated file rather than individual files.
         * 
         * @param indexFilePath the path for the consolidated index file (can be null)
         * @return this builder for method chaining
         */
        public Builder indexFilePath(final Path indexFilePath) {
            this.indexFilePath = indexFilePath;
            return this;
        }
        
        /**
         * Sets whether this should be a dry-run operation.
         * 
         * <p>In dry-run mode, handlers simulate operations without making
         * actual file changes. Useful for testing and validation.
         * 
         * @param dryRun true for dry-run mode, false for normal operation
         * @return this builder for method chaining
         */
        public Builder dryRun(final boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }
        
        /**
         * Sets whether existing files should be overwritten.
         * 
         * <p>When true, existing output files will be overwritten without prompting.
         * When false, existing files will be skipped to prevent data loss.
         * 
         * @param overwrite true to overwrite existing files, false to skip them
         * @return this builder for method chaining
         */
        public Builder overwrite(final boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }
        
        /**
         * Sets the verbosity level for output and logging.
         * 
         * <p>Controls the amount of detail in progress reporting and logging.
         * Defaults to NORMAL if not specified.
         * 
         * @param verbosity the verbosity level (can be null, defaults to NORMAL)
         * @return this builder for method chaining
         */
        public Builder verbosity(final VerbosityLevel verbosity) {
            this.verbosity = verbosity;
            return this;
        }
        
        /**
         * Builds the immutable ScanRequest instance.
         * 
         * <p>Validates that all required parameters are set and creates the final
         * scan request object. The directory path is the only required parameter.
         * 
         * <h3>Example:</h3>
         * <pre>{@code
         * ScanRequest request = new ScanRequest.Builder()
         *     .directoryPath(Paths.get("/data"))
         *     .verbosity(VerbosityLevel.VERBOSE)
         *     .build();
         * }</pre>
         * 
         * @return the configured scan request
         * @throws IllegalArgumentException if directory path is not set
         */
        public ScanRequest build() {
            if (this.directoryPath == null) {
                throw new IllegalArgumentException("Directory path is required");
            }
            
            return new ScanRequest(
                    this.directoryPath,
                    this.outputDirectory,
                    this.indexFilePath,
                    this.dryRun,
                    this.overwrite,
                    this.verbosity);
        }
    }
}
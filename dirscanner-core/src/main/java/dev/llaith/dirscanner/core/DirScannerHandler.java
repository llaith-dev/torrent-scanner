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
import java.util.function.Predicate;

/**
 * Interface for handling directory scanning operations.
 * 
 * <p>Implementations define how to discover and process files during a directory scan.
 * Each handler is responsible for specifying file matching criteria and processing logic.
 * This interface is the core extension point for the DirScanner framework.
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class ImageHandler implements DirScannerHandler {
 *     @Override
 *     public String searchGlob() {
 *         return "*.{jpg,jpeg,png,gif}";
 *     }
 *     
 *     @Override
 *     public Predicate<Path> filter() {
 *         return Files::isRegularFile;
 *     }
 *     
 *     @Override
 *     public void handleMatched(ScanRequest request, ProgressReporter reporter, Path file) {
 *         try {
 *             // Process image file
 *             BufferedImage image = ImageIO.read(file.toFile());
 *             // Extract and save metadata
 *             reporter.reportProcessed(file, "processed image");
 *         } catch (IOException e) {
 *             reporter.reportErrored(file, "failed to process image", e);
 *         }
 *     }
 * }
 * 
 * // Usage in a plugin or application
 * DirScannerHandler handler = new ImageHandler();
 * DirScanner scanner = new DirScanner(handler, progressReporter);
 * scanner.scan(scanRequest);
 * }</pre>
 * 
 * @see DirScanner
 * @see ScanRequest
 * @see ProgressReporter
 */
public interface DirScannerHandler {
    
    /**
     * Returns the glob pattern used to match files during directory scanning.
     * 
     * <p>The pattern follows standard glob syntax supported by {@link java.nio.file.DirectoryStream}.
     * Common patterns include single file extensions, multiple extensions, and recursive patterns.
     * 
     * <h3>Example patterns:</h3>
     * <ul>
     *   <li>{@code "*.torrent"} - All .torrent files</li>
     *   <li>{@code "*.{jpg,jpeg,png}"} - Multiple image formats</li>
     *   <li>{@code "**{@literal /}*.java"} - All Java files recursively (if supported)</li>
     *   <li>{@code "data_*.csv"} - CSV files with specific prefix</li>
     * </ul>
     * 
     * @return the glob pattern string for file matching
     */
    String searchGlob();
    
    /**
     * Returns a predicate to filter matched files based on additional criteria.
     * 
     * <p>This filter is applied after glob pattern matching to provide fine-grained
     * control over which files are processed. Common use cases include checking file
     * permissions, size limits, or modification dates.
     * 
     * <h3>Example filters:</h3>
     * <pre>{@code
     * // Only regular files (exclude directories, symlinks)
     * return Files::isRegularFile;
     * 
     * // Files larger than 1MB
     * return path -> {
     *     try {
     *         return Files.size(path) > 1024 * 1024;
     *     } catch (IOException e) {
     *         return false;
     *     }
     * };
     * 
     * // Files modified in the last 24 hours
     * return path -> {
     *     try {
     *         Instant lastModified = Files.getLastModifiedTime(path).toInstant();
     *         return lastModified.isAfter(Instant.now().minus(Duration.ofDays(1)));
     *     } catch (IOException e) {
     *         return false;
     *     }
     * };
     * }</pre>
     * 
     * @return predicate that returns true if the file should be processed
     */
    Predicate<Path> filter();
    
    /**
     * Handles a matched file during scanning.
     * 
     * <p>This method is called for each file that matches both the glob pattern
     * and the filter predicate. Implementations should process the file and report
     * progress using the provided reporter.
     * 
     * <p><strong>Important:</strong> This method should handle all exceptions internally
     * and report errors via the progress reporter rather than throwing exceptions.
     * 
     * <h3>Example implementation:</h3>
     * <pre>{@code
     * @Override
     * public void handleMatched(ScanRequest request, ProgressReporter reporter, Path file) {
     *     try {
     *         // Validate file can be processed
     *         if (!Files.isReadable(file)) {
     *             reporter.reportSkipped(file, "file not readable");
     *             return;
     *         }
     *         
     *         // Process the file
     *         MyFileData data = parseFile(file);
     *         
     *         // Write output (respect dry-run mode)
     *         if (!request.isDryRun()) {
     *             Path outputPath = determineOutputPath(file, request);
     *             writeProcessedData(outputPath, data);
     *         }
     *         
     *         reporter.reportProcessed(file, "parsed " + data.getRecordCount() + " records");
     *         
     *     } catch (ParseException e) {
     *         reporter.reportErrored(file, "parsing failed", e);
     *     } catch (IOException e) {
     *         reporter.reportErrored(file, "I/O error", e);
     *     }
     * }
     * }</pre>
     * 
     * @param request the scan request containing configuration options
     * @param progressReporter reporter for tracking progress, skipped files, and errors
     * @param file the matched file to process
     */
    void handleMatched(ScanRequest request, ProgressReporter progressReporter, Path file);
}
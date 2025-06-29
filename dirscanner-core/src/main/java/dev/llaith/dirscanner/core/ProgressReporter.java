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
 * Interface for reporting progress during directory scanning operations.
 * 
 * <p>Implementations of this interface handle progress reporting for the DirScanner framework,
 * providing feedback about file processing status, errors, and overall scan statistics.
 * The interface supports various transport layers including CLI, event-based systems,
 * and programmatic usage.
 * 
 * <p>Progress reporters are responsible for:
 * <ul>
 *   <li>Tracking and displaying scan progress</li>
 *   <li>Maintaining statistics (processed, skipped, errors)</li>
 *   <li>Handling error reporting and logging</li>
 *   <li>Providing transport-appropriate feedback (console, events, etc.)</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class MyProgressReporter implements ProgressReporter {
 *     private final ScanStatus scanStatus = new ScanStatus();
 *     
 *     @Override
 *     public Path reportProcessed(Path file, String message) {
 *         scanStatus.incrementFilesProcessed();
 *         System.out.println("✓ Processed: " + file + " - " + message);
 *         return file;
 *     }
 *     
 *     @Override
 *     public Path reportSkipped(Path file, String message) {
 *         scanStatus.incrementFilesSkipped();
 *         System.out.println("⊘ Skipped: " + file + " - " + message);
 *         return file;
 *     }
 *     
 *     @Override
 *     public Path reportErrored(Path file, String message, Throwable throwable) {
 *         scanStatus.incrementErrors();
 *         System.err.println("✗ Error: " + file + " - " + message);
 *         throwable.printStackTrace();
 *         return file;
 *     }
 *     
 *     @Override
 *     public ScanStatus scanStatus() {
 *         return scanStatus;
 *     }
 * }
 * 
 * // Usage
 * ProgressReporter reporter = new MyProgressReporter();
 * DirScanner scanner = new DirScanner(handler, reporter);
 * scanner.scan(request);
 * 
 * // Get final statistics
 * ScanStatus status = reporter.scanStatus();
 * System.out.println("Processed: " + status.getFilesProcessed());
 * }</pre>
 * 
 * @see ScanStatus
 * @see DirScanner
 * @see dev.llaith.dirscanner.transport.cli.ConsoleReporter
 */
public interface ProgressReporter {
    
    /**
     * Reports that a file has been successfully processed.
     * 
     * <p>This method is called when a file has been successfully processed by the handler.
     * Implementations should update their internal statistics and provide appropriate
     * feedback to the user or system.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * Path processedFile = reporter.reportProcessed(
     *     Paths.get("/path/to/file.torrent"), 
     *     "extracted metadata for 15 files"
     * );
     * }</pre>
     * 
     * @param file the file that was successfully processed
     * @param message descriptive message about what was accomplished
     * @return the file path (for method chaining and convenience)
     */
    Path reportProcessed(Path file, String message);
    
    /**
     * Reports that a file was skipped during processing.
     * 
     * <p>This method is called when a file matched the glob pattern but was skipped
     * for some reason (e.g., already exists, invalid format, permission denied).
     * Implementations should update their skip statistics and provide appropriate feedback.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * reporter.reportSkipped(
     *     Paths.get("/path/to/output.csv"), 
     *     "file exists (no-clobber mode)"
     * );
     * }</pre>
     * 
     * @param file the file that was skipped
     * @param message reason why the file was skipped
     * @return the file path (for method chaining and convenience)
     */
    Path reportSkipped(Path file, String message);
    
    /**
     * Reports an error that occurred while processing a file.
     * 
     * <p>This method is called when an error occurs during file processing.
     * Implementations should update their error statistics and provide appropriate
     * error feedback to the user or logging system.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * reporter.reportErrored(
     *     Paths.get("/path/to/corrupt.torrent"), 
     *     "failed to parse torrent metadata"
     * );
     * }</pre>
     * 
     * @param file the file that caused the error
     * @param message description of what went wrong
     * @return the file path (for method chaining and convenience)
     */
    Path reportErrored(Path file, String message);
    
    /**
     * Reports an error with full exception details.
     * 
     * <p>This method is called when an exception occurs during file processing.
     * Implementations should update their error statistics and provide detailed
     * error information including stack traces if appropriate for the transport layer.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * try {
     *     processFile(file);
     * } catch (IOException e) {
     *     reporter.reportErrored(file, "I/O error during processing", e);
     * }
     * }</pre>
     * 
     * @param file the file that caused the error
     * @param message description of what went wrong
     * @param throwable the exception that occurred
     * @return the file path (for method chaining and convenience)
     */
    Path reportErrored(Path file, String message, Throwable throwable);
    
    /**
     * Gets the current scan status with detailed statistics.
     * 
     * <p>Returns a {@link ScanStatus} object containing real-time statistics about
     * the current scanning operation, including counts of processed, skipped, and
     * errored files, as well as timing information.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * ScanStatus status = reporter.scanStatus();
     * System.out.println("Progress: " + status.getFilesProcessed() + " processed, " +
     *                    status.getFilesSkipped() + " skipped, " +
     *                    status.getErrors() + " errors");
     * System.out.println("Elapsed time: " + status.getElapsedTime() + "ms");
     * }</pre>
     * 
     * @return current scan status with statistics and timing information
     */
    ScanStatus scanStatus();
}
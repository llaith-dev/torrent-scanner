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

/**
 * Mutable status tracker for directory scanning operations.
 * 
 * <p>This class maintains real-time statistics about a scanning operation including
 * counts of processed, skipped, and errored files, along with timing information.
 * It's designed to be used by progress reporters to track and display scan progress.
 * 
 * <p>The status tracker automatically records the start time when created and provides
 * methods to increment various counters as files are processed. All timing calculations
 * are based on system milliseconds for consistency.
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. If used in
 * multi-threaded environments, external synchronization is required.
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create status tracker
 * ScanStatus status = new ScanStatus();
 * 
 * // Track progress during scanning
 * for (Path file : filesToProcess) {
 *     try {
 *         processFile(file);
 *         status.incrementFilesProcessed();
 *         System.out.println("Processed: " + status.getFilesProcessed());
 *     } catch (Exception e) {
 *         status.incrementErrors();
 *         System.err.println("Errors: " + status.getErrors());
 *     }
 * }
 * 
 * // Final report
 * System.out.println("Scan completed in " + status.getElapsedTime() + "ms");
 * System.out.println("Total files: " + status.getTotalFiles());
 * System.out.println("Success rate: " + 
 *     (100.0 * status.getFilesProcessed() / status.getTotalFiles()) + "%");
 * }</pre>
 * 
 * <h3>Integration with Progress Reporters:</h3>
 * <pre>{@code
 * public class MyProgressReporter implements ProgressReporter {
 *     private final ScanStatus scanStatus = new ScanStatus();
 *     
 *     @Override
 *     public Path reportProcessed(Path file, String message) {
 *         scanStatus.incrementFilesProcessed();
 *         updateDisplay();
 *         return file;
 *     }
 *     
 *     @Override
 *     public ScanStatus scanStatus() {
 *         return scanStatus;
 *     }
 *     
 *     private void updateDisplay() {
 *         System.out.printf("Progress: %d processed, %d errors (%.1fs elapsed)%n",
 *             scanStatus.getFilesProcessed(),
 *             scanStatus.getErrors(),
 *             scanStatus.getElapsedTime() / 1000.0);
 *     }
 * }
 * }</pre>
 * 
 * @see ProgressReporter
 * @see DirScanner
 */
public final class ScanStatus {
    
    private final long startTime = System.currentTimeMillis();
    private int filesProcessed = 0;
    private int filesSkipped = 0;
    private int errors = 0;
    
    /**
     * Increments the count of successfully processed files.
     * 
     * <p>Call this method when a file has been successfully processed by a handler.
     * Returns the new count after incrementing.
     * 
     * @return the new count of processed files after incrementing
     */
    public int incrementFilesProcessed() {
        return ++this.filesProcessed;
    }
    
    /**
     * Increments the count of skipped files.
     * 
     * <p>Call this method when a file matched the criteria but was skipped
     * for some reason (e.g., already exists, invalid format, permission denied).
     * Returns the new count after incrementing.
     * 
     * @return the new count of skipped files after incrementing
     */
    public int incrementFilesSkipped() {
        return ++this.filesSkipped;
    }
    
    /**
     * Increments the count of errors encountered.
     * 
     * <p>Call this method when an error occurs during file processing.
     * Returns the new count after incrementing.
     * 
     * @return the new count of errors after incrementing
     */
    public int incrementErrors() {
        return ++this.errors;
    }
    
    /**
     * Gets the current count of successfully processed files.
     * 
     * <p>This count represents files that were successfully handled without errors.
     * 
     * @return the number of files processed successfully
     */
    public int getFilesProcessed() {
        return this.filesProcessed;
    }
    
    /**
     * Gets the current count of skipped files.
     * 
     * <p>This count represents files that matched the criteria but were not
     * processed for various reasons (e.g., already exists, invalid format).
     * 
     * @return the number of files that were skipped
     */
    public int getFilesSkipped() {
        return this.filesSkipped;
    }
    
    /**
     * Gets the current count of errors encountered.
     * 
     * <p>This count represents files that caused exceptions or other errors
     * during processing attempts.
     * 
     * @return the number of errors that occurred
     */
    public int getErrors() {
        return this.errors;
    }
    
    /**
     * Gets the total count of all files encountered.
     * 
     * <p>This is the sum of processed, skipped, and errored files, representing
     * the total number of files that were examined during the scan.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * ScanStatus status = reporter.scanStatus();
     * double successRate = (double) status.getFilesProcessed() / status.getTotalFiles() * 100;
     * System.out.printf("Success rate: %.1f%% (%d/%d files)%n",
     *     successRate, status.getFilesProcessed(), status.getTotalFiles());
     * }</pre>
     * 
     * @return the total number of files processed, skipped, and errored
     */
    public int getTotalFiles() {
        return this.filesProcessed + this.filesSkipped + this.errors;
    }
    
    /**
     * Gets the timestamp when this status tracker was created.
     * 
     * <p>The start time is recorded automatically when the ScanStatus instance
     * is created and represents the beginning of the scanning operation.
     * 
     * @return the start time in milliseconds since epoch
     * @see System#currentTimeMillis()
     */
    public long getStartTime() {
        return this.startTime;
    }
    
    /**
     * Gets the elapsed time since the scan started.
     * 
     * <p>Calculates the time difference between now and when this status tracker
     * was created, providing a measure of how long the scanning operation has
     * been running.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * ScanStatus status = reporter.scanStatus();
     * System.out.printf("Scan running for %.2f seconds, processed %d files%n",
     *     status.getElapsedTime() / 1000.0, status.getFilesProcessed());
     * }</pre>
     * 
     * @return the elapsed time in milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - this.startTime;
    }
}
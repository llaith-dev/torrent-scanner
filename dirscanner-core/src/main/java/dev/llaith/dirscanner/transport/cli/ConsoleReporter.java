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

package dev.llaith.dirscanner.transport.cli;

import dev.llaith.dirscanner.core.ProgressReporter;
import dev.llaith.dirscanner.core.ScanReporter;
import dev.llaith.dirscanner.core.ScanRequest;
import dev.llaith.dirscanner.core.ScanStatus;
import dev.llaith.dirscanner.core.VerbosityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.nio.file.Path;

import static java.lang.String.format;

/**
 * CLI transport implementation for console reporting.
 * Handles formatted output to console streams based on verbosity levels.
 * Single responsibility: format and display scan results to console.
 */
public class ConsoleReporter implements ProgressReporter, ScanReporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsoleReporter.class);
    
    private final PrintStream out;
    private final PrintStream err;
    private final VerbosityLevel verbosity;
    private final boolean useColors;
    private final ScanStatus scanStatus = new ScanStatus();
    
    public ConsoleReporter(
            final boolean useColors,
            final VerbosityLevel verbosity) {
        
        this(System.out, System.err, useColors, verbosity);
    }
    
    public ConsoleReporter(
            final PrintStream out,
            final PrintStream err,
            final boolean useColors,
            final VerbosityLevel verbosity) {
        
        this.out = out;
        this.err = err;
        this.useColors = useColors;
        this.verbosity = verbosity;
    }
    
    @Override public void reportStart(final ScanRequest request) {
        
        if (this.verbosity == VerbosityLevel.QUIET) return;
        
        if (this.verbosity == VerbosityLevel.VERBOSE) {
            
            logger.info("Starting directory scan of directory: {}",
                        request.getDirectoryPath());
            
            if (request.isDryRun())
                logger.info("Running in dry-run mode - no files will be written");
            
            if (request.isIndexMode())
                logger.info("Index mode - will write consolidated file: {}",
                            request.getIndexFilePath());
            
            else
                logger.info("Individual mode - will write files to: {}",
                            request.getOutputDirectory() != null
                                    ? request.getOutputDirectory()
                                    : "source directories");
        }
    }
    
    @Override public void reportFailure(final Throwable throwable) {
        
        if (this.verbosity == VerbosityLevel.QUIET) return;
        
        printError(format("Failed to complete scan: %s ", throwable.getMessage()));
        
        throwable.printStackTrace(this.err);
    }
    
    @Override public int reportComplete() {
        
        if (this.verbosity != VerbosityLevel.QUIET) {
            
            final String summary = format(
                    "Scan completed in %sms: %s processed, %s skipped, %s errors",
                    this.scanStatus.getElapsedTime(),
                    this.scanStatus.getFilesProcessed(),
                    this.scanStatus.getFilesSkipped(),
                    this.scanStatus.getErrors());
            
            if (this.scanStatus.getErrors() > 0)
                printError(summary);
            else
                printSuccess(summary);
        }
        
        return this.scanStatus.getErrors() > 0 ? 1 : 0;
    }
    
    @Override public Path reportProcessed(final Path file, final String message) {
        
        this.scanStatus.incrementFilesProcessed();
        
        if (this.verbosity == VerbosityLevel.VERBOSE)
            this.out.printf("Processed %s: %s%n", file, message);
        
        return file;
    }
    
    @Override public Path reportSkipped(final Path file, final String message) {
        
        this.scanStatus.incrementFilesSkipped();
        
        if (this.verbosity == VerbosityLevel.VERBOSE)
            this.out.printf("Skipped %s: %s%n", file, message);
        
        return file;
    }
    
    @Override public Path reportErrored(final Path file, final String message) {
        
        this.scanStatus.incrementErrors();
        
        if (this.verbosity == VerbosityLevel.QUIET) return file;
        
        printError(format("Error with %s: %s ", file, message));
        
        return file;
    }
    
    @Override public Path reportErrored(final Path file, final String message, final Throwable throwable) {
        
        this.scanStatus.incrementErrors();
        
        if (this.verbosity == VerbosityLevel.QUIET) return file;
        
        printError(format("Error with %s: %s ", file, message));
        
        if (this.verbosity == VerbosityLevel.VERBOSE)
            throwable.printStackTrace(this.err);
        
        return file;
    }
    
    private void printSuccess(final String message) {
        
        if (this.useColors)
            this.out.println("\u001B[32m" + message + "\u001B[0m"); // Green
        else
            this.out.println(message);
    }
    
    private void printError(final String message) {
        
        if (this.useColors)
            this.err.println("\u001B[31m" + message + "\u001B[0m"); // Red
        else
            this.err.println(message);
    }
    
    @Override public ScanStatus scanStatus() {
        return this.scanStatus;
    }
}
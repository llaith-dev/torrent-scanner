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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Main entry point for the torrent-scanner application.
 * <p>
 * This application scans a directory for torrent files and exports their information to CSV files.
 * </p>
 */
@Command(
    name = "torrent-scanner",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Scans a directory for torrent files and exports their information to CSV files."
)
public class TorrentToCsvExporterCli implements Callable<Integer> {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TorrentToCsvExporterCli.class);

    @Parameters(index = "0", description = "The directory to scan for torrent files")
    protected String directoryPath;

    @Option(names = {"-v", "--verbose"}, description = "Show debug logs")
    protected boolean verbose;

    @Option(names = "--quiet", description = "Do not show info logs")
    protected boolean quiet;

    @Option(names = "--dry-run", description = "Do not write any files, just log what would happen")
    protected boolean dryRun;

    @Option(names = "--output-dir", description = "Directory to write CSV files to (instead of next to torrent files)")
    protected String outputDir;

    @Option(names = "--clobber", description = "Overwrite existing files instead of skipping them")
    protected boolean clobber;

    @Option(names = "--generate-index", description = "Generate an index file with all torrent information",
            paramLabel = "<path-to-index-file>")
    protected String indexFilePath;

    @Option(names = "--skip-scanning",
            description = "Skip scanning mode, only generate index if --generate-index is specified")
    protected boolean skipScanning;

    /**
     * Main method that starts the application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new TorrentToCsvExporterCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        configureLogging();

        logger.info("Starting torrent-scanner application");
        logger.debug("Command line options: verbose={}, quiet={}, dry-run={}, clobber={}, generate-index={}, skip-scanning={}",
                     this.verbose, this.quiet, this.dryRun, this.clobber, this.indexFilePath, this.skipScanning);

        // Check if skip-scanning is passed without generate-index
        if (this.skipScanning && (this.indexFilePath == null || this.indexFilePath.isEmpty())) {
            logger.info("Skip scanning mode enabled but no index file specified. Nothing to do.");
            System.out.println("Nothing to do");
            return 0;
        }

        try {
            int processedCount = 0;

            // Perform scanning if not skipped
            if (!this.skipScanning) {
                processedCount = scanDirectory();
                logger.info("Finished processing {} torrent files", processedCount);
            }

            // Generate index file if requested
            if (this.indexFilePath != null && !this.indexFilePath.isEmpty()) {
                generateIndex();
            }

            logger.info("Successfully processed {} torrent files", processedCount);
            return 0;
        } catch (final IOException e) {
            logger.error("Error processing directory: {}", e.getMessage(), e);
            logger.error("Error: {}", e.getMessage());
            return 1;
        }
    }

    /**
     * Generates an index file containing information from all processed torrent files.
     *
     * @throws IOException if an I/O error occurs
     */
    protected void generateIndex() throws IOException {
        logger.info("Generating index file: {}", this.indexFilePath);
        TorrentToCsvExporter.generateIndex(this.directoryPath, this.indexFilePath, this.dryRun, this.clobber);
    }

    /**
     * Scans the directory for torrent files.
     * This method is extracted to make the class more testable.
     *
     * @return the number of torrent files processed
     * @throws IOException if an I/O error occurs
     */
    protected int scanDirectory() throws IOException {
        return TorrentToCsvExporter.scanDirectory(this.directoryPath, this.outputDir, this.dryRun, this.clobber);
    }

    /**
     * Configures logging based on command line options.
     */
    private void configureLogging() {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        final Logger appLogger = loggerContext.getLogger("dev.llaith");

        // Set console appender level based on command line options
        if (this.verbose) {
            // In verbose mode, show debug logs
            rootLogger.getAppender("CONSOLE").setContext(loggerContext);
            rootLogger.setLevel(Level.DEBUG);
        } else if (this.quiet) {
            // In quiet mode, only show error logs
            rootLogger.getAppender("CONSOLE").setContext(loggerContext);
            rootLogger.setLevel(Level.ERROR);
        } else {
            // Default: show info logs and above
            rootLogger.getAppender("CONSOLE").setContext(loggerContext);
            rootLogger.setLevel(Level.INFO);
        }

        // Always keep debug level for file logging
        appLogger.setLevel(Level.DEBUG);
    }
}

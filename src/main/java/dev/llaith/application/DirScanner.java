package dev.llaith.application;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 *
 */
public class DirScanner {

    public enum VerbosityLevel {
        QUIET,    // No output except fatal errors, designed for scripting
        NORMAL,   // Summary only, start/stop, warnings, errors, surprise conditions
        VERBOSE,   // Detailed output, logged parameters, status changes, etc
        VERY_VERBOSE   // Detailed output at file level
    }

    public interface ProgressReporter {

        Path reportProcessed(Path file, String message);

        Path reportSkipped(Path file, String message);

        Path reportErrored(Path file, String message);

        Path reportErrored(Path file, String message, Throwable throwable);

        ScanStatus scanStatus();

    }

    public interface ScanReporter {

        void reportStart(ScanRequest request);

        void reportFailure(Throwable throwable);

        int reportComplete();

    }

    /**
     * Request object for torrent scanning operations.
     * Pure data transfer object with no behavior.
     */
    public static final class ScanRequest {

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

        public Path getDirectoryPath() {
            return this.directoryPath;
        }

        public Path getOutputDirectory() {
            return this.outputDirectory;
        }

        public Path getIndexFilePath() {
            return this.indexFilePath;
        }

        public boolean isDryRun() {
            return this.dryRun;
        }

        public boolean shouldOverwrite() {
            return this.overwrite;
        }

        public boolean isIndexMode() {
            return this.indexFilePath != null;
        }

        public VerbosityLevel getVerbosity() {
            return this.verbosity;
        }

        /**
         * Builder for creating scan requests.
         */
        public static final class Builder {
            private Path directoryPath = null;
            private Path outputDirectory = null;
            private Path indexFilePath = null;
            private boolean dryRun = false;
            private boolean overwrite = false;
            private VerbosityLevel verbosity = null;

            public Builder directoryPath(final Path directoryPath) {
                this.directoryPath = directoryPath;
                return this;
            }

            public Builder outputDirectory(final Path outputDirectory) {
                this.outputDirectory = outputDirectory;
                return this;
            }

            public Builder indexFilePath(final Path indexFilePath) {
                this.indexFilePath = indexFilePath;
                return this;
            }

            public Builder dryRun(final boolean dryRun) {
                this.dryRun = dryRun;
                return this;
            }

            public Builder overwrite(final boolean overwrite) {
                this.overwrite = overwrite;
                return this;
            }

            public Builder verbosity(final VerbosityLevel verbosity) {
                this.verbosity = verbosity;
                return this;
            }

            public ScanRequest build() {

                if (this.directoryPath == null)
                    throw new IllegalArgumentException("Directory path is required");

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

    /**
     * Result of a torrent scanning operation.
     * Pure data transfer object with scanning statistics.
     */
    public static final class ScanStatus {

        private final long startTime = System.currentTimeMillis();
        private int filesProcessed = 0;
        private int filesSkipped = 0;
        private int errors = 0;

        public int incrementFilesProcessed() {
            return ++this.filesProcessed;
        }

        public int incrementFilesSkipped() {
            return ++this.filesSkipped;
        }

        public int incrementErrors() {
            return ++this.errors;
        }

        public int getFilesProcessed() {
            return this.filesProcessed;
        }

        public int getFilesSkipped() {
            return this.filesSkipped;
        }

        public int getErrors() {
            return this.errors;
        }

        public int getTotalFiles() {
            return this.filesProcessed + this.filesSkipped + this.errors;
        }

        public long getStartTime() {
            return this.startTime;
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - this.startTime;
        }

    }

    public interface DirScannerHandler {

        String searchGlob();

        Predicate<Path> filter();

        void handleMatched(ScanRequest request, ProgressReporter progressReporter, Path file);

    }

    protected final DirScannerHandler handler;
    private final ProgressReporter reporter;

    public DirScanner(
            final DirScannerHandler handler,
            final ProgressReporter reporter) {

        this.handler = handler;
        this.reporter = reporter;

    }

    /**
     * Scans directory for torrent files and processes them according to the request.
     *
     * @param request the scan request
     * @throws IOException if file operations fail
     */
    public void scan(final ScanRequest request) throws IOException {

        final List<Path> matchedFiles = findMatchingFiles(
                request.getDirectoryPath(),
                this.handler.searchGlob(),
                this.handler.filter());

        for (final Path file : matchedFiles)
            this.handler.handleMatched(
                    request,
                    this.reporter,
                    file);

    }

    /**
     * Finds all .torrent files in the specified directory.
     *
     * @param directoryPath the directory to scan
     * @return list of torrent file paths
     * @throws IOException if directory scanning fails
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
                if (filter.test(entry))
                    paths.add(entry);
            }

        }

        return paths;

    }

}

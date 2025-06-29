package dev.llaith.torrentscanner.commands;

import dev.llaith.dirscanner.core.ScanRequest;
import dev.llaith.dirscanner.core.VerbosityLevel;
import dev.llaith.dirscanner.transport.cli.ConsoleReporter;
import dev.llaith.torrentscanner.TorrentScanner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "scan-torrents",
        description = "Scans directories for .torrent files and exports their metadata to CSV format",
        mixinStandardHelpOptions = true
)
public class ScanTorrentsCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Directory path to scan")
    private Path directoryPath;

    @Option(names = "--output-directory", description = "Directory to write output files")
    private Path outputDirectory;

    @Option(names = "--index-file", description = "Index file to write metadata")
    private Path indexFile;

    @Option(names = {"-d", "--dry-run"}, description = "Show what would be done without writing files")
    private boolean dryRun;

    @Option(names = {"-c", "--clobber"}, description = "Overwrite existing files")
    private boolean clobber;

    @Option(names = {"-q", "--quiet"}, description = "Quiet mode - minimal output")
    private boolean quiet;

    @Option(names = {"-v", "--verbose"}, description = "Increase verbosity")
    private boolean verbose;

    @Option(names = "--no-color", description = "Disable colored output")
    private boolean noColor;

    @Override
    public Integer call() {

        final VerbosityLevel verbosity = getVerbosityLevel();
        final ScanRequest request = buildScanRequest(verbosity);

        final ConsoleReporter reporter = createConsoleReporter(verbosity);

        reporter.reportStart(request);

        try {

            new TorrentScanner(reporter).scan(request);

        } catch (final IOException e) {

            reporter.reportFailure(e);

        }

        return reporter.reportComplete();

    }

    private ScanRequest buildScanRequest(final VerbosityLevel verbosity) {

        return new ScanRequest.Builder()
                .directoryPath(this.directoryPath)
                .dryRun(this.dryRun)
                .overwrite(this.clobber)
                .verbosity(verbosity)
                .outputDirectory(this.outputDirectory)
                .indexFilePath(this.indexFile)
                .build();

    }

    private VerbosityLevel getVerbosityLevel() {
        if (this.quiet) return VerbosityLevel.QUIET;
        return this.verbose ? VerbosityLevel.VERBOSE : VerbosityLevel.NORMAL;
    }

    private ConsoleReporter createConsoleReporter(final VerbosityLevel verbosity) {
        final boolean useColors = !this.noColor && System.console() != null;
        return new ConsoleReporter(useColors, verbosity);
    }

}

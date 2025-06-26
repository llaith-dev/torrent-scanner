package dev.llaith.application.cli;

import dev.llaith.application.DirScanner.ScanRequest;
import dev.llaith.application.DirScanner.VerbosityLevel;
import dev.llaith.domain.TorrentScanner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "scan-torrents",
        description = "Scans directories for .torrent files and exports their metadata to CSV format"
)
public class ScanTorrentsCommand implements Callable<Integer> {

    @Option(names = "--output-directory", description = "Directory to write output files")
    private Path outputDirectory;

    @Option(names = "--index-file", description = "Index file to write metadata")
    private Path indexFile;

    @ParentCommand
    private DirScannerCli parent;

    @Override
    public Integer call() {

        final ScanRequest request = buildScanRequest(this.parent.getVerbosityLevel());

        final ConsoleReporter reporter = this.parent.getScanReporter();

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
                .directoryPath(this.parent.directoryPath)
                .dryRun(this.parent.dryRun)
                .overwrite(this.parent.clobber)
                .verbosity(verbosity)
                .outputDirectory(this.outputDirectory)
                .indexFilePath(this.indexFile)
                .build();

    }

}

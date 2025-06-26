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

package dev.llaith.application.cli;

import dev.llaith.application.DirScanner.VerbosityLevel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 *
 */
@Command(
        name = "torrent-scanner",
        description = "Project DirScanner: a CLI tool for directory scanning with subcommands.",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        subcommands = { ScanTorrentsCommand.class }
)
public final class DirScannerCli implements Callable<Integer> {

    @Parameters(index = "0", description = "Directory path to scan")
    Path directoryPath;

    @Option(names = {"-d", "--dry-run"}, description = "Show what would be done without writing files")
    boolean dryRun;

    @Option(names = {"-c", "--clobber"}, description = "Overwrite existing files")
    boolean clobber;

    @Option(names = {"-q", "--quiet"}, description = "Quiet mode - minimal output")
    boolean quiet;

    @Option(names = {"-v", "--verbose"}, description = "Increase verbosity")
    boolean verbose;

    @Option(names = "--no-color", description = "Disable colored output")
    boolean noColor;

    @Override
    public Integer call() {

        // Print help if no subcommand is provided
        System.out.println("No subcommand specified. Available commands:");
        System.out.println("  scan-torrents   Scans directories for .torrent files and exports their metadata to CSV format");
        System.out.println();
        System.out.println("Use --help for more information.");

        return 0;

    }


    public VerbosityLevel getVerbosityLevel() {

        if (this.quiet) return VerbosityLevel.QUIET;

        return this.verbose ? VerbosityLevel.VERBOSE : VerbosityLevel.NORMAL;

    }

    public ConsoleReporter getScanReporter() {

        final boolean useColors = !this.noColor && System.console() != null;

        return new ConsoleReporter(useColors, this.getVerbosityLevel());

    }

}

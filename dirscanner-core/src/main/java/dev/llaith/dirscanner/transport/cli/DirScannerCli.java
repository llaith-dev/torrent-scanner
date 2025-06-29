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

import dev.llaith.dirscanner.core.VerbosityLevel;
import dev.llaith.dirscanner.plugin.DirScannerPlugin;
import dev.llaith.dirscanner.plugin.PluginManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Main CLI entry point for DirScanner framework.
 * Dynamically discovers and registers plugin commands.
 * Provides common CLI options and delegates to appropriate plugin commands.
 */
@Command(
        name = "dirscanner",
        description = "DirScanner: a plugin-based directory scanning framework with multiple transport layers.",
        mixinStandardHelpOptions = true,
        version = "1.0.0"
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
    
    @Option(names = "--config", description = "Configuration file path")
    Path configPath;
    
    private final PluginManager pluginManager;
    
    public DirScannerCli() {
        this.pluginManager = new PluginManager();
    }
    
    /**
     * Main entry point - discovers plugins and creates CLI with dynamic commands.
     */
    public static void main(final String[] args) {
        final DirScannerCli mainCommand = new DirScannerCli();
        final CommandLine commandLine = mainCommand.createCommandLine();
        final int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
    
    /**
     * Creates a CommandLine instance with dynamically registered plugin commands.
     */
    public CommandLine createCommandLine() {
        try {
            // Discover plugins
            this.pluginManager.discoverPlugins();
            
            // Create command line with this as main command
            final CommandLine commandLine = new CommandLine(this);
            
            // Register plugin commands
            final List<Class<? extends Callable<Integer>>> commandClasses = this.pluginManager.getCommandClasses();
            for (final Class<? extends Callable<Integer>> commandClass : commandClasses) {
                commandLine.addSubcommand(commandClass);
            }
            
            return commandLine;
            
        } catch (final Exception e) {
            System.err.println("Failed to initialize CLI: " + e.getMessage());
            e.printStackTrace();
            return new CommandLine(this);
        }
    }
    
    @Override
    public Integer call() {
        // Print help if no subcommand is provided
        System.out.println("No subcommand specified. Available commands:");
        
        final List<DirScannerPlugin> plugins = this.pluginManager.getDiscoveredPlugins();
        if (plugins.isEmpty()) {
            System.out.println("  No plugins discovered.");
        } else {
            for (final DirScannerPlugin plugin : plugins) {
                System.out.printf("  %-20s %s%n", 
                    plugin.getMetadata().getName(), 
                    plugin.getMetadata().getDescription());
            }
        }
        
        System.out.println();
        System.out.println("Use --help for more information.");
        
        return 0;
    }
    
    public VerbosityLevel getVerbosityLevel() {
        if (this.quiet) return VerbosityLevel.QUIET;
        return this.verbose ? VerbosityLevel.VERBOSE : VerbosityLevel.NORMAL;
    }
    
    public ConsoleReporter createConsoleReporter() {
        final boolean useColors = !this.noColor && System.console() != null;
        return new ConsoleReporter(useColors, this.getVerbosityLevel());
    }
    
    public PluginManager getPluginManager() {
        return this.pluginManager;
    }
}
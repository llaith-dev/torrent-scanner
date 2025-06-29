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

package dev.llaith.torrentscanner;

import dev.llaith.dirscanner.core.DirScannerHandler;
import dev.llaith.dirscanner.plugin.DirScannerPlugin;
import dev.llaith.dirscanner.plugin.DirScannerPluginInfo;
import dev.llaith.dirscanner.plugin.PluginContext;
import dev.llaith.dirscanner.plugin.PluginException;
import dev.llaith.dirscanner.plugin.PluginMetadata;
import dev.llaith.torrentscanner.commands.ScanTorrentsCommand;
import dev.llaith.torrentscanner.domain.TorrentHandler;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Main plugin implementation providing torrent file scanning and processing capabilities.
 * 
 * <p>This plugin extends the DirScanner framework to handle .torrent files, extracting
 * metadata such as file paths, sizes, and info hashes, and exporting the data to CSV format.
 * It demonstrates a complete plugin implementation with lifecycle management, command
 * registration, and handler creation.
 * 
 * <p>The plugin supports multiple operation modes:\n * <ul>\n *   <li><strong>Individual files:</strong> Creates separate CSV files for each torrent</li>\n *   <li><strong>Index mode:</strong> Consolidates all torrents into a single CSV file</li>\n *   <li><strong>Batch processing:</strong> Efficient memory usage for large torrent collections</li>\n * </ul>\n * \n * <h3>Plugin Features:</h3>\n * <ul>\n *   <li>Automatic discovery of .torrent files in directories</li>\n *   <li>BitTorrent metadata extraction using bt-core library</li>\n *   <li>RFC 4180 compliant CSV output with proper escaping</li>\n *   <li>Memory-efficient batch processing for large collections</li>\n *   <li>Comprehensive error handling and progress reporting</li>\n *   <li>Dry-run mode for testing and validation</li>\n * </ul>\n * \n * <h3>Usage Example (CLI):</h3>\n * <pre>\n * # Scan torrents in a directory\n * java -jar torrentscanner.jar scan-torrents /path/to/torrents\n * \n * # Create consolidated index\n * java -jar torrentscanner.jar scan-torrents /path/to/torrents \\\n *   --index-file /output/master-index.csv\n * \n * # Dry run with verbose output\n * java -jar torrentscanner.jar scan-torrents /path/to/torrents \\\n *   --dry-run --verbose\n * </pre>\n * \n * <h3>Configuration:</h3>\n * <p>The plugin can be configured via {@code torrentscanner.yaml}:</p>\n * <pre>{@code\n * # torrentscanner.yaml\n * batch-size: 100\n * output-format: csv\n * include-piece-info: false\n * }</pre>\n * \n * <h3>Plugin Architecture:</h3>\n * <p>This plugin demonstrates the complete DirScanner plugin pattern:\n * <ul>\n *   <li>{@link TorrentScannerPlugin} - Main plugin class with lifecycle management</li>\n *   <li>{@link dev.llaith.torrentscanner.commands.ScanTorrentsCommand} - CLI command implementation</li>\n *   <li>{@link dev.llaith.torrentscanner.domain.TorrentHandler} - File processing business logic</li>\n *   <li>{@link TorrentScanner} - Service orchestration layer</li>\n * </ul>\n * \n * @see dev.llaith.torrentscanner.commands.ScanTorrentsCommand\n * @see dev.llaith.torrentscanner.domain.TorrentHandler\n * @see TorrentScanner\n */
@DirScannerPluginInfo(
        name = "torrentscanner",
        description = "Scans directories for .torrent files and exports their metadata to CSV format",
        version = "1.0.0",
        author = "Nos Doughty"
)
public class TorrentScannerPlugin implements DirScannerPlugin {
    
    private Logger logger;
    private PluginContext context;
    
    /**
     * Gets plugin metadata from the {@link DirScannerPluginInfo} annotation.
     * 
     * <p>The metadata includes plugin name, description, version, and author
     * information as defined in the class-level annotation. This information
     * is used by the framework for plugin discovery and user interface display.
     * 
     * @return plugin metadata derived from annotation
     */
    @Override
    public PluginMetadata getMetadata() {
        final DirScannerPluginInfo annotation = this.getClass().getAnnotation(DirScannerPluginInfo.class);
        return PluginMetadata.fromAnnotation(annotation);
    }
    
    /**
     * Initializes the TorrentScanner plugin with framework context and services.
     * 
     * <p>This method sets up the plugin's logger and context for later use.
     * Currently, the plugin uses minimal initialization since torrent processing
     * doesn't require external dependencies or complex setup.
     * 
     * <p>Future enhancements could include:
     * <ul>
     *   <li>Loading plugin-specific configuration from torrentscanner.yaml</li>
     *   <li>Validating BitTorrent library dependencies</li>
     *   <li>Setting up performance monitoring or caching</li>
     * </ul>
     * 
     * @param context the plugin context providing framework services
     * @throws PluginException if initialization fails
     */
    @Override
    public void initialize(final PluginContext context) throws PluginException {
        this.context = context;
        this.logger = context.getLogger();
        
        logger.info("Initializing TorrentScanner plugin v{}", getMetadata().getVersion());
        
        try {
            // Plugin-specific initialization could go here
            // For example, loading configuration, validating dependencies, etc.
            
            logger.debug("TorrentScanner plugin initialization completed successfully");
            
        } catch (final Exception e) {
            throw new PluginException("Failed to initialize TorrentScanner plugin", e);
        }
    }
    
    /**
     * Shuts down the TorrentScanner plugin gracefully.
     * 
     * <p>Currently performs minimal cleanup since the plugin doesn't maintain
     * persistent resources. The method is designed to be extended if future
     * versions require resource cleanup.
     * 
     * <p>Potential future cleanup tasks:
     * <ul>
     *   <li>Closing database connections for torrent metadata caching</li>
     *   <li>Shutting down background processing threads</li>
     *   <li>Flushing any pending batch operations</li>
     *   <li>Releasing file system watchers or locks</li>
     * </ul>
     * 
     * @param timeout maximum time to wait for shutdown (currently unused)
     * @throws PluginException if shutdown fails
     */
    @Override
    public void shutdown(final Duration timeout) throws PluginException {
        if (this.logger != null) {
            this.logger.info("Shutting down TorrentScanner plugin");
        }
        
        try {
            // Plugin-specific cleanup could go here
            // For example, closing database connections, file handles, etc.
            
            if (this.logger != null) {
                this.logger.debug("TorrentScanner plugin shutdown completed successfully");
            }
            
        } catch (final Exception e) {
            throw new PluginException("Failed to shutdown TorrentScanner plugin", e);
        }
    }
    
    /**
     * Gets the CLI command classes provided by this plugin.
     * 
     * <p>This plugin contributes the {@code scan-torrents} command to the CLI
     * framework. The command provides all the functionality of the original
     * torrent scanner application with the same options and behavior.
     * 
     * <h3>Available Commands:</h3>
     * <ul>
     *   <li>{@link ScanTorrentsCommand} - Scans directories for .torrent files</li>
     * </ul>
     * 
     * <p>Future versions could add additional commands such as:
     * <ul>
     *   <li>validate-torrents - Verify torrent file integrity</li>
     *   <li>convert-torrents - Convert between torrent formats</li>
     *   <li>analyze-torrents - Generate statistics and reports</li>
     * </ul>
     * 
     * @return list containing the ScanTorrentsCommand class
     */
    @Override
    public List<Class<? extends Callable<Integer>>> getCommandClasses() {
        return List.of(ScanTorrentsCommand.class);
    }
    
    /**
     * Creates a new torrent file processing handler.
     * 
     * <p>The handler is responsible for:
     * <ul>
     *   <li>Defining the .torrent file glob pattern</li>
     *   <li>Filtering files to process only valid torrent files</li>
     *   <li>Parsing torrent metadata using the bt-core library</li>
     *   <li>Generating CSV output with proper escaping</li>
     *   <li>Managing batch processing for index mode</li>
     * </ul>
     * 
     * <p>Each handler instance is stateful and maintains batch data for
     * index mode operations. The framework may create multiple handlers
     * for concurrent processing or different scan operations.
     * 
     * @return a new TorrentHandler instance ready for file processing
     * @throws PluginException if handler creation fails
     * @see dev.llaith.torrentscanner.domain.TorrentHandler
     */
    @Override
    public DirScannerHandler createHandler() throws PluginException {
        try {
            return new TorrentHandler();
        } catch (final Exception e) {
            throw new PluginException("Failed to create TorrentHandler", e);
        }
    }
}
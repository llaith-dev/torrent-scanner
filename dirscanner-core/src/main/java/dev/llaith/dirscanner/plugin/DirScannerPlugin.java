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

package dev.llaith.dirscanner.plugin;

import dev.llaith.dirscanner.core.DirScannerHandler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Main interface for DirScanner framework plugins.
 * 
 * <p>Plugins are the primary extension mechanism for the DirScanner framework,
 * providing business logic for scanning and processing specific file types.
 * Each plugin contributes CLI commands and file processing handlers to the framework.
 * 
 * <p>The plugin system uses ServiceLoader for automatic discovery of plugin
 * implementations at runtime. Plugins can be packaged as part of the main
 * application or distributed as separate JAR files for dynamic loading.
 * 
 * <h3>Plugin Lifecycle:</h3>
 * <ol>
 *   <li><strong>Discovery:</strong> ServiceLoader finds plugin implementations</li>
 *   <li><strong>Metadata extraction:</strong> Plugin information is read from annotations</li>
 *   <li><strong>Initialization:</strong> Plugin is initialized with framework context</li>
 *   <li><strong>Command registration:</strong> Plugin's CLI commands are registered</li>
 *   <li><strong>Handler creation:</strong> Handlers are created on-demand for scanning</li>
 *   <li><strong>Shutdown:</strong> Plugin resources are cleaned up on framework shutdown</li>
 * </ol>
 * 
 * <h3>Plugin Implementation Example:</h3>
 * <pre>{@code
 * @DirScannerPluginInfo(
 *     name = \"imagescanner\",
 *     description = \"Processes image files and extracts metadata\",
 *     version = \"1.0.0\",
 *     author = \"Your Name\"
 * )
 * public class ImageScannerPlugin implements DirScannerPlugin {
 *     
 *     private ConfigurationService configService;
 *     private ImageConfig config;
 *     
 *     @Override
 *     public PluginMetadata getMetadata() {
 *         return PluginMetadata.fromAnnotation(this.getClass());
 *     }
 *     
 *     @Override\n *     public void initialize(PluginContext context) throws PluginException {\n *         this.configService = context.getConfigurationService();\n *         this.config = configService.loadConfig(\"imagescanner\", ImageConfig.class)\n *             .orElse(new ImageConfig());\n *         \n *         // Initialize any required resources\n *         initializeImageProcessors();\n *     }\n *     \n *     @Override\n *     public List<Class<? extends Callable<Integer>>> getCommandClasses() {\n *         return List.of(\n *             ScanImagesCommand.class,\n *             ExtractMetadataCommand.class\n *         );\n *     }\n *     \n *     @Override\n *     public DirScannerHandler createHandler() throws PluginException {\n *         return new ImageHandler(config);\n *     }\n *     \n *     @Override\n *     public void shutdown(Duration timeout) throws PluginException {\n *         // Cleanup resources\n *         cleanupImageProcessors();\n *     }\n * }\n * }</pre>\n * \n * <h3>ServiceLoader Registration:</h3>\n * <p>Create a file {@code META-INF/services/dev.llaith.dirscanner.plugin.DirScannerPlugin}\n * containing the fully qualified class name of your plugin:</p>\n * <pre>\n * com.example.ImageScannerPlugin\n * </pre>\n * \n * <h3>External Plugin Distribution:</h3>\n * <p>Plugins can be distributed as external JAR files and placed in the {@code ext/}\n * directory for automatic discovery at runtime (fat JAR mode only).</p>\n * \n * @see DirScannerPluginInfo\n * @see PluginContext\n * @see PluginMetadata\n * @see DirScannerHandler\n */
public interface DirScannerPlugin {
    
    /**
     * Gets metadata about this plugin.
     * 
     * <p>Plugin metadata provides essential information about the plugin including
     * its name, description, version, and other attributes. This information is used
     * for plugin discovery, configuration file naming, and user interface display.
     * 
     * <p>Metadata can be derived from {@link DirScannerPluginInfo} annotations
     * or created programmatically for dynamic plugins.
     * 
     * <h3>Example implementation:</h3>
     * <pre>{@code
     * @Override
     * public PluginMetadata getMetadata() {
     *     // From annotation (recommended)
     *     return PluginMetadata.fromAnnotation(this.getClass());
     *     
     *     // Or programmatically
     *     return new PluginMetadata(
     *         "myplugin",
     *         "My Custom Plugin",
     *         "1.0.0",
     *         "Author Name",
     *         true
     *     );
     * }
     * }</pre>
     * 
     * @return metadata about this plugin (must not be null)
     * @see DirScannerPluginInfo
     * @see PluginMetadata
     */
    PluginMetadata getMetadata();
    
    /**
     * Initializes the plugin with framework services and context.
     * 
     * <p>This method is called once during plugin discovery, after the plugin
     * instance is created but before any commands are registered or handlers
     * are requested. Use this method to:
     * <ul>
     *   <li>Load plugin configuration</li>
     *   <li>Initialize required resources (databases, connections, etc.)</li>
     *   <li>Validate plugin prerequisites</li>
     *   <li>Setup internal state</li>
     * </ul>
     * 
     * <p>The plugin context provides access to framework services including
     * configuration management. Plugins should store references to needed
     * services for later use.
     * 
     * <h3>Example implementation:</h3>
     * <pre>{@code
     * @Override
     * public void initialize(PluginContext context) throws PluginException {
     *     // Load plugin configuration
     *     ConfigurationService configService = context.getConfigurationService();
     *     this.config = configService.loadConfig("myplugin", MyConfig.class)
     *         .orElse(new MyConfig());
     *     
     *     // Validate prerequisites
     *     if (!checkSystemRequirements()) {
     *         throw new PluginException("System requirements not met");
     *     }
     *     
     *     // Initialize resources
     *     this.databaseConnection = createDatabaseConnection(config.getDatabaseUrl());
     *     this.processingThreadPool = Executors.newFixedThreadPool(config.getThreads());
     * }
     * }</pre>
     * 
     * @param context framework services and context available to the plugin
     * @throws PluginException if initialization fails or prerequisites are not met
     * @see PluginContext
     * @see ConfigurationService
     */
    void initialize(PluginContext context) throws PluginException;
    
    /**
     * Shuts down the plugin gracefully with a timeout.
     * 
     * <p>This method is called during framework shutdown to allow plugins to
     * clean up resources and complete any pending operations. Implementations
     * should respect the provided timeout and complete shutdown within the
     * specified duration.
     * 
     * <p>Common cleanup tasks include:
     * <ul>
     *   <li>Closing database connections</li>
     *   <li>Shutting down thread pools</li>
     *   <li>Flushing pending data to disk</li>
     *   <li>Closing file handles and network connections</li>
     *   <li>Releasing system resources</li>
     * </ul>
     * 
     * <h3>Example implementation:</h3>
     * <pre>{@code
     * @Override
     * public void shutdown(Duration timeout) throws PluginException {
     *     try {
     *         // Shutdown thread pool
     *         processingThreadPool.shutdown();
     *         if (!processingThreadPool.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
     *             processingThreadPool.shutdownNow();
     *         }
     *         
     *         // Close database connection
     *         if (databaseConnection != null) {
     *             databaseConnection.close();
     *         }
     *         
     *         // Flush any pending data
     *         flushPendingData();
     *         
     *     } catch (Exception e) {
     *         throw new PluginException("Failed to shutdown plugin", e);
     *     }
     * }
     * }</pre>
     * 
     * @param timeout maximum time to wait for shutdown completion
     * @throws PluginException if shutdown fails or times out
     */
    default void shutdown(final Duration timeout) throws PluginException {
        // Default implementation: no cleanup needed
    }
    
    /**
     * Gets the command classes that this plugin contributes to the CLI framework.
     * 
     * <p>Each command class must implement {@code Callable<Integer>} and be properly
     * annotated with PicoCLI annotations ({@code @Command}, {@code @Option}, {@code @Parameters}, etc.).
     * The framework will automatically register these commands and make them available
     * through the CLI interface.
     * 
     * <p>Commands should be self-contained and not depend on parent CLI context,
     * allowing them to work both as subcommands and independently.
     * 
     * <h3>Example command class:</h3>
     * <pre>{@code
     * @Command(
     *     name = "scan-images",
     *     description = "Scans directory for image files and extracts metadata"
     * )
     * public class ScanImagesCommand implements Callable<Integer> {
     *     
     *     @Parameters(index = "0", description = "Directory to scan")
     *     private Path directory;
     *     
     *     @Option(names = {"-o", "--output"}, description = "Output directory")
     *     private Path outputDirectory;
     *     
     *     @Option(names = {"-v", "--verbose"}, description = "Verbose output")
     *     private boolean verbose;
     *     
     *     @Override
     *     public Integer call() {
     *         // Command implementation
     *         return 0; // Success
     *     }
     * }
     * }</pre>
     * 
     * <h3>Example implementation:</h3>
     * <pre>{@code
     * @Override
     * public List<Class<? extends Callable<Integer>>> getCommandClasses() {
     *     return List.of(
     *         ScanImagesCommand.class,
     *         ExtractMetadataCommand.class,
     *         ConvertImagesCommand.class
     *     );
     * }
     * }</pre>
     * 
     * @return list of command classes for CLI registration (must not be null, can be empty)
     */
    List<Class<? extends Callable<Integer>>> getCommandClasses();
    
    /**
     * Creates a file processing handler for directory scanning operations.
     * 
     * <p>This method is called on-demand when a scan operation is requested that
     * uses this plugin. The handler defines how files are discovered (glob pattern
     * and filter) and processed during directory scanning.
     * 
     * <p>Handlers should be stateless or thread-safe if they will be used across
     * multiple scan operations. The framework may create multiple handler instances
     * or reuse them depending on usage patterns.
     * 
     * <p>The handler's configuration should typically be based on the plugin's
     * initialized configuration and context.
     * 
     * <h3>Example implementation:</h3>
     * <pre>{@code
     * @Override
     * public DirScannerHandler createHandler() throws PluginException {
     *     // Validate plugin is properly initialized
     *     if (config == null) {
     *         throw new PluginException("Plugin not initialized");
     *     }
     *     
     *     // Create handler with plugin configuration
     *     return new MyFileHandler(
     *         config.getGlobPattern(),
     *         config.getOutputFormat(),
     *         config.getBatchSize()
     *     );
     * }
     * }</pre>
     * 
     * @return a handler instance for processing files (must not be null)
     * @throws PluginException if handler creation fails or plugin is not properly initialized
     * @see DirScannerHandler
     */
    DirScannerHandler createHandler() throws PluginException;
}
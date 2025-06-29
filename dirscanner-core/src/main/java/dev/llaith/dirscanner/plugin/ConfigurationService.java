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

import java.nio.file.Path;
import java.util.Optional;

/**
 * Service for loading and managing plugin configuration files.
 * 
 * <p>The configuration service provides a standardized way for plugins to load
 * YAML-based configuration files with automatic object mapping using Jackson.
 * It follows a convention-based approach for locating configuration files and
 * provides fallback mechanisms for flexible deployment scenarios.
 * 
 * <p>The service supports multiple configuration file locations with the following
 * precedence order:\n * <ol>\n *   <li>Command line --config parameter (if provided)</li>\n *   <li>{@code <pluginName>.yaml} in current directory</li>\n *   <li>{@code ext/<pluginName>.yaml} in external plugins directory</li>\n * </ol>\n * \n * <p>Configuration files are in YAML format and automatically mapped to Java\n * objects using Jackson. The framework never fails on missing configuration -\n * plugins decide whether to use defaults or fail if configuration is required.\n * \n * <h3>Usage Example:</h3>\n * <pre>{@code\n * // Define configuration class\n * public class MyPluginConfig {\n *     private String databaseUrl = \"jdbc:h2:mem:default\";\n *     private int batchSize = 100;\n *     private boolean enableFeatureX = false;\n *     \n *     // getters and setters\n * }\n * \n * // Load configuration in plugin\n * @Override\n * public void initialize(PluginContext context) throws PluginException {\n *     ConfigurationService configService = context.getConfigurationService();\n *     \n *     Optional<MyPluginConfig> config = configService.loadConfig(\n *         \"myplugin\", MyPluginConfig.class);\n *     \n *     if (config.isPresent()) {\n *         this.config = config.get();\n *         logger.info(\"Loaded configuration: batch-size={}, feature-x={}\",\n *             config.getBatchSize(), config.isEnableFeatureX());\n *     } else {\n *         this.config = new MyPluginConfig();  // Use defaults\n *         logger.info(\"No configuration found, using defaults\");\n *     }\n * }\n * }</pre>\n * \n * <h3>Configuration File Example:</h3>\n * <pre>{@code\n * # myplugin.yaml\n * database-url: \"jdbc:postgresql://localhost/mydb\"\n * batch-size: 500\n * enable-feature-x: true\n * advanced-settings:\n *   timeout: 30s\n *   retry-count: 3\n * }</pre>\n * \n * <h3>Error Handling:</h3>\n * <p>The service is designed to be non-failing - if a configuration file cannot\n * be loaded or parsed, an empty Optional is returned rather than throwing an\n * exception. This allows plugins to gracefully handle missing or invalid\n * configurations by using defaults.</p>\n * \n * @see PluginContext#getConfigurationService()\n * @see DirScannerPlugin#initialize(PluginContext)\n */
public interface ConfigurationService {
    
    /**
     * Loads configuration for a plugin using the default naming convention.
     * 
     * <p>This method searches for configuration files in the standard locations
     * using the plugin name to construct the filename. The search order ensures
     * that more specific configurations override general ones.
     * 
     * <p>Search order:
     * <ol>
     *   <li>Command line {@code --config} parameter (if provided)</li>
     *   <li>{@code <pluginName>.yaml} in current directory</li>
     *   <li>{@code ext/<pluginName>.yaml} in external plugins directory</li>
     * </ol>
     * 
     * <p>The configuration file is parsed as YAML and automatically mapped to
     * the specified Java class using Jackson's ObjectMapper. Field names are
     * mapped using kebab-case to camelCase conversion (e.g., "batch-size" → batchSize).
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * // Load plugin configuration
     * Optional<TorrentConfig> config = configService.loadConfig(
     *     "torrentscanner", TorrentConfig.class);
     * 
     * if (config.isPresent()) {
     *     TorrentConfig cfg = config.get();
     *     logger.info("Batch size: {}", cfg.getBatchSize());
     * } else {
     *     logger.warn("No configuration found, using defaults");
     * }
     * }</pre>
     * 
     * @param <T> the type of configuration class
     * @param pluginName the name of the plugin (used for filename construction)
     * @param configClass the configuration class to deserialize to
     * @return configuration instance if found and valid, empty Optional otherwise
     * @throws IllegalArgumentException if pluginName or configClass is null
     */
    <T> Optional<T> loadConfig(String pluginName, Class<T> configClass);
    
    /**
     * Loads configuration from a specific file path.
     * 
     * <p>This method loads configuration from the exact path specified, bypassing
     * the standard naming convention and search locations. Use this when you need
     * to load configuration from a custom location or when implementing configuration
     * inheritance/composition patterns.
     * 
     * <p>The file is parsed as YAML and mapped to the specified class using the
     * same Jackson ObjectMapper configuration as the standard loading method.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * // Load from custom path
     * Path customConfig = Paths.get("/etc/myapp/custom-config.yaml");
     * Optional<MyConfig> config = configService.loadConfig(customConfig, MyConfig.class);
     * 
     * // Load base configuration then overlay custom settings
     * Optional<BaseConfig> base = configService.loadConfig("myplugin", BaseConfig.class);
     * Optional<CustomConfig> custom = configService.loadConfig(
     *     Paths.get("custom-overrides.yaml"), CustomConfig.class);
     * }</pre>
     * 
     * @param <T> the type of configuration class
     * @param configPath the path to the configuration file (must not be null)
     * @param configClass the configuration class to deserialize to (must not be null)
     * @return configuration instance if file exists and is valid, empty Optional otherwise
     * @throws IllegalArgumentException if configPath or configClass is null
     */
    <T> Optional<T> loadConfig(Path configPath, Class<T> configClass);
    
    /**
     * Checks if a configuration file exists for the given plugin.
     * 
     * <p>This method checks the standard configuration file locations to determine
     * if any configuration file exists for the specified plugin. It follows the
     * same search order as {@link #loadConfig(String, Class)} but only checks
     * for file existence without attempting to parse the content.
     * 
     * <p>This is useful for conditional logic where plugins want to behave
     * differently based on whether configuration is available.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * if (configService.hasConfig("myplugin")) {
     *     // Configuration exists, load it
     *     Optional<MyConfig> config = configService.loadConfig("myplugin", MyConfig.class);
     *     if (config.isPresent()) {
     *         // Use loaded configuration
     *     } else {
     *         // File exists but couldn't be parsed
     *         throw new PluginException("Invalid configuration file");
     *     }
     * } else {
     *     // No configuration file, plugin is optional or uses all defaults
     *     logger.info("No configuration found, plugin will use defaults");
     * }
     * }</pre>
     * 
     * @param pluginName the name of the plugin (must not be null)
     * @return true if a configuration file exists in any of the standard locations
     * @throws IllegalArgumentException if pluginName is null
     */
    boolean hasConfig(String pluginName);
    
    /**
     * Gets the expected path to the configuration file for a plugin.
     * 
     * <p>This method returns the path where the plugin's configuration file
     * would be located according to the standard naming convention. It does
     * not guarantee that the file actually exists at that location.
     * 
     * <p>The returned path follows the pattern: {@code <pluginName>.yaml}
     * in the current working directory. This is useful for generating
     * configuration templates, error messages, or documentation.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * Path expectedPath = configService.getConfigPath("myplugin");
     * if (!Files.exists(expectedPath)) {
     *     logger.info("To configure this plugin, create: {}", expectedPath);
     *     // Optionally create a template file
     *     writeConfigurationTemplate(expectedPath);
     * }
     * 
     * // Error reporting
     * if (!configService.hasConfig("myplugin")) {
     *     throw new PluginException(
     *         "Required configuration missing. Please create: " +
     *         configService.getConfigPath("myplugin"));
     * }
     * }</pre>
     * 
     * @param pluginName the name of the plugin (must not be null)
     * @return path where the configuration file should be located (never null)
     * @throws IllegalArgumentException if pluginName is null
     */
    Path getConfigPath(String pluginName);
}
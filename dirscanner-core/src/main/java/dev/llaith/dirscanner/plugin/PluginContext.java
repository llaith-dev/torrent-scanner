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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context object providing framework services and utilities to plugins.
 * 
 * <p>The plugin context is passed to plugins during initialization and provides
 * access to framework services that plugins can use throughout their lifecycle.
 * This includes configuration management, logging, and other framework utilities.
 * 
 * <p>The context ensures plugins have access to framework services in a controlled
 * and consistent manner, while maintaining proper separation between plugin code
 * and framework internals.
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Override
 * public void initialize(PluginContext context) throws PluginException {
 *     // Get plugin-specific logger
 *     Logger logger = context.getLogger();
 *     logger.info(\"Initializing plugin\");
 *     
 *     // Load plugin configuration
 *     ConfigurationService configService = context.getConfigurationService();
 *     Optional<MyConfig> config = configService.loadConfig(\"myplugin\", MyConfig.class);
 *     
 *     if (config.isPresent()) {\n *         logger.info(\"Loaded configuration: {}\", config.get());\n *         this.pluginConfig = config.get();\n *     } else {\n *         logger.warn(\"No configuration found, using defaults\");\n *         this.pluginConfig = new MyConfig();\n *     }\n *     \n *     // Initialize plugin resources\n *     initializeResources();\n *     logger.info(\"Plugin initialization completed\");\n * }\n * }</pre>\n * \n * <h3>Service Lifecycle:</h3>\n * <p>The context and its services are valid for the entire plugin lifecycle,\n * from initialization until shutdown. Plugins should not cache context references\n * beyond their own lifecycle boundaries.</p>\n * \n * @see DirScannerPlugin#initialize(PluginContext)\n * @see ConfigurationService\n */
public final class PluginContext {
    
    private final ConfigurationService configurationService;
    private final Logger logger;
    
    /**
     * Creates a new plugin context with the specified services.
     * 
     * <p>This constructor is typically called by the framework during plugin
     * initialization. The plugin name is used to create a scoped logger for
     * the plugin.
     * 
     * @param configurationService the configuration service for loading plugin configs
     * @param pluginName the name of the plugin (used for logger naming)
     * @throws IllegalArgumentException if configurationService or pluginName is null
     */
    public PluginContext(final ConfigurationService configurationService, final String pluginName) {
        if (configurationService == null) {
            throw new IllegalArgumentException("Configuration service must not be null");
        }
        if (pluginName == null) {
            throw new IllegalArgumentException("Plugin name must not be null");
        }
        
        this.configurationService = configurationService;
        this.logger = LoggerFactory.getLogger("plugin." + pluginName);
    }
    
    /**
     * Gets the configuration service for loading plugin configurations.
     * 
     * <p>The configuration service provides access to YAML-based configuration
     * files for plugins. Configurations are loaded by plugin name and automatically
     * mapped to Java objects using Jackson.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * ConfigurationService configService = context.getConfigurationService();
     * 
     * // Load plugin configuration
     * Optional<MyConfig> config = configService.loadConfig("myplugin", MyConfig.class);
     * 
     * if (config.isPresent()) {
     *     MyConfig cfg = config.get();
     *     // Use configuration
     * } else {
     *     // No configuration file found, use defaults
     * }
     * }</pre>
     * 
     * @return the configuration service for loading plugin configs (never null)
     * @see ConfigurationService#loadConfig(String, Class)
     */
    public ConfigurationService getConfigurationService() {
        return this.configurationService;
    }
    
    /**
     * Gets a logger specifically scoped to this plugin.
     * 
     * <p>The logger name follows the pattern "plugin.{pluginName}", allowing
     * for plugin-specific logging configuration and filtering. This enables
     * better debugging and monitoring of individual plugins.
     * 
     * <p>The logger uses SLF4J, so it's compatible with any logging backend
     * (Logback, Log4j, etc.) configured in the application.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * Logger logger = context.getLogger();
     * 
     * logger.info("Plugin starting up");
     * logger.debug("Processing file: {}", filename);
     * logger.warn("Configuration value missing, using default: {}", defaultValue);
     * 
     * try {
     *     // Plugin operation
     * } catch (Exception e) {
     *     logger.error("Plugin operation failed", e);
     * }
     * }</pre>
     * 
     * <h3>Logger Configuration:</h3>
     * <p>Plugin loggers can be configured independently in logback.xml:</p>
     * <pre>{@code
     * <logger name="plugin.torrentscanner" level="DEBUG"/>
     * <logger name="plugin" level="INFO"/>  <!-- All plugins -->
     * }</pre>
     * 
     * @return a plugin-scoped SLF4J logger (never null)
     * @see Logger
     */
    public Logger getLogger() {
        return this.logger;
    }
}
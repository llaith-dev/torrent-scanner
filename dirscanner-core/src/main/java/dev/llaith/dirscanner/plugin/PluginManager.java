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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Manages plugin discovery, loading, and lifecycle.
 * Supports both ServiceLoader discovery and external JAR loading.
 */
public class PluginManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);
    
    private final Map<String, DirScannerPlugin> plugins = new LinkedHashMap<>();
    private final Map<DirScannerPlugin, PluginContext> contexts = new HashMap<>();
    private final ConfigurationServiceImpl configurationService;
    
    public PluginManager() {
        this.configurationService = new ConfigurationServiceImpl();
    }
    
    /**
     * Discovers plugins using ServiceLoader and external JAR scanning.
     * 
     * @throws PluginException if plugin discovery or initialization fails
     */
    public void discoverPlugins() throws PluginException {
        logger.info("Starting plugin discovery...");
        
        // Discover from classpath via ServiceLoader
        discoverFromServiceLoader();
        
        // Discover from external JARs
        discoverFromExternalJars();
        
        // Initialize discovered plugins
        initializePlugins();
        
        logger.info("Plugin discovery completed. Found {} plugins: {}", 
                   this.plugins.size(),
                   this.plugins.keySet());
    }
    
    /**
     * Discovers plugins using Java ServiceLoader mechanism.
     */
    private void discoverFromServiceLoader() {
        final ServiceLoader<DirScannerPlugin> serviceLoader = ServiceLoader.load(DirScannerPlugin.class);
        
        for (final DirScannerPlugin plugin : serviceLoader) {
            registerPlugin(plugin);
        }
    }
    
    /**
     * Discovers plugins from external JAR files in the ext/ directory.
     */
    private void discoverFromExternalJars() {
        final Path extDir = Paths.get("ext");
        
        if (!Files.exists(extDir) || !Files.isDirectory(extDir)) {
            logger.debug("No ext/ directory found, skipping external JAR discovery");
            return;
        }
        
        try (final DirectoryStream<Path> jarFiles = Files.newDirectoryStream(extDir, "*.jar")) {
            for (final Path jarFile : jarFiles) {
                loadPluginFromJar(jarFile);
            }
        } catch (final IOException e) {
            logger.warn("Failed to scan ext/ directory for plugin JARs: {}", e.getMessage());
        }
    }
    
    /**
     * Loads plugins from a specific JAR file.
     */
    private void loadPluginFromJar(final Path jarFile) {
        try {
            logger.debug("Loading plugins from JAR: {}", jarFile);
            
            final URL jarUrl = jarFile.toUri().toURL();
            final URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
            
            final ServiceLoader<DirScannerPlugin> jarServiceLoader = ServiceLoader.load(DirScannerPlugin.class, classLoader);
            
            for (final DirScannerPlugin plugin : jarServiceLoader) {
                registerPlugin(plugin);
            }
            
        } catch (final Exception e) {
            logger.warn("Failed to load plugins from JAR {}: {}", jarFile, e.getMessage());
        }
    }
    
    /**
     * Registers a discovered plugin.
     */
    private void registerPlugin(final DirScannerPlugin plugin) {
        final PluginMetadata metadata = plugin.getMetadata();
        
        if (!metadata.isEnabled()) {
            logger.debug("Plugin {} is disabled, skipping", metadata.getName());
            return;
        }
        
        if (this.plugins.containsKey(metadata.getName())) {
            logger.warn("Plugin {} already registered, skipping duplicate", metadata.getName());
            return;
        }
        
        this.plugins.put(metadata.getName(), plugin);
        logger.debug("Registered plugin: {}", metadata);
    }
    
    /**
     * Initializes all registered plugins.
     */
    private void initializePlugins() throws PluginException {
        for (final Map.Entry<String, DirScannerPlugin> entry : this.plugins.entrySet()) {
            final String pluginName = entry.getKey();
            final DirScannerPlugin plugin = entry.getValue();
            
            try {
                final PluginContext context = new PluginContext(this.configurationService, pluginName);
                plugin.initialize(context);
                this.contexts.put(plugin, context);
                
                logger.debug("Initialized plugin: {}", pluginName);
                
            } catch (final Exception e) {
                logger.error("Failed to initialize plugin {}: {}", pluginName, e.getMessage());
                throw new PluginException("Failed to initialize plugin " + pluginName, e);
            }
        }
    }
    
    /**
     * Gets all discovered plugins.
     */
    public List<DirScannerPlugin> getDiscoveredPlugins() {
        return new ArrayList<>(this.plugins.values());
    }
    
    /**
     * Gets a plugin by name.
     */
    public Optional<DirScannerPlugin> getPlugin(final String name) {
        return Optional.ofNullable(this.plugins.get(name));
    }
    
    /**
     * Gets all command classes from all plugins.
     */
    public List<Class<? extends Callable<Integer>>> getCommandClasses() {
        return this.plugins.values().stream()
                .flatMap(plugin -> plugin.getCommandClasses().stream())
                .collect(Collectors.toList());
    }
    
    /**
     * Shuts down all plugins gracefully.
     */
    public void shutdown() {
        shutdown(DEFAULT_SHUTDOWN_TIMEOUT);
    }
    
    /**
     * Shuts down all plugins with a specified timeout.
     */
    public void shutdown(final Duration timeout) {
        logger.info("Shutting down {} plugins...", this.plugins.size());
        
        for (final Map.Entry<String, DirScannerPlugin> entry : this.plugins.entrySet()) {
            final String pluginName = entry.getKey();
            final DirScannerPlugin plugin = entry.getValue();
            
            shutdownPlugin(pluginName, plugin, timeout);
        }
        
        this.plugins.clear();
        this.contexts.clear();
        
        logger.info("Plugin shutdown completed");
    }
    
    /**
     * Shuts down a single plugin with timeout handling.
     */
    private void shutdownPlugin(final String pluginName, final DirScannerPlugin plugin, final Duration timeout) {
        final CompletableFuture<Void> shutdownFuture = CompletableFuture.runAsync(() -> {
            try {
                plugin.shutdown(timeout);
                logger.debug("Shutdown completed for plugin: {}", pluginName);
            } catch (final Exception e) {
                logger.warn("Plugin {} shutdown failed: {}", pluginName, e.getMessage());
            }
        });
        
        try {
            shutdownFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            logger.warn("Plugin {} shutdown timed out, forcing termination", pluginName);
            shutdownFuture.cancel(true);
        } catch (final Exception e) {
            logger.warn("Plugin {} shutdown failed: {}", pluginName, e.getMessage());
        }
    }
}
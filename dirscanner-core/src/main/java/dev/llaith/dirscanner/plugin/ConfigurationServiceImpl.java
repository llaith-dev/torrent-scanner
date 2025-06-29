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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Implementation of ConfigurationService using YAML files.
 * Supports the standard plugin configuration naming convention.
 */
public class ConfigurationServiceImpl implements ConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationServiceImpl.class);
    
    private final ObjectMapper yamlMapper;
    private Path overrideConfigPath = null;
    
    public ConfigurationServiceImpl() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * Sets a global configuration path override (from --config parameter).
     */
    public void setConfigPathOverride(final Path configPath) {
        this.overrideConfigPath = configPath;
    }
    
    @Override
    public <T> Optional<T> loadConfig(final String pluginName, final Class<T> configClass) {
        
        // Try paths in order of preference
        final Path[] candidatePaths = {
            this.overrideConfigPath,                    // 1. --config override
            Paths.get(pluginName + ".yaml"),          // 2. Current dir
            Paths.get("ext", pluginName + ".yaml")    // 3. ext/ dir
        };
        
        for (final Path candidatePath : candidatePaths) {
            if (candidatePath != null && Files.exists(candidatePath)) {
                final Optional<T> config = loadConfig(candidatePath, configClass);
                if (config.isPresent()) {
                    logger.debug("Loaded configuration for plugin {} from: {}", pluginName, candidatePath);
                    return config;
                }
            }
        }
        
        logger.debug("No configuration found for plugin: {}", pluginName);
        return Optional.empty();
    }
    
    @Override
    public <T> Optional<T> loadConfig(final Path configPath, final Class<T> configClass) {
        
        if (!Files.exists(configPath)) {
            logger.debug("Configuration file does not exist: {}", configPath);
            return Optional.empty();
        }
        
        if (!Files.isRegularFile(configPath)) {
            logger.warn("Configuration path is not a regular file: {}", configPath);
            return Optional.empty();
        }
        
        try {
            final T config = this.yamlMapper.readValue(configPath.toFile(), configClass);
            logger.debug("Successfully loaded configuration from: {}", configPath);
            return Optional.of(config);
            
        } catch (final Exception e) {
            logger.warn("Failed to load configuration from {}: {}", configPath, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public boolean hasConfig(final String pluginName) {
        
        final Path[] candidatePaths = {
            this.overrideConfigPath,
            Paths.get(pluginName + ".yaml"),
            Paths.get("ext", pluginName + ".yaml")
        };
        
        for (final Path candidatePath : candidatePaths) {
            if (candidatePath != null && Files.exists(candidatePath) && Files.isRegularFile(candidatePath)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public Path getConfigPath(final String pluginName) {
        
        // Return override path if set
        if (this.overrideConfigPath != null) {
            return this.overrideConfigPath;
        }
        
        // Try standard locations
        final Path currentDirPath = Paths.get(pluginName + ".yaml");
        if (Files.exists(currentDirPath)) {
            return currentDirPath;
        }
        
        final Path extDirPath = Paths.get("ext", pluginName + ".yaml");
        if (Files.exists(extDirPath)) {
            return extDirPath;
        }
        
        // Return default location even if it doesn't exist
        return currentDirPath;
    }
}
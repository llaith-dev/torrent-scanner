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

/**
 * Immutable metadata container for DirScanner plugins.
 * 
 * <p>This class holds essential information about a plugin including its name,
 * description, version, author, and enabled status. Plugin metadata is used
 * throughout the framework for discovery, configuration, and user interface display.
 * 
 * <p>Metadata can be created in several ways:
 * <ul>
 *   <li>From {@link DirScannerPluginInfo} annotations (recommended)</li>
 *   <li>Programmatically using static factory methods</li>
 *   <li>Directly via constructor for maximum control</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // From annotation (most common)
 * @DirScannerPluginInfo(name = \"myplugin\", description = \"My plugin\")
 * public class MyPlugin implements DirScannerPlugin {
 *     @Override
 *     public PluginMetadata getMetadata() {
 *         return PluginMetadata.fromAnnotation(this.getClass());
 *     }
 * }
 * 
 * // Programmatically with defaults
 * PluginMetadata metadata = PluginMetadata.of(\"myplugin\", \"My plugin\");
 * 
 * // Full control
 * PluginMetadata metadata = new PluginMetadata(
 *     \"myplugin\",
 *     \"My advanced plugin\",
 *     \"2.1.0\",
 *     \"John Doe\",
 *     true
 * );
 * 
 * // Display information
 * System.out.println(metadata.toString()); // \"myplugin v2.1.0 - My advanced plugin\"
 * }</pre>
 * 
 * @see DirScannerPluginInfo
 * @see DirScannerPlugin#getMetadata()
 */
public final class PluginMetadata {
    
    private final String name;
    private final String description;
    private final String version;
    private final String author;
    private final boolean enabled;
    
    public PluginMetadata(
            final String name,
            final String description,
            final String version,
            final String author,
            final boolean enabled) {
        
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
        this.enabled = enabled;
    }
    
    /**
     * Creates metadata from a {@link DirScannerPluginInfo} annotation instance.
     * 
     * <p>This method extracts all metadata fields from the provided annotation
     * and creates a new PluginMetadata instance with those values.
     * 
     * @param annotation the plugin info annotation (must not be null)
     * @return metadata instance created from annotation values
     * @throws IllegalArgumentException if annotation is null
     * @see #fromAnnotation(Class)
     */
    public static PluginMetadata fromAnnotation(final DirScannerPluginInfo annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Annotation must not be null");
        }
        return new PluginMetadata(
                annotation.name(),
                annotation.description(),
                annotation.version(),
                annotation.author(),
                annotation.enabled());
    }
    
    /**
     * Creates metadata from a {@link DirScannerPluginInfo} annotation on a class.
     * 
     * <p>This is the most common way to create plugin metadata. It looks for
     * a {@code @DirScannerPluginInfo} annotation on the provided class and
     * extracts metadata from it.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * @DirScannerPluginInfo(
     *     name = "myplugin",
     *     description = "My plugin description"
     * )
     * public class MyPlugin implements DirScannerPlugin {
     *     @Override
     *     public PluginMetadata getMetadata() {
     *         return PluginMetadata.fromAnnotation(this.getClass());
     *     }
     * }
     * }</pre>
     * 
     * @param pluginClass the plugin class to extract annotation from (must not be null)
     * @return metadata instance created from class annotation
     * @throws IllegalArgumentException if pluginClass is null or has no annotation
     */
    public static PluginMetadata fromAnnotation(final Class<?> pluginClass) {
        if (pluginClass == null) {
            throw new IllegalArgumentException("Plugin class must not be null");
        }
        
        DirScannerPluginInfo annotation = pluginClass.getAnnotation(DirScannerPluginInfo.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                "Class " + pluginClass.getName() + " is not annotated with @DirScannerPluginInfo");
        }
        
        return fromAnnotation(annotation);
    }
    
    /**
     * Creates metadata programmatically with minimal required information.
     * 
     * <p>This factory method creates metadata with default values for version ("1.0.0"),
     * author (empty string), and enabled status (true). Use this when you need to
     * create metadata without annotations.
     * 
     * <h3>Example usage:</h3>
     * <pre>{@code
     * public class DynamicPlugin implements DirScannerPlugin {
     *     @Override
     *     public PluginMetadata getMetadata() {
     *         return PluginMetadata.of("dynamic", "Dynamically created plugin");
     *     }
     * }
     * }</pre>
     * 
     * @param name the unique plugin name (must not be null or empty)
     * @param description the plugin description (must not be null or empty)
     * @return metadata instance with default values for other fields
     * @throws IllegalArgumentException if name or description is null or empty
     */
    public static PluginMetadata of(final String name, final String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin name must not be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin description must not be null or empty");
        }
        return new PluginMetadata(name, description, "1.0.0", "", true);
    }
    
    /**
     * Gets the unique plugin name.
     * 
     * <p>The plugin name is used for configuration file naming, command registration,
     * and plugin identification throughout the framework.
     * 
     * @return the plugin name (never null)
     * @see DirScannerPluginInfo#name()
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Gets the human-readable plugin description.
     * 
     * <p>The description explains what the plugin does and is displayed in
     * plugin listings and help text.
     * 
     * @return the plugin description (never null)
     * @see DirScannerPluginInfo#description()
     */
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Gets the plugin version string.
     * 
     * <p>The version is used for display, compatibility checking, and update management.
     * It typically follows semantic versioning format.
     * 
     * @return the plugin version (never null)
     * @see DirScannerPluginInfo#version()
     */
    public String getVersion() {
        return this.version;
    }
    
    /**
     * Gets the plugin author information.
     * 
     * <p>Author information is used for attribution and support purposes.
     * May be empty if not specified.
     * 
     * @return the plugin author (never null, but may be empty)
     * @see DirScannerPluginInfo#author()
     */
    public String getAuthor() {
        return this.author;
    }
    
    /**
     * Checks if the plugin is enabled by default.
     * 
     * <p>This flag indicates whether the plugin should be automatically loaded
     * when discovered. Disabled plugins can still be manually enabled.
     * 
     * @return true if the plugin is enabled by default, false otherwise
     * @see DirScannerPluginInfo#enabled()
     */
    public boolean isEnabled() {
        return this.enabled;
    }
    
    /**
     * Returns a formatted string representation of the plugin metadata.
     * 
     * <p>The format is: "name vversion - description"
     * 
     * <h3>Example output:</h3>
     * <pre>
     * torrentscanner v1.5.0 - Scans .torrent files and exports metadata to CSV
     * </pre>
     * 
     * @return formatted plugin information string
     */
    @Override
    public String toString() {
        return String.format("%s v%s - %s", this.name, this.version, this.description);
    }
}
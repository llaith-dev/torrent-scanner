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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for providing metadata about DirScanner plugin implementations.
 * 
 * <p>This annotation should be placed on plugin classes that implement
 * {@link DirScannerPlugin} to provide essential metadata about the plugin.
 * The framework uses this information for plugin discovery, configuration
 * file naming, command registration, and user interface display.
 * 
 * <p>All attributes have sensible defaults except for {@code name} and
 * {@code description}, which are required to provide meaningful plugin
 * identification.
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code\n * @DirScannerPluginInfo(\n *     name = \"torrentscanner\",\n *     description = \"Scans .torrent files and exports metadata to CSV\",\n *     version = \"1.5.0\",\n *     author = \"Nos Doughty\",\n *     enabled = true\n * )\n * public class TorrentScannerPlugin implements DirScannerPlugin {\n *     // Plugin implementation\n * }\n * }</pre>\n * \n * <h3>Configuration File Naming:</h3>\n * <p>The plugin {@code name} is used to determine configuration file names.\n * For example, a plugin named \"torrentscanner\" will look for configuration\n * in {@code torrentscanner.yaml}.</p>\n * \n * <h3>Command Registration:</h3>\n * <p>The plugin {@code name} is used as a prefix or namespace for plugin\n * commands to avoid naming conflicts between plugins.</p>\n * \n * @see DirScannerPlugin\n * @see PluginMetadata#fromAnnotation(Class)\n */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DirScannerPluginInfo {
    
    /**
     * The unique name of the plugin.
     * 
     * <p>This name serves multiple purposes in the framework:
     * <ul>
     *   <li><strong>Configuration:</strong> Used to locate plugin configuration files (e.g., "torrentscanner.yaml")</li>
     *   <li><strong>Identification:</strong> Used in plugin listing and discovery output</li>
     *   <li><strong>Logging:</strong> Used as a logger name prefix for plugin-specific logging</li>
     *   <li><strong>Command namespacing:</strong> May be used to namespace plugin commands</li>
     * </ul>
     * 
     * <p>The name should be:
     * <ul>
     *   <li>Lowercase and filesystem-safe (alphanumeric and hyphens)</li>
     *   <li>Unique across all plugins in the system</li>
     *   <li>Descriptive of the plugin's purpose</li>
     *   <li>Stable across plugin versions</li>
     * </ul>
     * 
     * <p>Examples: "torrentscanner", "imagemetadata", "pdf-processor"
     * 
     * @return the unique plugin name (required, must not be empty)
     */
    String name();
    
    /**
     * Human-readable description of the plugin's functionality.
     * 
     * <p>This description is displayed in plugin listings, help text, and user
     * interfaces to help users understand what the plugin does. It should be
     * concise but informative.
     * 
     * <p>Good descriptions:
     * <ul>
     *   <li>Clearly state what file types are processed</li>
     *   <li>Mention the primary output or action</li>
     *   <li>Use clear, non-technical language when possible</li>
     * </ul>
     * 
     * <p>Examples:
     * <ul>
     *   <li>"Scans .torrent files and exports metadata to CSV format"</li>
     *   <li>"Processes image files and extracts EXIF metadata"</li>
     *   <li>"Analyzes PDF documents for text content and structure"</li>
     * </ul>
     * 
     * @return the plugin description (required, must not be empty)
     */
    String description();
    
    /**
     * Plugin version string.
     * 
     * <p>The version should follow semantic versioning (SemVer) format when possible
     * (e.g., "1.0.0", "2.1.3", "1.0.0-beta1"). This version is used for:
     * <ul>
     *   <li>Plugin listing and information display</li>
     *   <li>Compatibility checking (future feature)</li>
     *   <li>Update management (future feature)</li>
     *   <li>Debugging and support</li>
     * </ul>
     * 
     * <p>The version should be updated whenever the plugin is modified to help
     * track plugin changes and compatibility.
     * 
     * @return the plugin version string (defaults to "1.0.0")
     */
    String version() default "1.0.0";
    
    /**
     * Plugin author information.
     * 
     * <p>This field can contain author name, organization, or contact information.
     * It's displayed in plugin listings and used for attribution and support purposes.
     * 
     * <p>Examples:
     * <ul>
     *   <li>"John Doe"</li>
     *   <li>"Acme Corporation"</li>
     *   <li>"Jane Smith <jane@example.com>"</li>
     * </ul>
     * 
     * @return the author information (optional, empty string by default)
     */
    String author() default "";
    
    /**
     * Whether this plugin is enabled by default.
     * 
     * <p>This flag controls whether the plugin should be automatically loaded
     * and initialized when discovered by the framework. Disabled plugins are
     * still discovered but not loaded, allowing users to selectively enable
     * them if needed.
     * 
     * <p>Use cases for disabled-by-default plugins:
     * <ul>
     *   <li>Experimental or beta plugins</li>
     *   <li>Plugins requiring additional setup or configuration</li>
     *   <li>Optional plugins that may conflict with others</li>
     *   <li>Resource-intensive plugins that should be opt-in</li>
     * </ul>
     * 
     * <p>Note: This is a hint to the framework and may be overridden by
     * user configuration or command-line options.
     * 
     * @return true if the plugin should be enabled by default, false otherwise
     */
    boolean enabled() default true;
}
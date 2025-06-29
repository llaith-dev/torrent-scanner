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

import java.io.Serial;

/**
 * Exception thrown when plugin operations fail.
 * 
 * <p>This exception is used throughout the plugin framework to indicate
 * various types of plugin-related failures including initialization errors,
 * execution problems, configuration issues, and cleanup failures.
 * 
 * <p>Plugin implementations should throw this exception (or a subclass)
 * when encountering errors that prevent proper plugin operation. The
 * framework will handle these exceptions appropriately based on the
 * operation context.
 * 
 * <h3>Common Usage Scenarios:</h3>
 * <ul>
 *   <li><strong>Initialization failures:</strong> Missing dependencies, invalid configuration</li>\n *   <li><strong>Handler creation errors:</strong> Resource allocation failures</li>\n *   <li><strong>Configuration problems:</strong> Invalid or missing required settings</li>\n *   <li><strong>Shutdown issues:</strong> Resource cleanup failures, timeout violations</li>\n * </ul>\n * \n * <h3>Usage Examples:</h3>\n * <pre>{@code\n * // Initialization failure\n * @Override\n * public void initialize(PluginContext context) throws PluginException {\n *     if (!checkSystemRequirements()) {\n *         throw new PluginException(\"Required system libraries not found\");\n *     }\n * }\n * \n * // Configuration error\n * @Override\n * public DirScannerHandler createHandler() throws PluginException {\n *     if (config.getDatabaseUrl() == null) {\n *         throw new PluginException(\"Database URL is required but not configured\");\n *     }\n *     \n *     try {\n *         return new MyHandler(config);\n *     } catch (SQLException e) {\n *         throw new PluginException(\"Failed to connect to database\", e);\n *     }\n * }\n * \n * // Shutdown timeout\n * @Override\n * public void shutdown(Duration timeout) throws PluginException {\n *     try {\n *         if (!threadPool.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {\n *             throw new PluginException(\"Plugin shutdown timed out after \" + timeout);\n *         }\n *     } catch (InterruptedException e) {\n *         Thread.currentThread().interrupt();\n *         throw new PluginException(\"Plugin shutdown interrupted\", e);\n *     }\n * }\n * }</pre>\n * \n * @see DirScannerPlugin\n * @see PluginContext\n */
public class PluginException extends Exception {
    
    @Serial private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new plugin exception with the specified error message.
     * 
     * <p>Use this constructor when you have a clear error message but no
     * underlying cause exception.
     * 
     * @param message the error message describing what went wrong (can be null)
     */
    public PluginException(final String message) {
        super(message);
    }
    
    /**
     * Creates a new plugin exception with an error message and underlying cause.
     * 
     * <p>Use this constructor when wrapping another exception with additional
     * context about what the plugin was trying to do when the error occurred.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * try {
     *     database.connect(config.getUrl());
     * } catch (SQLException e) {
     *     throw new PluginException("Failed to connect to database during initialization", e);
     * }
     * }</pre>
     * 
     * @param message the error message describing what went wrong (can be null)
     * @param cause the underlying exception that caused this error (can be null)
     */
    public PluginException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new plugin exception wrapping an underlying cause.
     * 
     * <p>Use this constructor when the underlying exception's message is
     * sufficient and you don't need to add additional context.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * try {
     *     Files.readAllLines(configFile);
     * } catch (IOException e) {
     *     throw new PluginException(e);  // IOException message is descriptive enough
     * }
     * }</pre>
     * 
     * @param cause the underlying exception that caused this error (can be null)
     */
    public PluginException(final Throwable cause) {
        super(cause);
    }
}
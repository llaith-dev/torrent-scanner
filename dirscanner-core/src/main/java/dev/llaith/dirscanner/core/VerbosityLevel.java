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

package dev.llaith.dirscanner.core;

/**
 * Enumeration of verbosity levels for controlling output and logging detail.
 * 
 * <p>This enum defines the different levels of detail that should be included
 * in progress reporting, logging, and console output across all transport layers.
 * The levels are arranged in ascending order of verbosity, allowing for easy
 * comparison and filtering.
 * 
 * <p>Transport layers and progress reporters should respect these levels when
 * determining what information to display or log. Higher verbosity levels
 * include all output from lower levels.
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class MyProgressReporter implements ProgressReporter {
 *     private final VerbosityLevel verbosity;
 *     
 *     public MyProgressReporter(VerbosityLevel verbosity) {
 *         this.verbosity = verbosity;
 *     }
 *     
 *     @Override
 *     public Path reportProcessed(Path file, String message) {
 *         // Always show progress at NORMAL and above
 *         if (verbosity.ordinal() >= VerbosityLevel.NORMAL.ordinal()) {\n *             System.out.println(\"Processed: \" + file.getFileName());\n *         }\n *         \n *         // Show detailed info at VERBOSE and above\n *         if (verbosity.ordinal() >= VerbosityLevel.VERBOSE.ordinal()) {\n *             System.out.println(\"  Details: \" + message);\n *         }\n *         \n *         // Show file-level details at VERY_VERBOSE\n *         if (verbosity == VerbosityLevel.VERY_VERBOSE) {\n *             System.out.println(\"  Full path: \" + file.toAbsolutePath());\n *             System.out.println(\"  Size: \" + Files.size(file) + \" bytes\");\n *         }\n *         \n *         return file;\n *     }\n * }\n * }</pre>\n * \n * <h3>Integration with CLI Options:</h3>\n * <pre>{@code\n * @Command(name = \"scan\")\n * public class ScanCommand implements Callable<Integer> {\n *     @Option(names = {\"-q\", \"--quiet\"}, description = \"Minimal output\")\n *     private boolean quiet;\n *     \n *     @Option(names = {\"-v\", \"--verbose\"}, description = \"Detailed output\")\n *     private boolean verbose;\n *     \n *     @Option(names = {\"--very-verbose\"}, description = \"Very detailed output\")\n *     private boolean veryVerbose;\n *     \n *     private VerbosityLevel getVerbosityLevel() {\n *         if (veryVerbose) return VerbosityLevel.VERY_VERBOSE;\n *         if (verbose) return VerbosityLevel.VERBOSE;\n *         if (quiet) return VerbosityLevel.QUIET;\n *         return VerbosityLevel.NORMAL;\n *     }\n * }\n * }</pre>\n * \n * @see ScanRequest#getVerbosity()\n * @see ProgressReporter\n */
public enum VerbosityLevel {
    
    /** 
     * Minimal output mode - no output except fatal errors.
     * 
     * <p>Designed for scripting and automated environments where minimal
     * output is desired. Only critical errors that prevent operation
     * should be displayed.
     * 
     * <p>Appropriate for:
     * <ul>
     *   <li>Batch processing scripts</li>
     *   <li>Cron jobs and scheduled tasks</li>
     *   <li>API integrations where output is not monitored</li>
     * </ul>
     */
    QUIET,
    
    /**
     * Standard output mode - summary information only.
     * 
     * <p>Shows start/stop messages, warnings, errors, and unexpected conditions.
     * This is the default verbosity level providing a good balance between
     * useful information and output volume.
     * 
     * <p>Typical output includes:
     * <ul>
     *   <li>Operation start and completion messages</li>
     *   <li>Summary statistics (files processed, errors)</li>
     *   <li>Warnings and error messages</li>
     *   <li>Unexpected conditions or surprises</li>
     * </ul>
     */
    NORMAL,
    
    /**
     * Detailed output mode - includes configuration and progress details.
     * 
     * <p>Shows all NORMAL output plus detailed information about operation
     * parameters, configuration, and progress updates. Useful for debugging
     * and understanding what the application is doing.
     * 
     * <p>Additional output includes:
     * <ul>
     *   <li>Configuration parameters and settings</li>
     *   <li>Progress updates during processing</li>
     *   <li>Status changes and state transitions</li>
     *   <li>Performance timing information</li>
     * </ul>
     */
    VERBOSE,
    
    /**
     * Maximum detail mode - includes file-level information.
     * 
     * <p>Shows all VERBOSE output plus detailed information about individual
     * files being processed. This can generate significant output volume
     * and is primarily useful for detailed debugging and analysis.
     * 
     * <p>Additional output includes:
     * <ul>
     *   <li>Individual file paths and details</li>
     *   <li>File sizes, timestamps, and attributes</li>
     *   <li>Detailed processing steps for each file</li>
     *   <li>Internal state and data structure information</li>
     * </ul>
     */
    VERY_VERBOSE
}
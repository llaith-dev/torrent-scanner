package dev.llaith.console;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles console output with color support using JLine/Jansi.
 * <p>
 * This class provides methods for outputting messages to the console with different
 * colors based on the message type (normal, warning, error). It also respects
 * verbose and quiet flags to control the verbosity of the output.
 * </p>
 */
public class ConsoleOutput {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleOutput.class);

    private static final Terminal terminal;

    static {
        try {
            // Initialize terminal with Jansi support
            terminal = TerminalBuilder.builder()
                    .jansi(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize terminal", e);
        }
    }

    private final boolean verbose;
    private final boolean quiet;

    /**
     * Creates a new ConsoleOutput instance.
     *
     * @param verbose if true, debug messages will be shown
     * @param quiet if true, only warning and error messages will be shown
     */
    public ConsoleOutput(boolean verbose, boolean quiet) {
        this.verbose = verbose;
        this.quiet = quiet;
    }

    /**
     * Outputs a debug message to the console.
     * Only shown if verbose mode is enabled.
     *
     * @param message the message to output
     */
    public void debug(String message) {
        logger.debug(message);
        if (verbose) {
            println(message, OutputType.DEBUG);
        }
    }

    /**
     * Outputs a debug message to the console with SLF4J-style formatting.
     * Only shown if verbose mode is enabled.
     *
     * @param format the message format with {} placeholders
     * @param args the arguments to replace the placeholders
     */
    public void debug(String format, Object... args) {
        String message = formatMessage(format, args);
        logger.debug(format, args);
        if (verbose) {
            println(message, OutputType.DEBUG);
        }
    }

    /**
     * Outputs an info message to the console.
     * Not shown if quiet mode is enabled.
     *
     * @param message the message to output
     */
    public void info(String message) {
        logger.info(message);
        if (!quiet) {
            println(message, OutputType.INFO);
        }
    }

    /**
     * Outputs an info message to the console with SLF4J-style formatting.
     * Not shown if quiet mode is enabled.
     *
     * @param format the message format with {} placeholders
     * @param args the arguments to replace the placeholders
     */
    public void info(String format, Object... args) {
        String message = formatMessage(format, args);
        logger.info(format, args);
        if (!quiet) {
            println(message, OutputType.INFO);
        }
    }

    /**
     * Outputs a warning message to the console.
     * Always shown, regardless of verbose or quiet mode.
     *
     * @param message the message to output
     */
    public void warn(String message) {
        logger.warn(message);
        println(message, OutputType.WARNING);
    }

    /**
     * Outputs a warning message to the console with SLF4J-style formatting.
     * Always shown, regardless of verbose or quiet mode.
     *
     * @param format the message format with {} placeholders
     * @param args the arguments to replace the placeholders
     */
    public void warn(String format, Object... args) {
        String message = formatMessage(format, args);
        logger.warn(format, args);
        println(message, OutputType.WARNING);
    }

    /**
     * Outputs an error message to the console.
     * Always shown, regardless of verbose or quiet mode.
     *
     * @param message the message to output
     */
    public void error(String message) {
        logger.error(message);
        println(message, OutputType.ERROR);
    }

    /**
     * Outputs an error message to the console with SLF4J-style formatting.
     * Always shown, regardless of verbose or quiet mode.
     *
     * @param format the message format with {} placeholders
     * @param args the arguments to replace the placeholders
     */
    public void error(String format, Object... args) {
        String message = formatMessage(format, args);
        logger.error(format, args);
        println(message, OutputType.ERROR);
    }

    /**
     * Outputs an error message to the console with an exception.
     * Always shown, regardless of verbose or quiet mode.
     *
     * @param message the message to output
     * @param throwable the exception to log
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
        println(message, OutputType.ERROR);
    }

    /**
     * Formats a message with SLF4J-style {} placeholders.
     *
     * @param format the message format with {} placeholders
     * @param args the arguments to replace the placeholders
     * @return the formatted message
     */
    private String formatMessage(String format, Object... args) {
        String result = format;
        for (Object arg : args) {
            int index = result.indexOf("{}");
            if (index >= 0) {
                result = result.substring(0, index) + arg + result.substring(index + 2);
            }
        }
        return result;
    }

    /**
     * Outputs a formatted message to the console.
     * Similar to String.format() but respects the output type.
     *
     * @param outputType the type of output
     * @param format the format string
     * @param args the arguments to format
     */
    public void printf(OutputType outputType, String format, Object... args) {
        String message = String.format(format, args);

        switch (outputType) {
            case DEBUG -> debug(message);
            case INFO -> info(message);
            case WARNING -> warn(message);
            case ERROR -> error(message);
        }
    }

    /**
     * Prints a message to the console with the specified output type.
     *
     * @param message the message to print
     * @param outputType the type of output
     */
    private void println(String message, OutputType outputType) {
        AttributedStyle style = switch (outputType) {
            case DEBUG -> AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
            case INFO -> AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
            case WARNING -> AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
            case ERROR -> AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
        };

        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.styled(style, message);
        
        System.out.println(asb.toAnsi(terminal));
    }

    /**
     * Enum representing the different types of output.
     */
    public enum OutputType {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
}
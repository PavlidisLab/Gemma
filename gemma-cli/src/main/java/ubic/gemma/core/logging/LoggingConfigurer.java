package ubic.gemma.core.logging;

import java.io.Closeable;

/**
 * Simple interface for configuring logging levels.
 */
public interface LoggingConfigurer {

    /**
     * Set the logging level of all loggers.
     * @throws IllegalArgumentException if newLevel is invalid
     */
    void configureAllLoggers( int newLevel );

    /**
     * Set the logging level of a specific logger.
     * @throws IllegalArgumentException if loggerName or newLevel is invalid
     */
    void configureLogger( String loggerName, int newLevel ) throws IllegalArgumentException;

    /**
     * Apply pending changes to the configuration from previous {@link #configureAllLoggers(int)} and
     * {@link #configureLogger(String, int)} invocations.
     */
    void apply();
}

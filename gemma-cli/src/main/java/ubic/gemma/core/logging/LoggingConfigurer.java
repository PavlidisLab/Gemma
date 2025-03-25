package ubic.gemma.core.logging;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Simple interface for configuring logging levels.
 */
public interface LoggingConfigurer {

    String[] NAMED_LEVELS = { "off", "fatal", "error", "warn", "info", "debug", "trace" };

    String[] NUMBERED_LEVELS = { "0", "1", "2", "3", "4", "5", "6" };

    /**
     * Obtain the list of all logger names.
     * @return
     */
    List<String> getAllLoggerNames();

    /**
     * Set the logging level of all loggers.
     * @throws IllegalArgumentException if newLevel is invalid
     */
    void configureAllLoggers( int newLevel );

    /**
     * Set the logging level of all loggers using a named level.
     * @throws IllegalArgumentException if newLevel is invalid
     */
    default void configureAllLoggers( String newLevel ) {
        int l = ArrayUtils.indexOf( NAMED_LEVELS, newLevel );
        Assert.isTrue( l != -1, "Level must be one of: " + String.join( ", ", NAMED_LEVELS ) + "." );
        configureAllLoggers( l );
    }

    /**
     * Set the logging level of a specific logger.
     * @throws IllegalArgumentException if loggerName or newLevel is invalid
     */
    void configureLogger( String loggerName, int newLevel ) throws IllegalArgumentException;

    /**
     * Set the logging level of a specific logger using a named level.
     * @throws IllegalArgumentException if loggerName or newLevel is invalid
     */
    default void configureLogger( String loggerName, String newLevel ) throws IllegalArgumentException {
        int l = ArrayUtils.indexOf( NAMED_LEVELS, newLevel );
        Assert.isTrue( l != -1 );
        configureLogger( loggerName, l );
    }

    /**
     * Apply pending changes to the configuration.
     */
    void apply();
}

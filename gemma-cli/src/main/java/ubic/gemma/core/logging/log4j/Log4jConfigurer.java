package ubic.gemma.core.logging.log4j;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import ubic.gemma.core.logging.LoggingConfigurer;

import java.util.Map;

/**
 * Implementation of {@link LoggingConfigurer} for Log4j.
 */
@CommonsLog
public class Log4jConfigurer implements LoggingConfigurer {

    private final LoggerContext loggerContext;

    public Log4jConfigurer() {
        loggerContext = ( LoggerContext ) LogManager.getContext( false );
    }

    @Override
    public void configureAllLoggers( int newLevel ) {
        Configuration config = loggerContext.getConfiguration();
        Level newLog4jLevel = toLog4jLevel( newLevel );
        // configure all individual loggers
        for ( Map.Entry<String, LoggerConfig> e : config.getLoggers().entrySet() ) {
            configureLogger( e.getKey(), e.getValue(), newLog4jLevel );
        }
    }

    /**
     * Set up logging according to the user-selected (or default) verbosity level.
     */
    @Override
    public void configureLogger( String loggerName, int newLevel ) {
        Configuration config = loggerContext.getConfiguration();
        configureLogger( loggerName, config.getLoggerConfig( loggerName ), toLog4jLevel( newLevel ) );
    }

    private void configureLogger( String loggerName, LoggerConfig loggerConfig, Level newLevel ) {
        if ( loggerName.equals( loggerConfig.getName() ) ) {
            log.debug( String.format( "Setting logging level of '%s' to %s.", loggerConfig.getName(), newLevel ) );
        } else {
            // effective logger differs, this means that there no configuration
            log.warn( String.format( "Setting logging level of '%s' to %s since there's no logger named '%s'. To prevent this, add an entry for '%s' in log4j.properties.",
                    loggerConfig.getName(), newLevel, loggerName, loggerName ) );
        }
        loggerConfig.setLevel( newLevel );
    }


    @Override
    public void apply() {
        loggerContext.updateLoggers();
    }

    private static Level toLog4jLevel( int level ) {
        switch ( level ) {
            case 0:
                return Level.OFF;
            case 1:
                return Level.FATAL;
            case 2:
                return Level.ERROR;
            case 3:
                return Level.WARN;
            case 4:
                return Level.INFO;
            case 5:
                return Level.DEBUG;
            default:
                throw new IllegalArgumentException( "the level must be between 0 and 5" );
        }
    }
}

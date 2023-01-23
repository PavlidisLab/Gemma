package ubic.gemma.core.util.test;

import org.assertj.core.api.AbstractAssert;
import ubic.gemma.core.logging.LoggingConfigurer;
import ubic.gemma.core.logging.log4j.Log4jConfigurer;
import ubic.gemma.core.util.CLI;

import javax.annotation.CheckReturnValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ utilities for {@link CLI}.
 * @author poirigui
 */
public class CliAssert<SELF extends CliAssert<SELF>> extends AbstractAssert<SELF, CLI> {

    private static final LoggingConfigurer loggingConfigurer = new Log4jConfigurer();
    private String[] commandArgs = {};

    protected CliAssert( CLI cli ) {
        super( cli, CliAssert.class );
    }

    @CheckReturnValue
    public CliAssert<SELF> withArguments( String... args ) {
        this.commandArgs = args;
        return this.myself;

    }

    /**
     * Configure verbosity for all loggers.
     * @param verbosity the target verbosity
     */
    @CheckReturnValue
    public CliAssert<SELF> withVerbosity( int verbosity ) {
        loggingConfigurer.configureAllLoggers( verbosity );
        loggingConfigurer.apply();
        return this.myself;
    }

    /**
     * Configure a single logger verbosity.
     * @param loggerName the name of the logger
     * @param verbosity the target verbosity
     */
    @CheckReturnValue
    public CliAssert<SELF> withLoggerVerbosity( String loggerName, int verbosity ) {
        loggingConfigurer.configureLogger( loggerName, verbosity );
        loggingConfigurer.apply();
        return this.myself;
    }

    public CliAssert<SELF> succeeds() {
        assertThat( actual.executeCommand( commandArgs ) )
                .isEqualTo( CLI.SUCCESS );
        return this.myself;
    }

    public CliAssert<SELF> fails() {
        assertThat( actual.executeCommand( commandArgs ) )
                .isNotEqualTo( CLI.SUCCESS );
        return this.myself;
    }
}

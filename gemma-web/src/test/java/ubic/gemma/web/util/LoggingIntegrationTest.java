package ubic.gemma.web.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class LoggingIntegrationTest {

    private static final LoggerContext context = ( LoggerContext ) LogManager.getContext();

    @BeforeClass
    public static void setUp() {
        assumeTrue( "The -Dgemma.log.dir must be set.",
                System.getProperty( "gemma.log.dir" ) != null );
        assumeTrue( "The -Dlog4j2.configurationFile must be set.",
                System.getProperty( "log4j2.configurationFile" ) != null );
    }

    @Test
    public void testRootLogger() {
        assertThat( context.getConfiguration().getLoggerConfig( "" ).getAppenders() )
                .containsKeys( "file", "warningFile", "errorFile", "slack" );
    }

    @Test
    public void testThatJerseyServerRuntimeLoggerDoesNotUseTheSlackAppender() {
        LoggerConfig config = context.getConfiguration()
                .getLoggerConfig( "org.glassfish.jersey.server.ServerRuntime$Responder" );
        assertThat( config.getName() )
                .isEqualTo( "org.glassfish.jersey.server.ServerRuntime$Responder" );
        assertThat( config.getAppenders() )
                .containsOnlyKeys( "file", "errorFile", "warningFile" );
        assertThat( config.isAdditive() ).isFalse();
    }
}

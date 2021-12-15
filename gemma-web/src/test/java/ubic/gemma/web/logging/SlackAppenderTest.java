package ubic.gemma.web.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

public class SlackAppenderTest {

    private static Log log = LogFactory.getLog( SlackAppenderTest.class );

    @Test
    public void test() {
        String slackToken = System.getProperty( "gemma.slack.token" );
        String slackChannel = System.getProperty( "gemma.slack.channel" );
        assumeTrue( "Both -Dgemma.slack.token and -Dgemma.slack.channel must be set for this test.",
                slackToken != null && slackChannel != null );
        try {
            raiseStackTrace();
            fail( "This should not be reached." );
        } catch ( IllegalArgumentException e ) {
            log.error( "This is just a test for the Log4j Slack appender.", e );
        }
    }

    private void raiseStackTrace() {
        throw new IllegalArgumentException( "This is the cause of the error which should be part of an attachment." );
    }

}
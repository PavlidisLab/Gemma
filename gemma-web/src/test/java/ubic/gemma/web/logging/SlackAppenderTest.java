package ubic.gemma.web.logging;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

public class SlackAppenderTest {

    private static final Log log = LogFactory.getLog( SlackAppenderTest.class );

    private Slack mockedSlack;

    @Before
    public void setUp() {
        String slackToken = System.getProperty( "gemma.slack.token" );
        String slackChannel = System.getProperty( "gemma.slack.channel" );
        mockedSlack = mock( Slack.class );
        MethodsClient mockedMethodClient = mock( MethodsClient.class );
        when( mockedSlack.methods( any( String.class ) ) ).thenReturn( mockedMethodClient );
        assumeTrue( "This test must be run with -Dlog4j1.compatibility=true.",
                Objects.equals( System.getProperty( "log4j1.compatibility" ), "true" ) );
        assumeTrue( "Both -Dgemma.slack.token and -Dgemma.slack.channel must be set for this test.",
                slackToken != null && slackChannel != null );
    }

    @After
    public void tearDown() {
        reset( mockedSlack );
    }

    @Test
    public void test() throws SlackApiException, IOException {
        try {
            raiseStackTrace();
            fail( "This should not be reached." );
        } catch ( IllegalArgumentException e ) {
            log.error( "This is just a test for the Log4j Slack appender.", e );
        }
        // TODO: verify( mockedSlack ).methods( slackToken );
        // TODO: verify( mockedSlack.methods( slackToken ) ).chatPostMessage( any( ChatPostMessageRequest.class ) );
    }

    @Test
    public void testWarnLogsShouldNotBeAppended() {
        log.warn( "This should not be captured by the Slack appender." );
        // TODO: verifyNoInteractions( mockedSlack );
    }

    private void raiseStackTrace() {
        throw new IllegalArgumentException( "This is the cause of the error which should be part of an attachment." );
    }

}
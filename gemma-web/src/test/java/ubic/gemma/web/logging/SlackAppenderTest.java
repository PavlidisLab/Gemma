package ubic.gemma.web.logging;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

public class SlackAppenderTest {

    private static Log log = LogFactory.getLog( SlackAppenderTest.class );

    private Slack mockedSlack;

    private String slackToken;
    private String slackChannel;

    @Before
    public void setUp() {
        slackToken = System.getProperty( "gemma.slack.token" );
        slackChannel = System.getProperty( "gemma.slack.channel" );
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
        try ( MockedStatic<Slack> slack = Mockito.mockStatic( Slack.class ) ) {
            slack.when( Slack::getInstance ).thenReturn( mockedSlack );
            try {
                raiseStackTrace();
                fail( "This should not be reached." );
            } catch ( IllegalArgumentException e ) {
                log.error( "This is just a test for the Log4j Slack appender.", e );
            }
            verify( mockedSlack ).methods( slackToken );
            verify( mockedSlack.methods( slackToken ) ).chatPostMessage( any( ChatPostMessageRequest.class ) );
        }
    }

    @Test
    public void testWarnLogsShouldNotBeAppended() {
        try ( MockedStatic<Slack> slack = Mockito.mockStatic( Slack.class ) ) {
            slack.when( Slack::getInstance ).thenReturn( mockedSlack );
            log.warn( "This should not be captured by the Slack appender." );
            verifyNoInteractions( mockedSlack );
        }
    }

    private void raiseStackTrace() {
        throw new IllegalArgumentException( "This is the cause of the error which should be part of an attachment." );
    }

}
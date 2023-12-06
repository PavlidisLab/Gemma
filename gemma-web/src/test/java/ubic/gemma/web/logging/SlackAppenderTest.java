package ubic.gemma.web.logging;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import org.apache.log4j.Level;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SlackAppenderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Slack mockedSlack;

    @Mock
    private MethodsClient mockedMethodClient;

    @Mock
    private ErrorHandler errorHandler;

    private SlackAppender appender;
    private String slackToken = "1234";
    private String slackChannel = "#gemma";

    @Before
    public void setUp() {
        when( mockedSlack.methods( any( String.class ) ) ).thenReturn( mockedMethodClient );
        appender = new SlackAppender( mockedSlack );
        appender.setErrorHandler( errorHandler );
        appender.setToken( slackToken );
        appender.setChannel( slackChannel );
        appender.setThreshold( Level.ERROR );
        appender.setLayout( new org.apache.log4j.PatternLayout( "%m" ) );
    }

    @Test
    public void test() throws Exception {
        LoggingEvent event = mock();
        when( event.getRenderedMessage() ).thenReturn( "test" );
        when( event.getLevel() ).thenReturn( Level.ERROR );
        appender.doAppend( event );
        appender.close();
        verify( mockedMethodClient ).chatPostMessage( any( ChatPostMessageRequest.class ) );
        ArgumentCaptor<ChatPostMessageRequest> captor = ArgumentCaptor.forClass( ChatPostMessageRequest.class );
        verify( mockedMethodClient ).chatPostMessage( captor.capture() );
        assertThat( captor.getValue() ).satisfies( m -> {
            assertThat( m.getText() ).isEqualTo( "test" );
        } );
        verify( mockedSlack ).close();
    }

    @Test
    public void testWithStacktrace() throws SlackApiException, IOException {
        LoggingEvent event = mock();
        when( event.getRenderedMessage() ).thenReturn( "test" );
        when( event.getLevel() ).thenReturn( Level.ERROR );
        ThrowableInformation ti = mock();
        when( ti.getThrowable() ).thenReturn( new RuntimeException( "foo", new RuntimeException( "bar" ) ) );
        when( event.getThrowableInformation() ).thenReturn( ti );
        appender.doAppend( event );
        ArgumentCaptor<ChatPostMessageRequest> captor = ArgumentCaptor.forClass( ChatPostMessageRequest.class );
        verify( mockedMethodClient ).chatPostMessage( captor.capture() );
        assertThat( captor.getValue() ).satisfies( m -> {
            assertThat( m.getText() ).isEqualTo( "test" );
            assertThat( m.getAttachments() )
                    .hasSize( 1 )
                    .first()
                    .satisfies( att -> {
                        assertThat( att )
                                .hasFieldOrPropertyWithValue( "title", "RuntimeException: foo" )
                                .hasFieldOrPropertyWithValue( "fallback", "This attachment normally contains an error stacktrace." );
                        assertThat( att.getText() ).contains( "Caused by: java.lang.RuntimeException: bar" );
                    } );
        } );
    }

    @Test
    public void testWarnLogsShouldNotBeAppended() {
        LoggingEvent event = mock();
        when( event.getLevel() ).thenReturn( Level.WARN );
        appender.doAppend( event );
        verifyNoInteractions( mockedSlack );
    }

    @Test
    public void testWhenSlackApiRaisesException() throws SlackApiException, IOException {
        SlackApiException e = new SlackApiException( mock(), "test" );
        doThrow( e ).when( mockedMethodClient ).chatPostMessage( any( ChatPostMessageRequest.class ) );
        LoggingEvent event = mock();
        when( event.getLevel() ).thenReturn( Level.ERROR );
        appender.doAppend( event );
        verify( mockedMethodClient ).chatPostMessage( any( ChatPostMessageRequest.class ) );
        verify( errorHandler ).error( "Failed to send logging event to Slack channel #gemma.", e, 1000, event );
    }

    @Test
    public void testWhenSlackFailsToClose() throws Exception {
        SlackApiException e = new SlackApiException( mock(), "test" );
        doThrow( e ).when( mockedSlack ).close();
        appender.close();
        verify( errorHandler ).error( "Failed to close the Slack instance.", e, 1001 );
    }
}
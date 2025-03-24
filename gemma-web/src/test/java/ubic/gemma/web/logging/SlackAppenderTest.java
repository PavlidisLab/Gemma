package ubic.gemma.web.logging;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ubic.gemma.core.logging.log4j.SlackAppender;

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

    @Before
    public void setUp() {
        when( mockedSlack.methods( any( String.class ) ) ).thenReturn( mockedMethodClient );
        appender = new SlackAppender.Builder()
                .setName( "slack" )
                .setToken( "1234" )
                .setChannel( "#gemma" )
                .setLayout( PatternLayout.newBuilder().withPattern( "%m%throwable{none}" ).build() )
                .build();
        appender.setSlackInstance( mockedSlack );
        appender.setHandler( errorHandler );
    }

    @Test
    public void test() throws Exception {
        LogEvent event = mock();
        Message message = mock();
        when( message.getFormattedMessage() ).thenReturn( "test" );
        when( event.getMessage() ).thenReturn( message );
        when( event.getLevel() ).thenReturn( Level.ERROR );
        appender.append( event );
        appender.stop();
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
        LogEvent event = mock();
        Message message = mock();
        when( message.getFormattedMessage() ).thenReturn( "test" );
        when( event.getMessage() ).thenReturn( message );
        when( event.getLevel() ).thenReturn( Level.ERROR );
        when( event.getThrown() ).thenReturn( new RuntimeException( "foo", new RuntimeException( "bar" ) ) );
        appender.append( event );
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
    public void testWhenSlackApiRaisesException() throws SlackApiException, IOException {
        SlackApiException e = new SlackApiException( mock(), "test" );
        doThrow( e ).when( mockedMethodClient ).chatPostMessage( any( ChatPostMessageRequest.class ) );
        LogEvent event = mock();
        when( event.getLevel() ).thenReturn( Level.ERROR );
        Message message = mock();
        when( message.getFormattedMessage() ).thenReturn( "test" );
        when( event.getMessage() ).thenReturn( message );
        appender.append( event );
        verify( mockedMethodClient ).chatPostMessage( any( ChatPostMessageRequest.class ) );
        verify( errorHandler ).error( "Failed to send logging event to Slack channel #gemma.", event, e );
    }

    @Test
    public void testWhenSlackFailsToClose() throws Exception {
        SlackApiException e = new SlackApiException( mock(), "test" );
        doThrow( e ).when( mockedSlack ).close();
        appender.stop();
        verify( errorHandler ).error( "Failed to close the Slack instance.", null, e );
    }
}
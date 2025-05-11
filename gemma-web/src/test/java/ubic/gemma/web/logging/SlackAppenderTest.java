package ubic.gemma.web.logging;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.TextObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ubic.gemma.core.logging.log4j.SlackAppender;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        MutableLogEvent event = new MutableLogEvent();
        Message message = new SimpleMessage( "test" );
        event.setMessage( message );
        event.setLevel( Level.ERROR );
        appender.append( event );
        appender.stop();
        verify( mockedMethodClient ).chatPostMessage( any( ChatPostMessageRequest.class ) );
        ArgumentCaptor<ChatPostMessageRequest> captor = ArgumentCaptor.forClass( ChatPostMessageRequest.class );
        verify( mockedMethodClient ).chatPostMessage( captor.capture() );
        assertThat( captor.getValue() ).satisfies( m -> {
            assertThat( ( ( SectionBlock ) m.getBlocks().get( 0 ) ).getText().getText() ).isEqualTo( "test" );
        } );
        verify( mockedSlack ).close();
    }

    @Test
    public void testWithStacktrace() throws SlackApiException, IOException {
        MutableLogEvent event = new MutableLogEvent();
        Message message = new SimpleMessage( "test" );
        event.setMessage( message );
        event.setLevel( Level.ERROR );
        event.setThrown( new RuntimeException( "foo", new RuntimeException( "bar" ) ) );
        appender.append( event );
        ArgumentCaptor<ChatPostMessageRequest> captor = ArgumentCaptor.forClass( ChatPostMessageRequest.class );
        verify( mockedMethodClient ).chatPostMessage( captor.capture() );
        assertThat( captor.getValue() ).satisfies( m -> {
            assertThat( ( ( SectionBlock ) m.getBlocks().get( 0 ) ).getText().getText() ).isEqualTo( "test" );
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
        MutableLogEvent event = new MutableLogEvent();
        event.setLevel( Level.ERROR );
        Message message = new SimpleMessage( "test" );
        event.setMessage( message );
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

    @Test
    public void testWithContextData() throws SlackApiException, IOException {
        MutableLogEvent event = new MutableLogEvent();
        event.setLoggerName( "Foo" );
        event.setLevel( Level.INFO );
        Map<String, String> map = new HashMap<>();
        map.put( "k", "v" );
        event.setContextData( new JdkMapAdapterStringMap( map, false ) );
        appender.append( event );
        ArgumentCaptor<ChatPostMessageRequest> captor = ArgumentCaptor.forClass( ChatPostMessageRequest.class );
        verify( mockedMethodClient ).chatPostMessage( captor.capture() );
        assertThat( captor.getValue() ).satisfies( m -> {
            assertThat( m.getBlocks() ).hasSize( 2 );
            assertThat( m.getBlocks().get( 1 ) )
                    .asInstanceOf( InstanceOfAssertFactories.type( SectionBlock.class ) )
                    .satisfies( section -> {
                        assertThat( section.getFields() )
                                .extracting( TextObject::getText )
                                .contains( "*Logger Name*", "Foo", "*k*", "v" );
                    } );
        } );
    }

    @Test
    public void testWithContextStack() throws SlackApiException, IOException {
        MutableLogEvent event = new MutableLogEvent();
        event.setLoggerName( "Foo" );
        event.setLevel( Level.INFO );
        DefaultThreadContextStack cs = new DefaultThreadContextStack();
        cs.push( "foo" );
        cs.push( "bar" );
        event.setContextStack( cs );
        appender.append( event );
        ArgumentCaptor<ChatPostMessageRequest> captor = ArgumentCaptor.forClass( ChatPostMessageRequest.class );
        verify( mockedMethodClient ).chatPostMessage( captor.capture() );
        assertThat( captor.getValue() ).satisfies( m -> {
            assertThat( m.getBlocks() ).hasSize( 2 );
            assertThat( m.getBlocks().get( 1 ) )
                    .asInstanceOf( InstanceOfAssertFactories.type( SectionBlock.class ) )
                    .satisfies( section -> {
                        assertThat( section.getFields() )
                                .extracting( TextObject::getText )
                                .contains( "*Logger Name*", "Foo", "*Stack*", "foo → bar" );
                    } );
        } );
    }

}
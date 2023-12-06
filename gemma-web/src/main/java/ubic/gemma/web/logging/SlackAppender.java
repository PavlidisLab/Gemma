package ubic.gemma.web.logging;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.Attachment;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.util.Collections;

public class SlackAppender extends AppenderSkeleton implements Appender {

    /**
     * Constant to use when reporting errors to the {@link #errorHandler}.
     */
    private static final int
            ERROR_ON_POST_MESSAGE_CODE = 1000,
            ERROR_ON_CLOSE_CODE = 1001;

    private final Slack slackInstance;

    private String token;
    private String channel;

    /**
     * Used in log4j.properties via reflection.
     */
    @SuppressWarnings("unused")
    public SlackAppender() {
        slackInstance = new Slack();
    }

    public SlackAppender( Slack slackInstance ) {
        this.slackInstance = slackInstance;
    }

    @Override
    protected void append( LoggingEvent loggingEvent ) {
        if ( !isAsSevereAsThreshold( loggingEvent.getLevel() ) ) {
            return;
        }
        try {
            ChatPostMessageRequest.ChatPostMessageRequestBuilder request = ChatPostMessageRequest.builder()
                    .channel( channel )
                    .text( this.layout.format( loggingEvent ) );

            // attach a stacktrace if available
            if ( loggingEvent.getThrowableInformation() != null )
                request.attachments( Collections.singletonList( throwableAsAttachment( loggingEvent.getThrowableInformation().getThrowable() ) ) );

            slackInstance.methods( token ).chatPostMessage( request.build() );
        } catch ( IOException | SlackApiException e ) {
            errorHandler.error( String.format( "Failed to send logging event to Slack channel %s.", channel ), e, ERROR_ON_POST_MESSAGE_CODE, loggingEvent );
        }
    }

    @Override
    public void close() {
        try {
            slackInstance.close();
        } catch ( Exception e ) {
            errorHandler.error( "Failed to close the Slack instance.", e, ERROR_ON_CLOSE_CODE );
        }
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    public void setToken( String token ) {
        this.token = token;
    }

    public void setChannel( String channel ) {
        this.channel = channel;
    }

    private Attachment throwableAsAttachment( Throwable t ) {
        return Attachment.builder()
                .title( ExceptionUtils.getMessage( t ) )
                .text( ExceptionUtils.getStackTrace( t ) )
                .fallback( "This attachment normally contains an error stacktrace." )
                .build();
    }
}

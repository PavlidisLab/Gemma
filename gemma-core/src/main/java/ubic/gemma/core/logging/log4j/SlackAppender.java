package ubic.gemma.core.logging.log4j;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.Attachment;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Log4j2 appender that report log events to a Slack channel.
 * @author poirigui
 */
@Plugin(name = "Slack", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class SlackAppender extends AbstractAppender {

    public static class Builder extends AbstractAppender.Builder<Builder> implements org.apache.logging.log4j.core.util.Builder<SlackAppender> {

        @PluginBuilderAttribute
        @Required
        private String token;
        @PluginBuilderAttribute
        @Required
        private String channel;

        public Builder setToken( String token ) {
            this.token = token;
            return this;
        }

        public Builder setChannel( String channel ) {
            this.channel = channel;
            return this;
        }

        @Override
        public SlackAppender build() {
            return new SlackAppender( getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray(), token, channel );
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Nullable
    private Slack slackInstance;

    private final String token;
    private final String channel;

    private SlackAppender( final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions, final Property[] properties, String token, String channel ) {
        super( name, filter, layout, ignoreExceptions, properties );
        this.token = token;
        this.channel = channel;
    }

    public void setSlackInstance( Slack slackInstance ) {
        Assert.isNull( this.slackInstance, "The Slack instance was already created." );
        this.slackInstance = slackInstance;
    }

    @Override
    public void append( LogEvent loggingEvent ) {
        try {
            ChatPostMessageRequest.ChatPostMessageRequestBuilder request = ChatPostMessageRequest.builder()
                    .channel( channel )
                    .text( new String( getLayout().toByteArray( loggingEvent ), StandardCharsets.UTF_8 ) );

            // attach a stacktrace if available
            if ( loggingEvent.getThrown() != null )
                request.attachments( Collections.singletonList( throwableAsAttachment( loggingEvent.getThrown() ) ) );

            getSlackInstance().methods( token ).chatPostMessage( request.build() );
        } catch ( IOException | SlackApiException e ) {
            getHandler().error( String.format( "Failed to send logging event to Slack channel %s.", channel ), loggingEvent, e );
        }
    }

    private synchronized Slack getSlackInstance() {
        if ( slackInstance == null ) {
            slackInstance = Slack.getInstance();
        }
        return slackInstance;
    }

    @Override
    public void stop() {
        if ( slackInstance == null ) {
            return;
        }
        try {
            slackInstance.close();
        } catch ( Exception e ) {
            getHandler().error( "Failed to close the Slack instance.", null, e );
        }
    }

    private Attachment throwableAsAttachment( Throwable t ) {
        return Attachment.builder()
                .title( ExceptionUtils.getMessage( t ) )
                .text( ExceptionUtils.getStackTrace( t ) )
                .fallback( "This attachment normally contains an error stacktrace." )
                .build();
    }
}

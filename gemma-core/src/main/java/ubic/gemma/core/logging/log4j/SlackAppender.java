package ubic.gemma.core.logging.log4j;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.Attachment;
import com.slack.api.model.Message;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.composition.TextObject;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * Log4j2 appender that report log events to a Slack channel.
 *
 * @author poirigui
 */
@Plugin(name = "Slack", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE)
public class SlackAppender extends AbstractAppender {

    private static final int MAX_FIELDS_PER_SECTION = 10;

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
            return new SlackAppender( getName(), getFilter(), isIgnoreExceptions(), getPropertyArray(), token, channel );
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
    private final MessageSource messageSource;

    private SlackAppender( final String name, final Filter filter, final boolean ignoreExceptions, final Property[] properties, String token, String channel ) {
        super( name, filter, null, ignoreExceptions, properties );
        this.token = token;
        this.channel = channel;
        ReloadableResourceBundleMessageSource bundle = new ReloadableResourceBundleMessageSource();
        bundle.setBasenames( "classpath:ubic/gemma/core/logging/log4j/messages" );
        this.messageSource = bundle;
    }

    public void setSlackInstance( Slack slackInstance ) {
        Assert.isNull( this.slackInstance, "The Slack instance was already created." );
        this.slackInstance = slackInstance;
    }

    @Override
    public void append( LogEvent loggingEvent ) {
        try {
            Locale locale = Locale.getDefault();
            ChatPostMessageRequest.ChatPostMessageRequestBuilder request = ChatPostMessageRequest.builder()
                    .channel( channel )
                    .metadata( metadataFromLogEvent( loggingEvent ) )
                    .blocks( blocksFromLogEvent( loggingEvent, locale ) );

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

    private List<LayoutBlock> blocksFromLogEvent( LogEvent loggingEvent, Locale locale ) {
        ArrayList<LayoutBlock> blocks = new ArrayList<>();
        blocks.add( messageFromLogEvent( loggingEvent ) );
        blocks.addAll( metadataAsTextFromLogEvent( loggingEvent, locale ) );
        return blocks;
    }

    private LayoutBlock messageFromLogEvent( LogEvent loggingEvent ) {
        return SectionBlock.builder()
                .text( PlainTextObject.builder()
                        .text( StringUtils.abbreviate( loggingEvent.getMessage().getFormattedMessage(), 3000 ) )
                        .build() )
                .build();
    }

    /**
     * Note: there's a limit of 10 fields per section
     */
    private List<LayoutBlock> metadataAsTextFromLogEvent( LogEvent loggingEvent, Locale locale ) {
        List<TextObject> fields = new ArrayList<>();
        if ( loggingEvent.getLoggerName() != null ) {
            fields.add( bold( messageSource.getMessage( "fields.loggerName", null, locale ) ) );
            fields.add( span( loggingEvent.getLoggerName() ) );
        }
        if ( loggingEvent.getInstant() != null ) {
            fields.add( bold( messageSource.getMessage( "fields.date", null, locale ) ) );
            fields.add( span( new Date( loggingEvent.getInstant().getEpochMillisecond() ).toString() ) );
        }
        if ( loggingEvent.getLevel() != null ) {
            fields.add( bold( messageSource.getMessage( "fields.level", null, locale ) ) );
            fields.add( span( loggingEvent.getLevel().toString() ) );
        }
        if ( loggingEvent.getThreadName() != null ) {
            fields.add( bold( messageSource.getMessage( "fields.threadName", null, locale ) ) );
            fields.add( span( loggingEvent.getThreadName() ) );
        }
        if ( loggingEvent.getSource() != null ) {
            fields.add( bold( messageSource.getMessage( "fields.source", null, locale ) ) );
            fields.add( span( loggingEvent.getSource().toString() ) );
        }
        if ( loggingEvent.getContextData() != null ) {
            loggingEvent.getContextData().forEach( ( k, v ) -> {
                fields.add( bold( messageSource.getMessage( "fields.contextData." + k, null, k, locale ) ) );
                fields.add( span( String.valueOf( v ) ) );
            } );
        }
        if ( loggingEvent.getContextStack() != null && !loggingEvent.getContextStack().isEmpty() ) {
            fields.add( bold( messageSource.getMessage( "fields.contextStack", null, locale ) ) );
            fields.add( span( String.join( " → ", loggingEvent.getContextStack() ) ) );
        }
        List<LayoutBlock> blocks = new ArrayList<>();
        for ( List<TextObject> batch : ListUtils.partition( fields, MAX_FIELDS_PER_SECTION ) ) {
            blocks.add( SectionBlock.builder().fields( batch ).build() );
        }
        return blocks;
    }

    private TextObject bold( String text ) {
        return MarkdownTextObject.builder().text( "*" + StringUtils.abbreviate( text, 2000 - 2 ) + "*" ).build();
    }

    private TextObject span( String text ) {
        return PlainTextObject.builder().text( StringUtils.abbreviate( text, 2000 ) ).build();
    }

    private Message.Metadata metadataFromLogEvent( LogEvent event ) {
        HashMap<String, Object> map = new HashMap<>();
        map.put( "message", event.getMessage().getFormattedMessage() );
        map.put( "logger_name", event.getLoggerName() );
        map.put( "logger_fqcn", event.getLoggerFqcn() );
        map.put( "thread_name", event.getThreadName() );
        map.put( "thread_id", event.getThreadId() );
        map.put( "thread_priority", event.getThreadPriority() );
        if ( event.getLevel() != null ) {
            map.put( "level", event.getLevel().toString() );
        }
        map.put( "time_millis", event.getTimeMillis() );
        if ( event.getContextStack() != null ) {
            map.put( "context_stack", new ArrayList<>( event.getContextStack().asList() ) );
        }
        if ( event.getContextData() != null ) {
            map.put( "context_data", new HashMap<>( event.getContextData().toMap() ) );
        }
        if ( event.getSource() != null ) {
            map.put( "source", event.getSource().toString() );
        }
        return Message.Metadata.builder()
                .eventType( "log_event" )
                .eventPayload( map )
                .build();
    }

    private Attachment throwableAsAttachment( Throwable t ) {
        return Attachment.builder()
                .title( ExceptionUtils.getMessage( t ) )
                .text( ExceptionUtils.getStackTrace( t ) )
                .fallback( "This attachment normally contains an error stacktrace." )
                .build();
    }
}

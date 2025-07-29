/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.logging.log4j;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import ubic.gemma.core.job.progress.ProgressUpdateCallback;
import ubic.gemma.core.job.progress.ProgressUpdateContext;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This appender is used by remote tasks to send progress notifications to the webapp. The information for these
 * notifications is retrieved from the {@link LogEvent}. This information comes from regular logging statements
 * inlined in the source code (ie. log.info("the text")).
 *
 * @author keshav
 */
@Plugin(name = "ProgressUpdate", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE)
public class ProgressUpdateAppender extends AbstractAppender {

    /**
     * Key used in {@link ThreadContext} for referring to the current progress update context.
     */
    private static final String CURRENT_PROGRESS_UPDATE_CONTEXT_KEY = "ubic.gemma.core.logging.log4j.ProgressUpdateAppender.currentContextKey";

    private static final Map<String, ProgressUpdateContext> contextMap = new ConcurrentHashMap<>();

    /**
     * Obtain the current context for reporting progress, if any.
     */
    static Optional<ProgressUpdateContext> currentContext() {
        return Optional.of( ProgressUpdateAppender.CURRENT_PROGRESS_UPDATE_CONTEXT_KEY )
                .map( ThreadContext::get )
                .map( contextMap::get );
    }

    /**
     * Create a new context for observing progress.
     */
    public static ProgressUpdateContext createContext( ProgressUpdateCallback callback ) {
        return new ProgressUpdateContextImpl( callback );
    }

    public static class Builder extends AbstractAppender.Builder<Builder> implements org.apache.logging.log4j.core.util.Builder<ProgressUpdateAppender> {

        @Override
        public ProgressUpdateAppender build() {
            return new ProgressUpdateAppender( getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray() );
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private ProgressUpdateAppender( String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties ) {
        super( name, filter, layout, ignoreExceptions, properties );
    }

    @Override
    public void append( LogEvent event ) {
        currentContext().ifPresent( context -> {
            context.reportProgressUpdate( new String( getLayout().toByteArray( event ), StandardCharsets.UTF_8 ) );
        } );
    }

    /**
     * Represents a context under which progress update logs are intercepted the {@link ProgressUpdateCallback} is
     * invoked.
     * @author poirigui
     */
    public static class ProgressUpdateContextImpl implements ProgressUpdateContext {

        private final String key;
        private final ProgressUpdateCallback progressUpdateCallback;

        private String previousKey;

        private ProgressUpdateContextImpl( ProgressUpdateCallback progressUpdateCallback ) {
            this.key = UUID.randomUUID().toString();
            this.progressUpdateCallback = progressUpdateCallback;
            init();
        }

        private void init() {
            contextMap.put( key, this );
            previousKey = ThreadContext.get( CURRENT_PROGRESS_UPDATE_CONTEXT_KEY );
            ThreadContext.put( CURRENT_PROGRESS_UPDATE_CONTEXT_KEY, key );
        }

        /**
         * Report a progress update.
         */
        public void reportProgressUpdate( String message ) {
            try ( CloseableThreadContext.Instance ignored = CloseableThreadContext
                    .put( CURRENT_PROGRESS_UPDATE_CONTEXT_KEY, null ) ) {
                progressUpdateCallback.onProgressUpdate( message );
            }
        }

        @Override
        public void close() {
            if ( previousKey != null ) {
                ThreadContext.put( CURRENT_PROGRESS_UPDATE_CONTEXT_KEY, previousKey );
            } else {
                ThreadContext.remove( CURRENT_PROGRESS_UPDATE_CONTEXT_KEY );
            }
            contextMap.remove( key, this );
        }
    }

}

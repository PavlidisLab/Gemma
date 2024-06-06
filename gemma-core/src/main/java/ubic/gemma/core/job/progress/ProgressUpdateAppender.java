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
package ubic.gemma.core.job.progress;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This appender is used by remote tasks to send progress notifications to the webapp. The information for these
 * notifications is retrieved from the {@link LoggingEvent}. This information comes from regular logging statements
 * inlined in the source code (ie. log.info("the text")).
 *
 * @author keshav
 */
public class ProgressUpdateAppender extends AppenderSkeleton {

    /**
     * Key used in {@link MDC} for referring to the current progress update context.
     */
    private static final String MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY = "currentProgressUpdateAppenderContext";

    @Override
    protected void append( LoggingEvent event ) {
        if ( !isAsSevereAsThreshold( event.getLevel() ) ) {
            return;
        }
        ProgressUpdateContext progressUpdateContext = ( ProgressUpdateContext ) MDC.get( MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY );
        if ( progressUpdateContext == null )
            return;
        Object previousContext = MDC.get( MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY );
        MDC.remove( MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY );
        progressUpdateContext.getProgressUpdateCallback().onProgressUpdate( event.getRenderedMessage() );
        MDC.put( MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY, previousContext );
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * Represents a context under which progress update logs are intercepted the {@link ProgressUpdateCallback} is
     * invoked.
     * @author poirigui
     */
    public static class ProgressUpdateContext implements AutoCloseable {

        public static ProgressUpdateContext currentContext() {
            return ( ProgressUpdateContext ) MDC.get( ProgressUpdateAppender.MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY );
        }

        private final ProgressUpdateCallback progressUpdateCallback;

        public ProgressUpdateContext( ProgressUpdateCallback progressUpdateCallback ) {
            this.progressUpdateCallback = progressUpdateCallback;
            init();
        }

        private void init() {
            MDC.put( ProgressUpdateAppender.MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY, this );
        }

        public ProgressUpdateCallback getProgressUpdateCallback() {
            return progressUpdateCallback;
        }

        @Override
        public void close() {
            MDC.remove( ProgressUpdateAppender.MDC_CURRENT_PROGRESS_UPDATE_CONTEXT_KEY );
        }
    }

    /**
     * Callback used to emit progress updates.
     * @author poirigui
     */
    public interface ProgressUpdateCallback {
        void onProgressUpdate( String message );
    }
}

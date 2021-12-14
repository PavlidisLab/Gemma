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
package ubic.gemma.core.job.executor.common;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This appender is used by remote tasks to send progress notifications to the webapp. The information for these
 * notifications is retrieved from the {@link LoggingEvent}. This information comes from regular logging statements
 * inlined in the source code (ie. log.info("the text")).
 *
 * @author keshav
 */
public class ProgressUpdateAppender extends AppenderSkeleton {

    /**
     * Key used in {@link MDC} for referring ot the task identifier.
     */
    private static final String MDC_TASK_ID_KEY = "taskId";

    private static final ConcurrentMap<String, ProgressUpdateCallback> progressUpdateCallbackByTaskId = new ConcurrentHashMap<>();

    public static void setProgressUpdateCallback( String taskId, ProgressUpdateCallback callback ) {
        progressUpdateCallbackByTaskId.put( taskId, callback );
    }

    public static void removeProgressUpdateCallback( String taskId ) {
        progressUpdateCallbackByTaskId.remove( taskId );
    }

    @Override
    protected void append( LoggingEvent event ) {
        String taskId = ( String ) event.getMDC( MDC_TASK_ID_KEY );
        if ( taskId == null )
            return;
        ProgressUpdateCallback callback = progressUpdateCallbackByTaskId.get( taskId );
        if ( callback != null ) {
            Object oldTaskId = MDC.get( MDC_TASK_ID_KEY );
            MDC.remove( MDC_TASK_ID_KEY );
            callback.addProgressUpdate( event.getRenderedMessage() );
            MDC.put( MDC_TASK_ID_KEY, oldTaskId );
        }
    }

    @Override
    public void close() {
        progressUpdateCallbackByTaskId.clear();
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * Represents a task context under which the {@link ProgressUpdateCallback} is invoked.
     * @author poirigui
     */
    public static class TaskContext implements AutoCloseable {

        public static TaskContext currentContext() {
            String taskId = ( String ) MDC.get( ProgressUpdateAppender.MDC_TASK_ID_KEY );
            return taskId != null ? new TaskContext( taskId ) : null;
        }

        private final String taskId;

        public TaskContext( String taskId ) {
            this.taskId = taskId;
            init();
        }

        private void init() {
            MDC.put( ProgressUpdateAppender.MDC_TASK_ID_KEY, this.taskId );
        }

        public String getTaskId() {
            return taskId;
        }

        @Override
        public void close() {
            MDC.remove( ProgressUpdateAppender.MDC_TASK_ID_KEY );
        }
    }
}

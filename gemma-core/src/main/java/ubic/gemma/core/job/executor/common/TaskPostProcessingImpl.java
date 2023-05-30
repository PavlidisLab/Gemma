/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.EmailNotificationContext;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.util.MailUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * author: anton date: 10/02/13
 */
@Component
public class TaskPostProcessingImpl implements TaskPostProcessing {
    private static final Log log = LogFactory.getLog( TaskPostProcessingImpl.class );

    @Autowired
    private MailUtils mailUtils;

    @Override
    public void addEmailNotification( CompletableFuture<? extends TaskResult> future, EmailNotificationContext context, Executor executor ) {
        future.thenAcceptAsync( taskResult -> {
            // This will be called when future with our running task is done.
            mailUtils.sendTaskCompletedNotificationEmail( context, taskResult );
        }, executor ).exceptionally( throwable -> {
            TaskPostProcessingImpl.log.error( "Shouldn't happen since we take care of exceptions inside ExecutingTask. ", throwable );
            return null;
        } );
    }
}

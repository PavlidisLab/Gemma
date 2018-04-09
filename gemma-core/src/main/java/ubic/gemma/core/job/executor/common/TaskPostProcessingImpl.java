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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.EmailNotificationContext;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.util.MailUtils;

/**
 * author: anton date: 10/02/13
 */
@Component
public class TaskPostProcessingImpl implements TaskPostProcessing {
    private static final Log log = LogFactory.getLog( TaskPostProcessingImpl.class );

    @Autowired
    private MailUtils mailUtils;

    @Override
    public void addEmailNotification( ListenableFuture<TaskResult> future, EmailNotificationContext context ) {
        FutureCallback<TaskResult> emailNotificationCallback = this.createEmailNotificationFutureCallback( context );
        // This will be called when future with our running task is done.
        Futures.addCallback( future, emailNotificationCallback );
    }

    private FutureCallback<TaskResult> createEmailNotificationFutureCallback( final EmailNotificationContext context ) {

        return new FutureCallback<TaskResult>() {
            private final EmailNotificationContext emailNotificationContext = context;

            @Override
            public void onSuccess( TaskResult taskResult ) {
                mailUtils.sendTaskCompletedNotificationEmail( emailNotificationContext, taskResult );
            }

            @Override
            public void onFailure( Throwable throwable ) {
                TaskPostProcessingImpl.log
                        .error( "Shouldn't happen since we take care of exceptions inside ExecutingTask. " + throwable
                                .getMessage() );
            }
        };
    }

}

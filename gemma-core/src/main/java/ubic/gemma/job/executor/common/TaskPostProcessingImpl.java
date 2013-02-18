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
package ubic.gemma.job.executor.common;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.EmailNotificationContext;
import ubic.gemma.job.TaskResult;
import ubic.gemma.util.MailUtils;

/**
 * author: anton
 * date: 10/02/13
 */
@Component
public class TaskPostProcessingImpl implements TaskPostProcessing {
    private static Log log = LogFactory.getLog( TaskPostProcessingImpl.class );

    @Autowired private MailUtils mailUtils;

    @Override
    public void addEmailNotification( ListenableFuture<TaskResult> future, EmailNotificationContext context ) {
        FutureCallback<TaskResult> emailNotificationCallback = createEmailNotificationFutureCallback( context );
        // This will be called when future with our running task is done.
        Futures.addCallback( future, emailNotificationCallback );
    }


    private FutureCallback<TaskResult> createEmailNotificationFutureCallback( final EmailNotificationContext context ) {
        FutureCallback<TaskResult> futureCallback = new FutureCallback<TaskResult>() {
            private EmailNotificationContext emailNotifcationContext = context;

            @Override
            public void onSuccess( TaskResult taskResult ) {
                mailUtils.sendTaskCompletedNotificationEmail( emailNotifcationContext, taskResult );
            }

            @Override
            public void onFailure( Throwable throwable ) {
                log.error( "Shouldn't happen since we take care of exceptions inside ExecutingTask. "
                        + throwable.getMessage() );
            }
        };

        return futureCallback;
    }

}

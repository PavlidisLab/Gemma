/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.job.grid.worker;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.*;
import ubic.gemma.job.grid.util.JMSBroker;
import ubic.gemma.util.MailUtils;

import javax.jms.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 03/01/13
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
@Component("taskControlListener")
public class TaskControlListener implements MessageListener {

    private static final Log log = LogFactory.getLog( TaskControlListener.class );

    @Autowired private RemoteTaskRunningService remoteTaskRunningService;
    @Autowired private JMSBroker jmsBroker;
    @Autowired private MailUtils mailUtils;

    @Override
    public void onMessage( Message message) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            TaskControl taskControl = (TaskControl) objectMessage.getObject();
            String taskId = taskControl.getTaskId();

            assert taskId != null;

            ListenableFuture<TaskResult> future = remoteTaskRunningService.getRunningTaskFuture( taskControl.getTaskId() );

            switch (taskControl.getRequest()) {
                case CANCEL:
                    log.info( "Received CANCEL task control message for task: " + taskControl.getTaskId() );
                    if (future != null) {
                        boolean cancelled = future.cancel( true );
                        if ( cancelled ) {
                            Destination lifeCycleQueue = new ActiveMQQueue( "task.lifeCycle."+taskId );
                            TaskStatusUpdate statusUpdate = new TaskStatusUpdate( SubmittedTask.Status.CANCELLED, new Date() );
                            jmsBroker.sendMessage( lifeCycleQueue, statusUpdate );
                        }
                    }
                    break;
                case ADD_EMAIL_NOTIFICATION:
                    log.info( "Received ADD_EMAIL_NOTIFICATION task control message for task: " + taskControl.getTaskId() );
                    // This will be called when future with our running task is done.
                    FutureCallback<TaskResult> emailNotificationCallback = new FutureCallback<TaskResult>() {
                        @Override
                        public void onSuccess( TaskResult taskResult ) {
                            mailUtils.sendTaskCompletedNotificationEmail( taskResult );
                        }

                        @Override
                        public void onFailure( Throwable throwable ) {
                            log.error( "Shouldn't happen since we take care of exceptions inside ExecutingTask. "
                                    + throwable.getMessage() );
                        }
                    };
                    Futures.addCallback( future, emailNotificationCallback );
                    break;
            }

        } catch (JMSException e) {
            log.warn( "Got JMSException: " + e.getMessage() );
        }
    }

}

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
package ubic.gemma.job.executor.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.executor.common.TaskControl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 *
 *
 */
@Component("taskControlListener")
public class TaskControlListener implements MessageListener {

    private static final Log log = LogFactory.getLog( TaskControlListener.class );

    @Autowired private RemoteTaskRunningService remoteTaskRunningService;

    @Override
    public void onMessage( Message message ) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            TaskControl taskControl = (TaskControl) objectMessage.getObject();
            String taskId = taskControl.getTaskId();
            assert taskId != null;

            SubmittedTaskRemote submittedTask = remoteTaskRunningService.getSubmittedTask( taskControl.getTaskId() );
            if (submittedTask == null) {
                log.warn( "Got control request for taskId:" + taskId + ", but no submitted task found." );
                return;
            }

            switch (taskControl.getRequest()) {
                case CANCEL:
                    log.info( "Received CANCEL control message for task: " + taskControl.getTaskId() );
                    submittedTask.requestCancellation();
                    break;
                case ADD_EMAIL_NOTIFICATION:
                    log.info( "Received ADD_EMAIL_NOTIFICATION control message for task: " + taskControl.getTaskId() );
                    submittedTask.addEmailAlertNotificationAfterCompletion();
                    break;
            }

        } catch (JMSException e) {
            log.warn( "Got JMSException: " + e.getMessage() );
        }
    }
}

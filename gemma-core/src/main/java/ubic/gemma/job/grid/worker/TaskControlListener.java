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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.RemoteTaskRunningService;
import ubic.gemma.job.TaskControl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.concurrent.Future;

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

    @Autowired RemoteTaskRunningService remoteTaskRunningService;

    @Override
    public void onMessage( Message message) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            TaskControl taskControl = (TaskControl) objectMessage.getObject();
            switch (taskControl.getRequest()) {
                case CANCEL:
                    log.info( "Received CANCEL task control message for task: " + taskControl.getTaskId() );
                    Future future = remoteTaskRunningService.getRunningTaskFuture( taskControl.getTaskId() );
                    if (future != null) {
                        future.cancel( true );
                    }
                    break;
                case ADD_EMAIL_NOTIFICATION:
                    log.info( "Received ADD_EMAIL_NOTIFICATION task control message for task: " + taskControl.getTaskId() );
                    // remoteTaskRunningService
                    //TODO: Implement me!
                    break;
            }

        } catch (JMSException e) {
            log.warn( "Got JMSException: " + e.getMessage() );
        }
    }
}

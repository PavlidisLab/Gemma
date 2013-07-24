/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.job.executor.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.TaskCommand;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * Part of worker context. Receives commands and submits them to the RemoteTaskRunningService
 * 
 * @author anton
 * @version $Id$
 */
@Component("taskSubmissionListener")
public class TaskSubmissionListener implements MessageListener {
    private static final Log log = LogFactory.getLog( TaskSubmissionListener.class );

    @Autowired
    RemoteTaskRunningService remoteTaskRunningService;

    @Override
    public void onMessage( Message message ) {
        log.info( "Received new remote task command!" );

        ObjectMessage objectMessage = ( ObjectMessage ) message;
        TaskCommand taskCommand = null;
        try {
            taskCommand = ( TaskCommand ) objectMessage.getObject();
        } catch ( JMSException e ) {
            e.printStackTrace();
        }
        log.info( "Submitting task command for execution." );

        remoteTaskRunningService.submit( taskCommand );
    }
}

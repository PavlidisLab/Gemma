/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.job.grid.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.stereotype.Component;
import ubic.gemma.util.ConfigUtils;

import javax.jms.*;
import java.util.Enumeration;

/**
 * TODO
 */
@Component
public class JMSBrokerMonitorImpl implements JMSBrokerMonitor {
    protected static Log log = LogFactory.getLog( JMSBrokerMonitorImpl.class );

    @Autowired(required = false)
    @Qualifier("amqJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Override
    public boolean isRemoteTasksEnabled() {
        return ConfigUtils.isRemoteTasksEnabled();
    }

    @Override
    public boolean canServiceRemoteTasks() {
        boolean isAnyWorkerHostAlive;
        try {
            isAnyWorkerHostAlive = getNumberOfWorkerHosts() > 0;
        } catch ( JMSException e ) {
            // Broker is probably down.
            isAnyWorkerHostAlive = false;
        }
        return isAnyWorkerHostAlive;
    }

    @Override
    public int getNumberOfWorkerHosts() throws JMSException {
        MapMessage reply = sendTaskSubmissionQueueDiagnosticMessage();
        return ( int ) reply.getLong( "consumerCount" );
    }

    @Override
    public int getTaskSubmissionQueueLength() throws JMSException {
        MapMessage reply = sendTaskSubmissionQueueDiagnosticMessage();
        return ( int ) reply.getLong( "enqueueCount" );
    }

    @Override
    public String getTaskSubmissionQueueDiagnosticMessage() throws JMSException {
        MapMessage reply = sendTaskSubmissionQueueDiagnosticMessage();

        if ( reply == null ) return "Statistics plugin appears to be turned off or is too slow";

        String message = "";
        for ( Enumeration e = reply.getMapNames(); e.hasMoreElements(); ) {
            String name = e.nextElement().toString();
            message += name + "=" + reply.getObject( name ) + "\n";
        }

        return message;
    }

    private MapMessage sendTaskSubmissionQueueDiagnosticMessage() throws JMSException {
        MapMessage reply = jmsTemplate.execute( new SessionCallback<MapMessage>() {
            @Override
            public MapMessage doInJms( Session session ) throws JMSException {
                Queue replyTo = session.createTemporaryQueue();
                Message message = session.createMessage();
                message.setJMSReplyTo( replyTo );
                Queue queryQueue = session.createQueue( "ActiveMQ.Statistics.Destination.tasks.submit" );
                MessageProducer producer = session.createProducer( queryQueue );
                MessageConsumer consumer = session.createConsumer( replyTo );
                producer.send( message );
                return ( MapMessage ) consumer.receive( 5000 );
            }
        }, true );
        return reply;
    }
}

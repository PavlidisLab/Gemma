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
package ubic.gemma.infrastructure.jms;

import org.apache.activemq.command.ActiveMQQueue;
import ubic.gemma.infrastructure.common.MessageReceiver;

import javax.jms.Queue;

/**
 * author: anton
 * date: 08/02/13
 */
public class JmsMessageReceiver<T> implements MessageReceiver<T> {

    private JMSHelper jmsHelper;
    private Queue queue;

    public JmsMessageReceiver( JMSHelper jmsHelper, String queueName ) {
        this.jmsHelper = jmsHelper;
        this.queue = new ActiveMQQueue( queueName );
    }

    @Override
    public T receive() {
        Object message = jmsHelper.receiveMessage( queue );
        return (T) message;
    }

    @Override
    public T blockingReceive() {
        Object message = jmsHelper.blockingReceiveMessage( queue );
        return (T) message;
    }
}

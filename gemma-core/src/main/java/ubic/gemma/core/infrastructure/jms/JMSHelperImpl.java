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
package ubic.gemma.core.infrastructure.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.io.Serializable;

/**
 * *
 *
 * @author anton
 */
@Component
public class JMSHelperImpl implements JMSHelper {

    @Autowired
    @Qualifier("amqJmsTemplate")
    private JmsTemplate amqJmsTemplate;

    @Override
    public Object blockingReceiveMessage( final Destination destination ) {

        return amqJmsTemplate.execute( new SessionCallback<Object>() {
            @Override
            public Object doInJms( Session session ) throws JMSException {
                MessageConsumer consumer = session.createConsumer( destination );
                Message message = consumer.receive();
                if ( message == null )
                    return null;
                ObjectMessage objectMessage = ( ObjectMessage ) message;
                return objectMessage.getObject();
            }
        }, true );
    }

    /**
     * Doesn't wait. Gets object from destination if present.
     *
     * @param destination destination
     * @return object or null if queue is empty
     */
    @Override
    public Object receiveMessage( final Destination destination ) {

        return amqJmsTemplate.execute( new SessionCallback<Object>() {
            @Override
            public Object doInJms( Session session ) throws JMSException {
                MessageConsumer consumer = session.createConsumer( destination );
                Message message = consumer.receiveNoWait();
                if ( message == null )
                    return null;
                ObjectMessage objectMessage = ( ObjectMessage ) message;
                return objectMessage.getObject();
            }
        }, true );
    }

    /**
     * Sends active-mq message to specified destination. We clear interrupt flag and reset it after the call. Since we
     * want the task with interrupt request
     *
     * @param destination destination
     * @param object      object
     */
    @Override
    public void sendMessage( Destination destination, final Serializable object ) {
        boolean savedInterrupted = Thread.interrupted(); // Actually clears the flag.
        try {
            amqJmsTemplate.send( destination, new MessageCreator() {
                @Override
                public Message createMessage( Session session ) throws JMSException {
                    return session.createObjectMessage( object );
                }
            } );
        } finally {
            if ( savedInterrupted )
                Thread.currentThread().interrupt(); // Be nice and reset the flag.
        }
    }
}

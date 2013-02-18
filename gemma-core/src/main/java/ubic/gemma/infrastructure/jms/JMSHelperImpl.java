package ubic.gemma.infrastructure.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 05/02/13
 * Time: 9:55 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class JMSHelperImpl implements JMSHelper {

    @Autowired @Qualifier("amqJmsTemplate") private JmsTemplate amqJmsTemplate;

    /**
     * Sends active-mq message to specified destination.
     *
     * We clear interrupt flag and reset it after the call.
     * Since we want the task with interrupt request
     *
     * @param destination
     * @param object
     */
    public void sendMessage( Destination destination, final Serializable object ) {
        boolean savedInterrupted = Thread.interrupted(); // Actually clears the flag.
        try {
            amqJmsTemplate.send( destination, new MessageCreator() {
                @Override
                public Message createMessage( Session session ) throws JMSException {
                    ObjectMessage message = session.createObjectMessage( object );
                    return message;
                }
            } );
        } finally {
            if ( savedInterrupted ) Thread.currentThread().interrupt(); // Be nice and reset the flag.
        }
    }

    /**
     * Doesn't wait. Gets object from destination if present.
     *
     * @param destination
     * @return object or null if queue is empty
     */
    @Override
    public Object receiveMessage( final Destination destination ) {
        Object receivedObject = amqJmsTemplate.execute( new SessionCallback<Object>() {
            @Override
            public Object doInJms( Session session ) throws JMSException {
                MessageConsumer consumer = session.createConsumer( destination );
                Message message = consumer.receiveNoWait();
                if (message == null) return null;
                ObjectMessage objectMessage = (ObjectMessage) message;
                return objectMessage.getObject();
            }
        },true);

        return receivedObject;
    }

    @Override
    public Object blockingReceiveMessage( final Destination destination ) {
        Object receivedObject = amqJmsTemplate.execute( new SessionCallback<Object>() {
            @Override
            public Object doInJms( Session session ) throws JMSException {
                MessageConsumer consumer = session.createConsumer( destination );
                Message message = consumer.receive();
                if (message == null) return null;
                ObjectMessage objectMessage = (ObjectMessage) message;
                return objectMessage.getObject();
            }
        },true);

        return receivedObject;
    }
}

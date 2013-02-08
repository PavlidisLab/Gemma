package ubic.gemma.job.grid.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA. User: anton Date: 05/02/13 Time: 9:55 AM To change this template use File | Settings |
 * File Templates.
 */
@Component
public class JMSBrokerImpl implements JMSBroker {

    @Autowired(required = false)
    @Qualifier("amqJmsTemplate")
    private JmsTemplate amqJmsTemplate;

    public void sendMessage( Destination destination, final Serializable object ) {
        amqJmsTemplate.send( destination, new MessageCreator() {
            @Override
            public Message createMessage( Session session ) throws JMSException {
                ObjectMessage message = session.createObjectMessage( object );
                return message;
            }
        } );
    }

}

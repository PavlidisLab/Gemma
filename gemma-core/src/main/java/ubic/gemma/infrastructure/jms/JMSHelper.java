package ubic.gemma.infrastructure.jms;

import java.io.Serializable;

import javax.jms.Destination;

/**
 * Created with IntelliJ IDEA. User: anton Date: 05/02/13 Time: 9:55 AM To change this template use File | Settings |
 * File Templates.
 */
public interface JMSHelper {

    public Object blockingReceiveMessage( Destination destination );

    public Object receiveMessage( Destination destination );

    // TODO: have two versions one that ignores Thread.intterupt flag and one that doesn't
    public void sendMessage( Destination destination, final Serializable object );
}

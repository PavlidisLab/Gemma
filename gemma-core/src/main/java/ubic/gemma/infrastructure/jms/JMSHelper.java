package ubic.gemma.infrastructure.jms;

import javax.jms.Destination;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 05/02/13
 * Time: 9:55 AM
 * To change this template use File | Settings | File Templates.
 */
public interface JMSHelper {

    //TODO: have two versions one that ignores Thread.intterupt flag and one that doesn't
    public void sendMessage( Destination destination, final Serializable object );

    public Object receiveMessage (Destination destination);
    public Object blockingReceiveMessage( Destination destination );
}

package ubic.gemma.job.grid.util;

import javax.jms.Destination;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 05/02/13
 * Time: 9:55 AM
 * To change this template use File | Settings | File Templates.
 */
public interface JMSBroker {
    public void sendMessage( Destination destination, final Serializable object );
}

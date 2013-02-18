package ubic.gemma.util;

import ubic.gemma.job.EmailNotificationContext;
import ubic.gemma.job.TaskResult;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 04/02/13
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public interface MailUtils {

    public void sendTaskCompletedNotificationEmail( EmailNotificationContext context, TaskResult taskResult );
}

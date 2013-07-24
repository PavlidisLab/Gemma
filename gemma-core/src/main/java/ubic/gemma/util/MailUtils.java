package ubic.gemma.util;

import ubic.gemma.job.EmailNotificationContext;
import ubic.gemma.job.TaskResult;

/**
 * @author anton
 * @version $Id$
 */
public interface MailUtils {

    /**
     * @param context
     * @param taskResult
     */
    public void sendTaskCompletedNotificationEmail( EmailNotificationContext context, TaskResult taskResult );
}

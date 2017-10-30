package ubic.gemma.core.util;

import ubic.gemma.core.job.EmailNotificationContext;
import ubic.gemma.core.job.TaskResult;

/**
 * @author anton
 *
 */
public interface MailUtils {

    /**
     * @param context
     * @param taskResult
     */
    public void sendTaskCompletedNotificationEmail( EmailNotificationContext context, TaskResult taskResult );
}

package ubic.gemma.core.util;

import ubic.gemma.core.job.EmailNotificationContext;
import ubic.gemma.core.job.TaskResult;

/**
 * @author anton
 */
public interface MailUtils {

    void sendTaskCompletedNotificationEmail( EmailNotificationContext context, TaskResult taskResult );
}

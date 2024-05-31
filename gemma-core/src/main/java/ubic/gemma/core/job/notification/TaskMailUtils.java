package ubic.gemma.core.job.notification;

import ubic.gemma.core.job.TaskResult;

/**
 * @author anton
 */
public interface TaskMailUtils {

    void sendTaskCompletedNotificationEmail( EmailNotificationContext context, TaskResult taskResult );
}

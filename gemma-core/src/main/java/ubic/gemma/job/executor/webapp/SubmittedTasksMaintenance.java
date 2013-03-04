package ubic.gemma.job.executor.webapp;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.job.SubmittedTask;

import java.util.Collection;
import java.util.Date;

/**
 * TODO: finish!
 * Remove entries that's been executing or waiting for their results to be picked up for too long.
 *  monitor finished tasks that have not been retrieved.
 */
@Component
public class SubmittedTasksMaintenance {

    private static Log log = LogFactory.getLog( SubmittedTasksMaintenance.class );

    @Autowired private TaskRunningService taskRunningService;

    private static final int MAX_QUEUE_MINUTES = 60;

    /**
     * How long we will hold onto results after a task has finished before removing it from task list.
     */
    private static final int MAX_KEEP_TRACK_AFTER_COMPLETED_MINUTES = 120;

    /**
     * Check if a task has been running or queued for too long, and cancel it if necessary. Email alert will always be
     * sent in that case.
     *
     */
    @Scheduled(fixedDelay = 120000 )
    public void doSubmittedTasksMaintenance() {
        log.info( "Doing submitted tasks maintenance." );
        // Assumes collection implementing weakly consistent iterator with remove support.
        Collection<SubmittedTask> tasks = taskRunningService.getSubmittedTasks();

        for (SubmittedTask task : tasks) {
            switch (task.getStatus()) {
                case QUEUED:
                    Date submissionTime = task.getSubmissionTime();
                    Integer maxQueueWait = task.getTaskCommand().getMaxQueueMinutes();
                    assert submissionTime != null;
                    assert maxQueueWait != null;

                    if (submissionTime.before( DateUtils.addMinutes( new Date(), -maxQueueWait ) )) {
                        log.warn( "Submitted task " + task.getTaskCommand().getClass().getSimpleName()
                                + " has been queued for too long (max=" + maxQueueWait
                                + "minutes), attempting to cancel: " + task.getTaskId() );

                        task.addEmailAlert();
                        // -> email admin? is worker dead?

                        task.requestCancellation();
                    }
                    break;
                case RUNNING:
                    Date startTime = task.getStartTime();
                    int maxRunTime = task.getTaskCommand().getMaxRuntime();
                    assert startTime != null;

                    if (startTime.before( DateUtils.addMinutes( new Date(), -maxRunTime ) )) {
                        log.warn( "Running task is taking too long, attempting to cancel: " + task.getTaskId()
                                + " " + task.getTaskCommand().getClass().getSimpleName() );

                        task.addEmailAlert();

                        task.requestCancellation();
                    }
                    break;
                case CANCELLING:

                    break;
                case FAILED:
                case COMPLETED:
                    if (task.getFinishTime().before(
                            DateUtils.addMinutes( new Date(),
                                    -MAX_KEEP_TRACK_AFTER_COMPLETED_MINUTES ) )) {
                        log.debug( task.getStatus().name() + " task result not retrieved, timing out: "
                                + task.getTaskId() + " "
                                + task.getTaskCommand().getClass().getSimpleName() );
                        tasks.remove( task );
                    }
                    break;
                case UNKNOWN:

                    break;
            }
        }
    }
}

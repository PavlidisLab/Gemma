package ubic.gemma.job;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Remove entries that's been executing or waiting for their results to be picked up for too long.
 */
public class SubmittedTasksMaintenanceThread implements Runnable {

    private static Log log = LogFactory.getLog( SubmittedTasksMaintenanceThread.class );

    /**
     * How often we look for tasks to cleanup (milliseconds). This should be set to be longer than the grid monitor task
     * interval. The longer it is, the longer it will be before dead tasks are noticed.
     */
    private static final int TASK_CLEANUP_FREQUENCY = 30000;

    private static final int MAX_QUEUE_MINUTES = 60;

    /**
     * How long we will hold onto results after a task has finished before giving up.
     */
    private static final int MAX_KEEP_TRACK_AFTER_COMPLETED_MINUTES = 30;


    /**
     * Map to work on. Assumes ConcurrentHashMap because it requires weakly consistent iterator with remove support.
     */
    private Map<String, SubmittedTask> submittedTasks;
    private TaskRunningService taskRunningService;

    public SubmittedTasksMaintenanceThread( Map<String, SubmittedTask> submittedTasks,
                                            TaskRunningService taskRunningService ) {
        this.submittedTasks = submittedTasks;
        this.taskRunningService = taskRunningService;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep( TASK_CLEANUP_FREQUENCY );
            } catch (InterruptedException e) {
                return;
            }
            checkSubmittedTasks();
        }
    }

    /**
     * Check if a task has been running or queued for too long, and cancel it if necessary. Email alert will always be
     * sent in that case.
     *
     */
    private void checkSubmittedTasks() {
        // Weakly consistent iterator, supports remove
        Collection<SubmittedTask> tasks = submittedTasks.values();
        for (SubmittedTask task : tasks) {
            switch (task.getStatus()) {
                case QUEUED:
                    Date submissionTime = task.getSubmissionTime();
                    Integer maxQueueWait = task.getCommand().getMaxQueueMinutes();
                    assert submissionTime != null;
                    assert maxQueueWait != null;

                    if (submissionTime.before( DateUtils.addMinutes( new Date(), -maxQueueWait ) )) {
                        log.warn( "Submitted task " + task.getCommand().getClass().getSimpleName()
                                + " has been queued for too long (max=" + maxQueueWait
                                + "minutes), attempting to cancel: " + task.getTaskId() );

                        task.addEmailAlert();
                        // -> email admin? is worker dead?

                        taskRunningService.cancelTask( task );
                    }
                    break;
                case RUNNING:
                    Date startTime = task.getStartTime();
                    int maxRunTime = task.getCommand().getMaxRuntime();
                    assert startTime != null;

                    if (startTime.before( DateUtils.addMinutes( new Date(), -maxRunTime ) )) {
                        log.warn( "Running task is taking too long, attempting to cancel: " + task.getTaskId()
                                + " " + task.getCommand().getClass().getSimpleName() );

                        task.addEmailAlert();

                        task.cancel();
                    }
                    break;
                case FAILED:
                case DONE:
                case CANCELLED:
                    if (task.getFinishTime().before(
                            DateUtils.addMinutes( new Date(),
                                    -MAX_KEEP_TRACK_AFTER_COMPLETED_MINUTES ) )) {
                        log.debug( task.getStatus().name() + " task result not retrieved, timing out: "
                                + task.getTaskId() + " "
                                + task.getCommand().getClass().getSimpleName() );
                        tasks.remove( task );
                    }
                    break;
                case UNKNOWN:

                    break;
            }
            if (Thread.currentThread().isInterrupted()) return;
        }
    }
}

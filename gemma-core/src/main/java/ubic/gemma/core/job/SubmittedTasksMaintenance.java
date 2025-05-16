package ubic.gemma.core.job;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;

@Component
public class SubmittedTasksMaintenance {

    /**
     * How long we will hold onto results after a task has finished before removing it from task list.
     */
    private static final int MAX_KEEP_TRACK_AFTER_COMPLETED_MINUTES = 10;
    private static final Log log = LogFactory.getLog( SubmittedTasksMaintenance.class );
    @Autowired
    private TaskRunningService taskRunningService;

    /**
     * Check if a task has been running or queued for too long, and cancel it if necessary. Email alert will always be
     * sent in that case.
     */
    @Scheduled(fixedDelay = 120000)
    public void doSubmittedTasksMaintenance() {
        // Assumes collection implementing weakly consistent iterator with remove support.
        Collection<SubmittedTask> tasks = taskRunningService.getSubmittedTasks();

        int numQueued = 0;
        int numRunning = 0;
        int numDone = 0;
        int numCancelled = 0;
        for ( SubmittedTask task : tasks ) {
            if ( !task.getStatus().equals( SubmittedTask.Status.COMPLETED ) ) {
                SubmittedTasksMaintenance.log
                        .info( "Checking task: " + task.getTaskCommand().getClass().getSimpleName() + task.getTaskId()
                                + " started=" + task.getStartTime() + " status=" + task.getStatus() );
            }
            switch ( task.getStatus() ) {
                case QUEUED:
                    numQueued++;
                    Date submissionTime = task.getSubmissionTime();
                    long maxQueueWaitMillis = task.getTaskCommand().getMaxQueueMillis();
                    assert submissionTime != null;

                    if ( maxQueueWaitMillis >= 0 && System.currentTimeMillis() > submissionTime.getTime() + maxQueueWaitMillis ) {
                        SubmittedTasksMaintenance.log.warn( String.format( "Submitted task has been queued for too long (max=%d ms), attempting to cancel: %s %s",
                                maxQueueWaitMillis, task.getTaskId(), task.getTaskCommand().getClass().getSimpleName() ) );

                        task.addEmailAlert();
                        // -> email admin? is worker dead?

                        task.requestCancellation();
                    }
                    break;
                case RUNNING:
                    numRunning++;
                    Date startTime = task.getStartTime();
                    long maxRunTimeMillis = task.getTaskCommand().getMaxRuntimeMillis();
                    assert startTime != null;

                    if ( maxRunTimeMillis >= 0 && System.currentTimeMillis() > startTime.getTime() + maxRunTimeMillis ) {
                        SubmittedTasksMaintenance.log.warn( String.format( "Running task is taking too long (max=%d ms), attempting to cancel: %s %s",
                                maxRunTimeMillis, task.getTaskId(), task.getTaskCommand().getClass().getSimpleName() ) );

                        task.addEmailAlert();

                        task.requestCancellation();
                    }
                    break;
                case CANCELLING:
                    numCancelled++;
                    break;
                case FAILED:
                    // fall through
                case COMPLETED:
                    numDone++;
                    if ( task.getFinishTime().before( DateUtils.addMinutes( new Date(),
                            -SubmittedTasksMaintenance.MAX_KEEP_TRACK_AFTER_COMPLETED_MINUTES ) ) ) {
                        SubmittedTasksMaintenance.log
                                .debug( task.getStatus().name() + " task result not retrieved, timing out: " + task
                                        .getTaskId() + " " + task.getTaskCommand().getClass().getSimpleName() );
                        // concurrent modification.
                        tasks.remove( task );
                    }
                    break;
                case UNKNOWN:
                    break;
                default:
                    break;
            }
        }

        if ( !tasks.isEmpty() && numDone != tasks.size() )
            SubmittedTasksMaintenance.log.info( String.format( "%d tasks monitored; Done: %d; Running: %d; Cancelled: %d; Queued: %d",
                    tasks.size(), numDone, numRunning, numCancelled, numQueued ) );
    }
}

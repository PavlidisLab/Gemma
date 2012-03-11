package ubic.gemma.job.progress;

import java.util.Collection;
import java.util.List;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;

public interface ProgressStatusService {

    /**
     * Set up an email alert for this job; an email will be sent when it has finished (or failed).
     * 
     * @param taskId
     */
    public abstract void addEmailAlert( String taskId );

    /**
     * Attempt to cancel the job.
     * 
     * @param taskId
     * @return true if cancelling was error-free, false otherwise.
     */
    public abstract boolean cancelJob( String taskId );

    /**
     * @return
     * @see ubic.gemma.job.TaskRunningServiceImpl#getCancelledTasks()
     */
    @Secured({ "GROUP_ADMIN" })
    public abstract Collection<TaskCommand> getCancelledTasks();

    /**
     * @return
     * @see ubic.gemma.job.TaskRunningServiceImpl#getFailedTasks()
     */
    @Secured({ "GROUP_ADMIN" })
    public abstract Collection<TaskResult> getFailedTasks();

    /**
     * @return
     * @see ubic.gemma.job.TaskRunningServiceImpl#getFinishedTasks()
     */
    @Secured({ "GROUP_ADMIN" })
    public abstract Collection<TaskResult> getFinishedTasks();

    /**
     * Get the latest information about how a job is doing.
     * 
     * @param taskId
     * @return
     */
    public abstract List<ProgressData> getProgressStatus( String taskId );

    /**
     * @return
     * @see ubic.gemma.job.TaskRunningServiceImpl#getSubmittedTasks()
     */
    @Secured({ "GROUP_ADMIN" })
    public abstract Collection<TaskCommand> getSubmittedTasks();

}
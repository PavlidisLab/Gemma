package ubic.gemma.job;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

public interface SubmittedTask <T extends TaskResult> {

    static enum Status {QUEUED, RUNNING, FAILED, DONE, CANCELLED, UNKNOWN}

    public String getTaskId();
    public TaskCommand getCommand();

    public Queue<String> getProgressUpdates();

    /**
     * Send cancel request.
     * TODO: Handle case where task is waiting in the queue when it's being cancelled
     */
    public void cancel();


    public Date getStartTime();
    public Date getSubmissionTime();
    public Date getFinishTime();

    public Status getStatus();
    boolean isDone();

    /**
     * Get the result of the task. Blocking call. Re-throws exception thrown by the task.
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public T getResult() throws ExecutionException, InterruptedException;

    public boolean isRunningRemotely();

    void addEmailAlert();
    public boolean isEmailAlert();

}
package ubic.gemma.job.executor.common;

import java.io.Serializable;

/**
 *
 * This is used to send control requests to remotely queued/running tasks.
 *
 *
 */
public class TaskControl implements Serializable {
    public enum Request{CANCEL, ADD_EMAIL_NOTIFICATION}

    private Request request;
    private String taskId;

    public TaskControl(String taskId, Request request ) {
        this.taskId = taskId;
        this.request = request;
    }

    public Request getRequest() {
        return this.request;
    }

    public String getTaskId() {
        return taskId;
    }
}

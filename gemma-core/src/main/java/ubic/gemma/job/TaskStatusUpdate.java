package ubic.gemma.job;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 07/01/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class TaskStatusUpdate implements Serializable {

    //TODO: maybe add message as well. ex: Cancelled due to running for too long.
    private SubmittedTask.Status status;
    private Date statusChangeTime;

    public TaskStatusUpdate( SubmittedTask.Status status, Date statusChangeTime ) {
        this.status = status;
        this.statusChangeTime = statusChangeTime;
    }

    public SubmittedTask.Status getStatus() {
        return status;
    }

    public void setStatus( SubmittedTask.Status status ) {
        this.status = status;
    }

    public Date getStatusChangeTime() {
        return statusChangeTime;
    }

    public void setStatusChangeTime( Date statusChangeTime ) {
        this.statusChangeTime = statusChangeTime;
    }
}

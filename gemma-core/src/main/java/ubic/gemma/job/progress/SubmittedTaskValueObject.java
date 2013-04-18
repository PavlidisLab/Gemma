package ubic.gemma.job.progress;

import org.apache.commons.lang.StringUtils;
import ubic.gemma.job.SubmittedTask;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class SubmittedTaskValueObject implements Serializable {

    private String taskId;
    private Date submissionTime;
    private Date startTime;
    private Date finishTime;
    private String submitter;
    private String taskType;
    private String logMessages;
    private boolean runningRemotely;
    private boolean done;
    private boolean emailAlert;
    private String taskStatus;

    private String lastLogMessage;

    public SubmittedTaskValueObject() {
    }

    public SubmittedTaskValueObject( SubmittedTask submittedTask ) {
        this.taskId = submittedTask.getTaskId();
        this.taskType = submittedTask.getTaskCommand().getTaskClass() == null ? "Not specified" : submittedTask.getTaskCommand().getTaskClass().getSimpleName();
        this.submitter = submittedTask.getTaskCommand().getSubmitter();
        this.submissionTime = submittedTask.getSubmissionTime();
        this.startTime = submittedTask.getStartTime();
        this.finishTime = submittedTask.getFinishTime();
        this.runningRemotely = submittedTask.isRunningRemotely();
        this.taskStatus = submittedTask.getStatus().name();
        this.done = submittedTask.isDone();
        this.emailAlert = submittedTask.isEmailAlert();
        this.logMessages = StringUtils.join( submittedTask.getProgressUpdates(), "\n" );
        this.lastLogMessage = submittedTask.getLastProgressUpdates();
    }

    public String getLogMessages() {
        return logMessages;
    }

    public String getLastLogMessage() {
        return lastLogMessage;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public Date getSubmissionTime() {
        return submissionTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public boolean getRunningRemotely() {
        return runningRemotely;
    }

    public boolean getDone() {
        return done;
    }

    public boolean getEmailAlert() {
        return emailAlert;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getSubmitter() {
        return submitter;
    }

    public String getTaskType() {
        return taskType;
    }

    public static Collection<SubmittedTaskValueObject> convert2ValueObjects(Collection<SubmittedTask> submittedTasks) {

        Collection<SubmittedTaskValueObject> converted = new HashSet<SubmittedTaskValueObject>();
        if ( submittedTasks == null ) return converted;

        for ( SubmittedTask submittedTask : submittedTasks ) {
            converted.add( new SubmittedTaskValueObject( submittedTask ) );
        }

        return converted;
    }
}

package ubic.gemma.job.progress;

import ubic.gemma.job.SubmittedTask;
import ubic.gemma.job.TaskCommandValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class SubmittedTaskValueObject implements Serializable {

    private String taskId;
    private Date submissionTime;
    private Date startTime;
    private String submitter;
    private String taskType;
    private boolean runningRemotely;

//    private TaskCommandValueObject taskCommandValueObject;

    public SubmittedTaskValueObject() {
    }

    public SubmittedTaskValueObject( SubmittedTask submittedTask ) {
        this.taskId = submittedTask.getTaskId();
        this.taskType = submittedTask.getCommand().getTaskClass() == null ? "Not specified" : submittedTask.getCommand().getTaskClass().getSimpleName();
        this.submitter = submittedTask.getCommand().getSubmitter();
        this.submissionTime = submittedTask.getSubmissionTime();
        this.startTime = submittedTask.getStartTime();
        this.runningRemotely = submittedTask.isRunningRemotely();
//        this.taskCommandValueObject = new TaskCommandValueObject( submittedTask.getCommand() );
    }

    public Date getSubmissionTime() {
        return submissionTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public boolean getRunningRemotely() {
        return runningRemotely;
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

//    public TaskCommandValueObject getTaskCommandValueObject() {
//        return taskCommandValueObject;
//    }

    public static Collection<SubmittedTaskValueObject> convert2ValueObjects(Collection<SubmittedTask> submittedTasks) {

        Collection<SubmittedTaskValueObject> converted = new HashSet<SubmittedTaskValueObject>();
        if ( submittedTasks == null ) return converted;

        for ( SubmittedTask submittedTask : submittedTasks ) {
            converted.add( new SubmittedTaskValueObject( submittedTask ) );
        }

        return converted;
    }
}

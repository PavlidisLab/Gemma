package ubic.gemma.rest;

import lombok.Getter;
import lombok.Setter;import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisRemoveTaskCommand;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.BatchInfoFetchTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.SvdTaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Queue;

/**
 * Snapshot of a {@link SubmittedTask} for the REST API.
 * <p>
 * Returned by the dispatch endpoints (with HTTP 202 + a {@code Location} header) and by
 * {@code GET /tasks/{taskId}}. The underlying task store is in-memory only and tasks are evicted roughly
 * 10 minutes after completion, so {@code GET /tasks/{taskId}} returns 404 once the window elapses.
 */
@Getter
@Setter
public class TaskStatusValueObject {

    private String taskId;
    /**
     * Identifier of the experiment the task is operating on, when derivable from the task command.
     */
    @Nullable
    private Long experimentId;
    /**
     * Pipeline step the task corresponds to: {@code preprocess}, {@code pca}, {@code batchInfo}, {@code dea},
     * {@code deaRemove}. {@code null} for tasks whose command class isn't a known pipeline command.
     */
    @Nullable
    private String step;
    /**
     * Lower-cased {@link SubmittedTask.Status} value:
     * {@code queued}, {@code running}, {@code completed}, {@code failed}, {@code cancelling}, {@code unknown}.
     */
    private String status;
    @Nullable
    private Date submittedAt;
    @Nullable
    private Date startedAt;
    @Nullable
    private Date completedAt;
    /**
     * Last progress update emitted by the task. Empty string when nothing is available.
     */
    private String message;

    public TaskStatusValueObject() {
    }

    public TaskStatusValueObject( SubmittedTask task ) {
        this.taskId = task.getTaskId();
        this.status = task.getStatus() != null ? task.getStatus().name().toLowerCase() : "unknown";
        this.submittedAt = task.getSubmissionTime();
        this.startedAt = task.getStartTime();
        this.completedAt = task.getFinishTime();
        this.message = lastMessage( task );
        TaskCommand cmd = task.getTaskCommand();
        if ( cmd != null ) {
            this.step = stepFor( cmd );
            this.experimentId = experimentIdFor( cmd );
        }
    }

    private static String lastMessage( SubmittedTask task ) {
        String last = task.getLastProgressUpdates();
        if ( last != null && !last.isEmpty() ) {
            return last;
        }
        Queue<String> updates = task.getProgressUpdates();
        if ( updates != null && !updates.isEmpty() ) {
            String tail = null;
            for ( String s : updates ) {
                tail = s;
            }
            return tail != null ? tail : "";
        }
        return "";
    }

    @Nullable
    private static String stepFor( TaskCommand cmd ) {
        // Order matters: subclass checks must come before their parents.
        if ( cmd instanceof DifferentialExpressionAnalysisRemoveTaskCommand ) {
            return "deaRemove";
        }
        if ( cmd instanceof DifferentialExpressionAnalysisTaskCommand ) {
            return "dea";
        }
        if ( cmd instanceof PreprocessTaskCommand ) {
            return ( ( PreprocessTaskCommand ) cmd ).diagnosticsOnly() ? "pca" : "preprocess";
        }
        if ( cmd instanceof BatchInfoFetchTaskCommand ) {
            return "batchInfo";
        }
        if ( cmd instanceof SvdTaskCommand ) {
            return "pca";
        }
        return null;
    }

    @Nullable
    private static Long experimentIdFor( TaskCommand cmd ) {
        ExpressionExperiment ee = null;
        if ( cmd instanceof DifferentialExpressionAnalysisTaskCommand ) {
            ee = ( ( DifferentialExpressionAnalysisTaskCommand ) cmd ).getExpressionExperiment();
        } else if ( cmd instanceof PreprocessTaskCommand ) {
            ee = ( ( PreprocessTaskCommand ) cmd ).getExpressionExperiment();
        } else if ( cmd instanceof BatchInfoFetchTaskCommand ) {
            ee = ( ( BatchInfoFetchTaskCommand ) cmd ).getExpressionExperiment();
        } else if ( cmd instanceof SvdTaskCommand ) {
            ee = ( ( SvdTaskCommand ) cmd ).getExpressionExperiment();
        }
        return ee != null ? ee.getId() : null;
    }
}
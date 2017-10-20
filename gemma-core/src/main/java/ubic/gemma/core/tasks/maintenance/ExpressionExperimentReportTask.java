package ubic.gemma.core.tasks.maintenance;

import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.Task;

/**
 * Handles delegation of report generation (to the space, or run locally)
 * 
 * @author klc
 *
 */

public interface ExpressionExperimentReportTask extends Task<TaskResult, ExpressionExperimentReportTaskCommand> {
}



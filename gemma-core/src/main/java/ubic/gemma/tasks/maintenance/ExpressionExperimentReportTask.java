package ubic.gemma.tasks.maintenance;

import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.Task;

/**
 * Handles delegation of report generation (to the space, or run locally)
 * 
 * @author klc
 * @version $Id$
 */

public interface ExpressionExperimentReportTask extends Task<TaskResult, ExpressionExperimentReportTaskCommand> {
}



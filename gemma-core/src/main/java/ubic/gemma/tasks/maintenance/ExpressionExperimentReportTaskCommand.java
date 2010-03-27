package ubic.gemma.tasks.maintenance;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class ExpressionExperimentReportTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment = null;
    private boolean all = false;

    public ExpressionExperimentReportTaskCommand( Boolean all ) {
        super();
        this.all = all;
    }

    public ExpressionExperimentReportTaskCommand( ExpressionExperiment expressionExperiment ) {
        super();
        this.expressionExperiment = expressionExperiment;
    }

    public ExpressionExperimentReportTaskCommand( String taskId, Boolean all ) {
        super();
        this.setTaskId( taskId );
        this.all = all;
    }

    /**
     * @param taskId
     */
    public ExpressionExperimentReportTaskCommand( String taskId, ExpressionExperiment expressionExperiment ) {
        super();
        this.setTaskId( taskId );
        this.expressionExperiment = expressionExperiment;
    }

    public boolean doAll() {
        return all;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setAll( Boolean all ) {
        this.all = all;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

}

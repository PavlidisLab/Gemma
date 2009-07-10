package ubic.gemma.grid.javaspaces.expression.experiment;

import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class ExpressionExperimentReportTaskCommand  extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment = null;
    private boolean all = false;
    
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public boolean doAll(){
        return all;
    }
    
    public void setAll(Boolean all){
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

    public ExpressionExperimentReportTaskCommand( String taskId, Boolean all ) {
        super();
        this.setTaskId( taskId );
        this.all = all;
    }
    
    public ExpressionExperimentReportTaskCommand( Boolean all ) {
        super();
        this.all = all;
    }
    
    public ExpressionExperimentReportTaskCommand(ExpressionExperiment expressionExperiment ) {
        super();
        this.expressionExperiment = expressionExperiment;
    }


}

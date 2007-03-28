package ubic.gemma.javaspaces.gigaspaces;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

public class ExpressionExperimentTaskImpl implements ExpressionExperimentTask {

    private long counter = 0;
    private ExpressionExperimentService expressionExperimentService = null;

    public Result execute( ExpressionExperiment expressionExperiment ) {

        ExpressionExperiment persistedExpressionExperiment = expressionExperimentService.create( expressionExperiment );
        Long id = persistedExpressionExperiment.getId();
        counter++;
        Result result = new Result();
        result.setTaskID( counter );
        result.setAnswer( id );

        return result;

    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}

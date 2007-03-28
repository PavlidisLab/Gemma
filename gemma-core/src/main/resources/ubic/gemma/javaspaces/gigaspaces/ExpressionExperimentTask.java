package ubic.gemma.javaspaces.gigaspaces;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface ExpressionExperimentTask {
    
    public Result execute(Foo foo);
    
    public Result execute(ExpressionExperiment expressionExperiment);

}

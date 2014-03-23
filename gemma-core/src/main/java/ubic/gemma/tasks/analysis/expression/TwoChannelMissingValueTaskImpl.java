package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.AbstractTask;

/**
 * Space task for computing two channel missing values.
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class TwoChannelMissingValueTaskImpl extends AbstractTask<TaskResult, TwoChannelMissingValueTaskCommand>
        implements TwoChannelMissingValueTask {

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValues;

    @Override
    public TaskResult execute() {
        ExpressionExperiment ee = taskCommand.getExpressionExperiment();

        Collection<RawExpressionDataVector> missingValueVectors = twoChannelMissingValues.computeMissingValues( ee,
                taskCommand.getS2n(), taskCommand.getExtraMissingValueIndicators() );

        TaskResult result = new TaskResult( taskCommand, missingValueVectors.size() );

        return result;
    }
}

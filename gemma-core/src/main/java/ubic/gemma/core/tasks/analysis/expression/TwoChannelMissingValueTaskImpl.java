package ubic.gemma.core.tasks.analysis.expression;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.core.tasks.AbstractTask;

/**
 * Space task for computing two channel missing values.
 * 
 * @author paul
 *
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
        System.out.println("MVs: " + missingValueVectors.size());

        return new TaskResult( taskCommand, missingValueVectors.size() );
    }
}

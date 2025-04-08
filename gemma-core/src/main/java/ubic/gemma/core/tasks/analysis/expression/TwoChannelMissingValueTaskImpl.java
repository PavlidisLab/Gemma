package ubic.gemma.core.tasks.analysis.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * Space task for computing two channel missing values.
 * 
 * @author paul
 *
 */
@Component
@Scope("prototype")
public class TwoChannelMissingValueTaskImpl extends AbstractTask<TwoChannelMissingValueTaskCommand>
        implements TwoChannelMissingValueTask {

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValues;

    @Override
    public TaskResult call() {
        ExpressionExperiment ee = getTaskCommand().getExpressionExperiment();

        Collection<RawExpressionDataVector> missingValueVectors = twoChannelMissingValues.computeMissingValues( ee,
                getTaskCommand().getS2n(), getTaskCommand().getExtraMissingValueIndicators() );
        System.out.println("MVs: " + missingValueVectors.size());

        return newTaskResult( missingValueVectors.size() );
    }
}

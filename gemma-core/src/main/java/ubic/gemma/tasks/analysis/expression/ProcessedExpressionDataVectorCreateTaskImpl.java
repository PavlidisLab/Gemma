package ubic.gemma.tasks.analysis.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

@Component
@Scope("prototype")
public class ProcessedExpressionDataVectorCreateTaskImpl implements ProcessedExpressionDataVectorCreateTask {

    @Autowired private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;
    @Autowired private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired private SampleCoexpressionMatrixService coexpressionMatrixService;

    private ProcessedExpressionDataVectorCreateTaskCommand command;

    @Override
    public void setCommand(ProcessedExpressionDataVectorCreateTaskCommand command) {
        this.command = command;
    }

    /*
             * (non-Javadoc)
             *
             * @see
             * ubic.gemma.grid.javaspaces.task.analysis.preprocess.ProcessedExpressionDataVectorCreateTask#execute(ubic.gemma
             * .grid .javaspaces.analysis.preprocess.SpacesProcessedExpressionDataVectorCreateCommand)
             */
    @Override
    public TaskResult execute() {

        ExpressionExperiment ee = command.getExpressionExperiment();
        Collection<ProcessedExpressionDataVector> processedVectors = null;
        if ( command.isCorrelationMatrixOnly() ) {
            // only create them if necessary. This is sort of stupid, it's just so I didn't have to create a whole other
            // task for the correlation matrix computation.
            processedVectors = processedExpressionDataVectorService.getProcessedDataVectors( ee );
            if ( processedVectors.isEmpty() ) {
                processedVectors = processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
            }
        } else {
            processedVectors = processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
        }

        coexpressionMatrixService.create( ee, processedVectors );

        TaskResult result = new TaskResult( command, processedVectors.size() );
        return result;
    }

}

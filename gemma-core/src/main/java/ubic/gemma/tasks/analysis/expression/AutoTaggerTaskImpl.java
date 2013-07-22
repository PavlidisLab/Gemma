package ubic.gemma.tasks.analysis.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator;
import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotatorImpl;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.AbstractTask;

import java.util.Collection;

/**
 * Run the semantic tagger.
 *
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class AutoTaggerTaskImpl extends AbstractTask<TaskResult, AutoTaggerTaskCommand> implements AutoTaggerTask {

    @Autowired private ExpressionExperimentAnnotator expressionExperimentAnnotator;
    @Autowired private ExpressionExperimentService expressionExperimentService;

    @Override
    public TaskResult execute() {
        if ( !ExpressionExperimentAnnotatorImpl.ready() ) {
            throw new RuntimeException( "Sorry, the auto-tagger is not available." );
        }

        ExpressionExperiment ee = expressionExperimentService.load( taskCommand.getEntityId() );

        if ( ee == null ) {
            throw new IllegalArgumentException( "No experiment with id=" + taskCommand.getEntityId() + " could be loaded" );
        }

        Collection<Characteristic> characteristics = expressionExperimentAnnotator.annotate( ee, true );

        return new TaskResult( taskCommand, characteristics );
    }
}

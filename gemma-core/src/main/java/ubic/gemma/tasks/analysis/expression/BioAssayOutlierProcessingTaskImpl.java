package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.service.SampleRemoveService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;

/**
 * Handle 'flagging' a sample as an outlier. The sample will not be used in analyses.
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class BioAssayOutlierProcessingTaskImpl implements BioAssayOutlierProcessingTask {

    @Autowired
    BioAssayService bioAssayService;
    @Autowired
    SampleRemoveService sampleRemoveService;

    private BioAssayOutlierProcessingTaskCommand command;

    @Override
    public void setCommand( TaskCommand command ) {
        assert command instanceof BioAssayOutlierProcessingTaskCommand;
        this.command = ( BioAssayOutlierProcessingTaskCommand ) command;
    }

    @Override
    public TaskResult execute() {
        Collection<BioAssay> bioAssays = bioAssayService.load( command.getBioAssayIds() );
        if ( bioAssays.isEmpty() ) {
            throw new RuntimeException( "did not find bioAssays" );
        }

        if ( command.isRevert() ) {
            sampleRemoveService.unmarkAsMissing( bioAssays );
        } else {
            sampleRemoveService.markAsMissing( bioAssays );
        }
        return new TaskResult( command, bioAssays );
    }
}

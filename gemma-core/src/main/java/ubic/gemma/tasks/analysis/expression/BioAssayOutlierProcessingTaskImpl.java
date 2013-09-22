package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.service.SampleRemoveService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.tasks.AbstractTask;

/**
 * Handle 'flagging' a sample as an outlier. The sample will not be used in analyses.
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class BioAssayOutlierProcessingTaskImpl extends AbstractTask<TaskResult, BioAssayOutlierProcessingTaskCommand>
        implements BioAssayOutlierProcessingTask {

    @Autowired
    BioAssayService bioAssayService;

    @Autowired
    SampleRemoveService sampleRemoveService;

    @Override
    public TaskResult execute() {
        Collection<BioAssay> bioAssays = bioAssayService.load( taskCommand.getBioAssayIds() );
        if ( bioAssays.isEmpty() ) {
            throw new RuntimeException( "did not find bioAssays" );
        }

        if ( taskCommand.isRevert() ) {
            sampleRemoveService.unmarkAsMissing( bioAssays );
        } else {
            sampleRemoveService.markAsMissing( bioAssays );
        }
        return new TaskResult( taskCommand, bioAssays );
    }
}

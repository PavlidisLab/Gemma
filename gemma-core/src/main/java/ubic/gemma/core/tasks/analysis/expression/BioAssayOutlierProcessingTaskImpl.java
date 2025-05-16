package ubic.gemma.core.tasks.analysis.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.service.OutlierFlaggingService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;

import java.util.Collection;
import java.util.HashSet;

/**
 * Handle 'flagging' a sample as an outlier. The sample will not be used in analyses.
 *
 * @author paul
 */
@Component
@Scope("prototype")
public class BioAssayOutlierProcessingTaskImpl extends AbstractTask<BioAssayOutlierProcessingTaskCommand>
        implements BioAssayOutlierProcessingTask {

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private OutlierFlaggingService sampleRemoveService;

    @Override
    public TaskResult call() {
        Collection<BioAssay> bioAssays = bioAssayService.load( getTaskCommand().getBioAssayIds() );
        if ( bioAssays.isEmpty() ) {
            throw new RuntimeException( "Could not locate the bioassays" );
        }

        if ( getTaskCommand().isRevert() ) {
            sampleRemoveService.unmarkAsMissing( bioAssays );
        } else {
            sampleRemoveService.markAsMissing( bioAssays );
        }
        bioAssays = bioAssayService.thaw( bioAssays );

        HashSet<BioAssayValueObject> flagged = new HashSet<>();
        for ( BioAssay ba : bioAssays ) {
            flagged.add( new BioAssayValueObject( ba, false ) );
        }

        return newTaskResult( flagged );
    }
}

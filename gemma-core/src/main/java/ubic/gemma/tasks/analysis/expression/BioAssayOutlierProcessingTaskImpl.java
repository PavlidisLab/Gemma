package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.service.OutlierFlaggingService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
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
    private BioAssayService bioAssayService;

    @Autowired
    private OutlierFlaggingService sampleRemoveService;

    @Override
    public TaskResult execute() {
        Collection<BioAssay> bioAssays = bioAssayService.load( taskCommand.getBioAssayIds() );
        if ( bioAssays.isEmpty() ) {
            throw new RuntimeException( "Could not locate the bioassays" );
        }

        if ( taskCommand.isRevert() ) {
            sampleRemoveService.unmarkAsMissing( bioAssays );
        } else {
            sampleRemoveService.markAsMissing( bioAssays );
        }
        bioAssayService.thaw( bioAssays );

        Collection<BioAssayValueObject> flagged = new HashSet<>();
        for ( BioAssay ba : bioAssays ) {
            flagged.add( new BioAssayValueObject( ba ) );
        }

        return new TaskResult( taskCommand, flagged );
    }
}

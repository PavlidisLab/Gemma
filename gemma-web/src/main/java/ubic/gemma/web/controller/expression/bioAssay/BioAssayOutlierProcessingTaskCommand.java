package ubic.gemma.web.controller.expression.bioAssay;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.tasks.analysis.expression.BioAssayOutlierProcessingTask;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 03/01/13
 * Time: 5:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class BioAssayOutlierProcessingTaskCommand extends TaskCommand {
    public BioAssayOutlierProcessingTaskCommand( Long id ) {
        this.setEntityId( id );
    }

    @Override
    public Class getTaskClass() {
        return BioAssayOutlierProcessingTask.class;
    }
}

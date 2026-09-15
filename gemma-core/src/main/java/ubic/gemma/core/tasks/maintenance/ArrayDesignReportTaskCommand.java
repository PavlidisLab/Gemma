package ubic.gemma.core.tasks.maintenance;

import ubic.gemma.core.job.Task;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Regenerate the cached on-disk platform reports, either for every platform or for a single one.
 * <p>
 * The platform counterpart of {@link ExpressionExperimentReportTaskCommand}. Needed because these
 * counts are far too expensive to compute per request — counting distinct genes for one large
 * platform measures ~1.7s against production — and the Quartz trigger that would refresh them
 * monthly ({@code SchedulerConfig.arrayDesignReportTrigger}) is gated on the {@code scheduler}
 * profile, which production nodes do not run.
 *
 * @author paul
 */
public class ArrayDesignReportTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private ArrayDesign arrayDesign = null;
    private boolean all = false;

    public ArrayDesignReportTaskCommand( Boolean all ) {
        super();
        this.all = all;
    }

    public ArrayDesignReportTaskCommand( ArrayDesign arrayDesign ) {
        super();
        this.arrayDesign = arrayDesign;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return ArrayDesignReportTask.class;
    }

    public boolean doAll() {
        return all;
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public void setAll( Boolean all ) {
        this.all = all;
    }
}

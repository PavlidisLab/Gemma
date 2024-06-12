package ubic.gemma.core.tasks.visualization;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA. User: anton Date: 22/01/13 Time: 10:10 AM To change this template use File | Settings |
 * File Templates.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class DifferentialExpressionSearchTaskCommand extends TaskCommand {

    private static final long serialVersionUID = -8510536003059837349L;
    private final Collection<GeneValueObject> geneGroup;
    private final Collection<ExpressionExperimentDetailsValueObject> experimentGroup;
    private final String geneGroupName;
    private final String experimentGroupName;

    public DifferentialExpressionSearchTaskCommand( Collection<GeneValueObject> geneGroup,
            Collection<ExpressionExperimentDetailsValueObject> experimentGroup, String geneGroupName,
            String experimentGroupName ) {

        assert !geneGroup.isEmpty();
        assert !experimentGroup.isEmpty();

        this.geneGroup = geneGroup;
        this.experimentGroup = experimentGroup;
        this.geneGroupName = geneGroupName;
        this.experimentGroupName = experimentGroupName;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return DifferentialExpressionSearchTask.class;
    }

    public Collection<GeneValueObject> getGeneGroup() {
        return geneGroup;
    }

    public Collection<ExpressionExperimentDetailsValueObject> getExperimentGroup() {
        return experimentGroup;
    }

    public String getGeneGroupName() {
        return geneGroupName;
    }

    public String getExperimentGroupName() {
        return experimentGroupName;
    }
}

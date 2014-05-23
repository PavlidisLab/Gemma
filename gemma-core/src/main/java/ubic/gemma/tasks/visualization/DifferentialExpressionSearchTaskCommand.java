package ubic.gemma.tasks.visualization;

import java.util.Collection;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.tasks.Task;

/**
 * Created with IntelliJ IDEA. User: anton Date: 22/01/13 Time: 10:10 AM To change this template use File | Settings |
 * File Templates.
 */
public class DifferentialExpressionSearchTaskCommand extends TaskCommand {

    private final Collection<GeneValueObject> geneGroup;
    private final Collection<ExpressionExperimentValueObject> experimentGroup;
    private final String geneGroupName;
    private final String experimentGroupName;

    /**
     * @param geneGroups - the sets of genes to query
     * @param experimentGroups - the sets of experiments to query
     * @param geneGroupNames - metadata
     * @param experimentGroupNames
     */
    public DifferentialExpressionSearchTaskCommand( Collection<GeneValueObject> geneGroup,
            Collection<ExpressionExperimentValueObject> experimentGroup, String geneGroupName,
            String experimentGroupName ) {

        assert !geneGroup.isEmpty();
        assert !experimentGroup.isEmpty();

        this.geneGroup = geneGroup;
        this.experimentGroup = experimentGroup;
        this.geneGroupName = geneGroupName;
        this.experimentGroupName = experimentGroupName;
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return DifferentialExpressionSearchTask.class;
    }

    public Collection<GeneValueObject> getGeneGroup() {
        return geneGroup;
    }

    public Collection<ExpressionExperimentValueObject> getExperimentGroup() {
        return experimentGroup;
    }

    public String getGeneGroupName() {
        return geneGroupName;
    }

    public String getExperimentGroupName() {
        return experimentGroupName;
    }
}

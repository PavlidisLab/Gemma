package ubic.gemma.tasks.visualization;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 22/01/13
 * Time: 10:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class DifferentialExpressionSearchTaskCommand extends TaskCommand {

    private final List<List<Gene>> geneGroups;
    private final List<Collection<ExpressionExperiment>> experimentGroups;
    private final List<String> geneGroupNames;
    private final List<String> experimentGroupNames;

    /**
     * @param geneGroups - the sets of genes to query
     * @param experimentGroups - the sets of experiments to query
     * @param geneGroupNames - metadata
     * @param experimentGroupNames
     */
    public DifferentialExpressionSearchTaskCommand( List<List<Gene>> geneGroups,
                                             List<Collection<ExpressionExperiment>> experimentGroups, List<String> geneGroupNames,
                                             List<String> experimentGroupNames ) {

        assert !geneGroups.isEmpty() && !geneGroups.get( 0 ).isEmpty();
        assert !experimentGroups.isEmpty() && !experimentGroups.get( 0 ).isEmpty();
        assert geneGroups.size() == geneGroupNames.size();
        assert experimentGroups.size() == experimentGroupNames.size();

        this.geneGroups = geneGroups;
        this.experimentGroups = experimentGroups;
        this.geneGroupNames = geneGroupNames;
        this.experimentGroupNames = experimentGroupNames;
    }

    @Override
    public Class getTaskClass() {
        return DifferentialExpressionSearchTask.class;
    }

    public List<List<Gene>> getGeneGroups() {
        return geneGroups;
    }

    public List<Collection<ExpressionExperiment>> getExperimentGroups() {
        return experimentGroups;
    }

    public List<String> getGeneGroupNames() {
        return geneGroupNames;
    }

    public List<String> getExperimentGroupNames() {
        return experimentGroupNames;
    }
}

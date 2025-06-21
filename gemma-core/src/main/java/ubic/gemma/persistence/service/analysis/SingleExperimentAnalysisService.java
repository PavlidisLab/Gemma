package ubic.gemma.persistence.service.analysis;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.SecurableBaseImmutableService;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for analysis service manipulating single experiments.
 * @author poirigui
 */
public interface SingleExperimentAnalysisService<T extends SingleExperimentAnalysis> extends AnalysisService<T>, SecurableBaseImmutableService<T> {

    /**
     * @param experimentAnalyzed experiment analyzed
     * @param includeSubSets     if the experiment is a {@link ubic.gemma.model.expression.experiment.ExpressionExperiment},
     *                           include analyses for its {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet}
     * @return find all the analyses that involved the given investigation
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByExperiment( BioAssaySet experimentAnalyzed, boolean includeSubSets );

    /**
     * @param investigations investigations
     * @param includeSubSets if the experiment is a {@link ubic.gemma.model.expression.experiment.ExpressionExperiment},
     *                       include analyses for its {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet}
     * @return Given a collection of investigations returns a Map of Analysis --&gt; collection of Investigations
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ", "AFTER_ACL_MAP_READ" })
    Map<BioAssaySet, Collection<T>> findByExperiments( Collection<BioAssaySet> investigations, boolean includeSubSets );

    /**
     * Not secured: for internal use only
     *
     * @param experimentAnalyzedIds starting list of experiment or subset IDs
     * @param includeSubSets        retain IDs of experiments that have subset analyses
     * @return the ones which have an analysis
     */
    Collection<Long> getExperimentsWithAnalysis( Collection<Long> experimentAnalyzedIds, boolean includeSubSets );

    /**
     * Not secured: for internal use only
     *
     * @param taxon taxon
     * @return ids of bioassaysets from the given taxon that have a coexpression analysis
     */
    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    /**
     * Removes all analyses for the given experiment
     *
     * @param ee             the expriment to remove all analyses for
     * @param includeSubSets if the experiment is a {@link ubic.gemma.model.expression.experiment.ExpressionExperiment},
     *                       also delete analyses of its {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet}.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeForExperiment( BioAssaySet ee, boolean includeSubSets );
}

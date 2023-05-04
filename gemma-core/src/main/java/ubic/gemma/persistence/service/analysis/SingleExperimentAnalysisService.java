package ubic.gemma.persistence.service.analysis;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for analysis service manipulating single experiments.
 * @author poirigiu
 */
public interface SingleExperimentAnalysisService<T extends SingleExperimentAnalysis> extends AnalysisService<T> {

    /**
     * Removes all analyses for the given experiment
     * @param ee the expriment to remove all analyses for
     */
    @Secured({ "GROUP_USER", "ACL_ANALYSIS_EDIT" })
    void removeForExperiment( BioAssaySet ee );

    /**
     * @param investigation investigation
     * @return find all the analyses that involved the given investigation
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByExperiment( BioAssaySet investigation );

    /**
     * @param investigations investigations
     * @return Given a collection of investigations returns a Map of Analysis --&gt; collection of Investigations
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ", "AFTER_ACL_MAP_READ" })
    Map<BioAssaySet, Collection<T>> findByExperiments( Collection<BioAssaySet> investigations );

    /**
     * Not secured: for internal use only
     *
     * @param idsToFilter starting list of bioassayset ids.
     * @return the ones which have a coexpression analysis.
     */
    Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    /**
     * Not secured: for internal use only
     *
     * @param taxon taxon
     * @return ids of bioassaysets from the given taxon that have a coexpression analysis
     */
    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );
}

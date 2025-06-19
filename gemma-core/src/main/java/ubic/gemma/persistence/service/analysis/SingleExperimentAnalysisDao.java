package ubic.gemma.persistence.service.analysis;

import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;

import java.util.Collection;
import java.util.Map;

/**
 * @author poirigui
 */
public interface SingleExperimentAnalysisDao<T extends SingleExperimentAnalysis> extends AnalysisDao<T> {

    /**
     * Indicate if there is an analysis (or at least one) for the given experiment.
     * @param includeSubSets include subsets of the specified experiments
     */
    boolean existsByExperiment( BioAssaySet ee, boolean includeSubSets );

    /**
     * Find by associated experiment via {@link SingleExperimentAnalysis#getExperimentAnalyzed()}.
     * @param includeSubSets include subsets of the specified experiments
     */
    Collection<T> findByExperiment( BioAssaySet experiment, boolean includeSubSets );

    /**
     * Given a collection of experiments returns a Map of Analysis --&gt; collection of Experiments
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     *
     * @param experiments    experiments
     * @param includeSubSets include subsets of the specified experiments
     * @return map to analyses
     */
    Map<BioAssaySet, Collection<T>> findByExperiments( Collection<? extends BioAssaySet> experiments, boolean includeSubSets );
}

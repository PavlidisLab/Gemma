/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * @see DifferentialExpressionAnalysis
 */
public interface DifferentialExpressionAnalysisDao extends ubic.gemma.persistence.service.analysis.AnalysisDao<DifferentialExpressionAnalysis> {

    /**
     * Find by associated {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}.
     *
     * @param includeSubSets include subsets of the specified experiment
     */
    Collection<DifferentialExpressionAnalysis> findByExperiment( ExpressionExperiment experiment, boolean includeSubSets );

    /**
     * Find by associated experiment via {@link SingleExperimentAnalysis#getExperimentAnalyzed()}.
     */
    Collection<DifferentialExpressionAnalysis> findByExperimentAnalyzed( BioAssaySet experimentAnalyzed );

    /**
     * Find by associated {@link ExpressionExperiment}s.
     *
     * @param includeSubSets include subsets of the specified experiments
     * @return map to analyses grouped by source experiment
     */
    Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> findByExperiments( Collection<ExpressionExperiment> experiments, boolean includeSubSets );

    /**
     * Retrieve analysis for the given experiment or experiment subset IDs.
     * <p>
     * Results are grouped by source experiment.
     *
     * @param experimentIds   experiment IDs (no subset allowed!)
     * @param includeSubSets  include analyses for subsets of the requested experiment IDs
     * @param arrayDesignUsed a map of experiments or subset ID to array design IDs used
     * @param ee2fv           a map of experiments or subset ID to factor values used
     * @return map to analyses grouped by source experiment
     */
    Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> findByExperimentIds(
            Collection<Long> experimentIds, boolean includeSubSets,
            @Nullable Map<Long, Collection<Long>> arrayDesignUsed,
            @Nullable Map<Long, Collection<FactorValue>> ee2fv );

    Collection<DifferentialExpressionAnalysis> findByName( String name );

    /**
     * Retrieve analyses associated with the factor, either through the subset factor or as factors for result sets.
     */
    Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    /**
     * @see #findByFactor(ExperimentalFactor)
     */
    Collection<DifferentialExpressionAnalysis> findByFactors( Collection<ExperimentalFactor> experimentalFactors );

    /**
     * Find an analysis for the given experiment and identifier.
     */
    @Nullable
    DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment experimentAnalyzed, boolean includeSubSets, Long analysisId );

    /**
     * Find experiments (or subsets) that have differential expression analyses for the given gene.
     */
    Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene );

    Collection<Long> getExperimentsWithAnalysis( Collection<Long> eeIds, boolean includeSubSets );
}

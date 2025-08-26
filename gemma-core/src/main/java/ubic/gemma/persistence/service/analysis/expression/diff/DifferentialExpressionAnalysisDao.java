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
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see DifferentialExpressionAnalysis
 */
public interface DifferentialExpressionAnalysisDao extends ubic.gemma.persistence.service.analysis.AnalysisDao<DifferentialExpressionAnalysis> {

    /**
     * Find by associated experiment via {@link SingleExperimentAnalysis#getExperimentAnalyzed()}.
     * @param includeSubSets include subsets of the specified experiments
     */
    Collection<DifferentialExpressionAnalysis> findByExperimentAnalyzed( ExpressionExperiment experiment, boolean includeSubSets );

    Collection<DifferentialExpressionAnalysis> findByExperimentAnalyzed( BioAssaySet experimentAnalyzed );

    /**
     * Given a collection of experiments returns a Map of Analysis --&gt; collection of Experiments
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     *
     * @param experiments    experiments
     * @param includeSubSets include subsets of the specified experiments
     * @return map to analyses
     */
    Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> findByExperimentsAnalyzed( Collection<ExpressionExperiment> experiments, boolean includeSubSets );

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
    DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment experimentAnalyzed, Long analysisId, boolean includeSubSets );

    Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentAnalyzedId( Collection<Long> experimentAnalyzedId );

    Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene );

    Collection<Long> getExperimentsWithAnalysis( Collection<Long> eeIds, boolean includeSubSets );

    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    Map<Long, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds(
            Collection<Long> expressionExperimentIds, int offset, int limit, boolean includeSubSets );
}

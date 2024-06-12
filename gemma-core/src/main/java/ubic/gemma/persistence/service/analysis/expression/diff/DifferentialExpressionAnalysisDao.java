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

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see DifferentialExpressionAnalysis
 */
public interface DifferentialExpressionAnalysisDao extends SingleExperimentAnalysisDao<DifferentialExpressionAnalysis> {

    /**
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     */
    Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     */
    Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

    /**
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     */
    Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold );

    Collection<DifferentialExpressionAnalysis> find( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold );

    /**
     * Retrieve analyses associated with the factor, either through the subset factor or as factors for result sets.
     */
    Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    /**
     * @see #findByFactor(ExperimentalFactor)
     */
    Collection<DifferentialExpressionAnalysis> findByFactors( Collection<ExperimentalFactor> experimentalFactors );

    Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentIds( Collection<Long> investigationIds );

    Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene );

    Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments );

    Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    Map<Long, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds(
            Collection<Long> expressionExperimentIds, int offset, int limit );
}

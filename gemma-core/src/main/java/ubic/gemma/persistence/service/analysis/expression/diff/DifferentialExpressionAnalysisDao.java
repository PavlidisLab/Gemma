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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see DifferentialExpressionAnalysis
 */
public interface DifferentialExpressionAnalysisDao extends SingleExperimentAnalysisDao<DifferentialExpressionAnalysis> {

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
    DifferentialExpressionAnalysis findByExperimentAnalyzedAndId( BioAssaySet experimentAnalyzed, Long analysisId, boolean includeSubSets );

    Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentAnalyzedId( Collection<Long> experimentAnalyzedId );

    Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene );

    Collection<Long> getExperimentsWithAnalysis( Collection<Long> experimentAnalyzedIds, boolean includeSubSets );

    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    Map<Long, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds(
            Collection<Long> expressionExperimentIds, int offset, int limit, boolean includeSubSets );
}

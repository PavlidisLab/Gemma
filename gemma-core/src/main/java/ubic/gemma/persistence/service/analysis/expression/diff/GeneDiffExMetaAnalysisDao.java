/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.Map;

/**
 * @author Paul
 */
public interface GeneDiffExMetaAnalysisDao extends BaseDao<GeneDifferentialExpressionMetaAnalysis> {

    /**
     * @param analysisId id
     * @return a collection of included result set info value objects using the given meta-analysis id
     */
    Collection<IncludedResultSetInfoValueObject> findIncludedResultSetsInfoById( long analysisId );

    /**
     * @param metaAnalysisIds ids
     * @return a collection of summary value objects using the given ids of meta-analyses
     */
    Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMetaAnalyses(
            Collection<Long> metaAnalysisIds );

    /**
     * @param analysisId id
     * @return a collection of result value objects using the given meta-analysis id
     */
    Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> findResultsById( long analysisId );

    Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    GeneDifferentialExpressionMetaAnalysisResult loadResult( Long idResult );

    GeneDifferentialExpressionMetaAnalysis loadWithResultId( Long idResult );

    Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigation( Investigation investigation );

    Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations );

    Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name );

    Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon );

    void removeForExperiment( ExpressionExperiment ee );
}

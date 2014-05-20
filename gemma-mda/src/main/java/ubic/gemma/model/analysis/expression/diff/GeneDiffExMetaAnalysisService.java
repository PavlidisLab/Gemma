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

package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.BaseValueObject;
import ubic.gemma.model.analysis.AnalysisService;
import ubic.gemma.model.genome.Taxon;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public interface GeneDiffExMetaAnalysisService extends AnalysisService<GeneDifferentialExpressionMetaAnalysis> {

    @Secured({ "GROUP_USER" })
    public GeneDifferentialExpressionMetaAnalysis create( GeneDifferentialExpressionMetaAnalysis analysis );

    @Secured({ "GROUP_USER" })
    public BaseValueObject delete( Long id );

    /**
     * 
     */
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByParentTaxon( Taxon taxon );

    /**
     * 
     */
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon );

    /**
     * @param analysisId
     * @return
     */
    public Collection<GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject> findIncludedResultSetsInfoById(
            long analysisId );

    /**
     * @param metaAnalysisIds
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMetaAnalyses(
            Collection<Long> metaAnalysisIds );

    /**
     * @param analysisId
     * @return
     */
    public Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> findResultsById( long analysisId );

    /**
     * @param idResult
     * @return
     */
    public GeneDifferentialExpressionMetaAnalysisResult loadResult( Long idResult );

    /**
     * @param idResult
     * @return
     */
    public GeneDifferentialExpressionMetaAnalysis loadWithResultId( Long idResult );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( GeneDifferentialExpressionMetaAnalysis analysis );

}

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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResultValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.IncludedResultSetInfoValueObject;
import ubic.gemma.model.common.BaseValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.AnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.SecurableBaseService;

import java.util.Collection;

/**
 * @author Paul
 */
public interface GeneDiffExMetaAnalysisService extends AnalysisService<GeneDifferentialExpressionMetaAnalysis>, SecurableBaseService<GeneDifferentialExpressionMetaAnalysis> {

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon );

    Collection<IncludedResultSetInfoValueObject> findIncludedResultSetsInfoById( long analysisId );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> findMetaAnalyses(
            Collection<Long> metaAnalysisIds );

    Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> findResultsById( long analysisId );

    @Secured({ "GROUP_ADMIN" })
    BaseValueObject delete( Long id );
}

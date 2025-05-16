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

package ubic.gemma.core.analysis.expression.diff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.GeneDiffExMetaAnalysisService;

import java.util.Collection;
import java.util.HashSet;

/**
 * * @author frances
 */
@Component
public class GeneDiffExMetaAnalysisHelperServiceImpl implements GeneDiffExMetaAnalysisHelperService {

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    @Override
    public GeneDifferentialExpressionMetaAnalysisDetailValueObject convertToValueObject(
            GeneDifferentialExpressionMetaAnalysis metaAnalysis ) {
        if ( metaAnalysis == null ) {
            return null;
        }

        GeneDifferentialExpressionMetaAnalysisDetailValueObject analysisVO = new GeneDifferentialExpressionMetaAnalysisDetailValueObject();

        analysisVO.setNumGenesAnalyzed( metaAnalysis.getNumGenesAnalyzed() );

        Collection<ExpressionAnalysisResultSet> resultSetsIncluded = metaAnalysis.getResultSetsIncluded();

        analysisVO.setIncludedResultSetsInfo(
                new HashSet<IncludedResultSetInfoValueObject>( resultSetsIncluded.size() ) );

        for ( ExpressionAnalysisResultSet resultSetIncluded : resultSetsIncluded ) {
            IncludedResultSetInfoValueObject includedResultSetInfo = new IncludedResultSetInfoValueObject();
            includedResultSetInfo.setExperimentId( resultSetIncluded.getAnalysis().getExperimentAnalyzed().getId() );
            includedResultSetInfo.setAnalysisId( resultSetIncluded.getAnalysis().getId() );
            includedResultSetInfo.setResultSetId( resultSetIncluded.getId() );
            analysisVO.getIncludedResultSetsInfo().add( includedResultSetInfo );
        }

        analysisVO.setResults( new HashSet<GeneDifferentialExpressionMetaAnalysisResultValueObject>(
                metaAnalysis.getResults().size() ) );

        for ( GeneDifferentialExpressionMetaAnalysisResult result : metaAnalysis.getResults() ) {
            GeneDifferentialExpressionMetaAnalysisResultValueObject resultVO = new GeneDifferentialExpressionMetaAnalysisResultValueObject();

            GeneValueObject gene = new GeneValueObject( result.getGene() );
            resultVO.setGeneSymbol( gene.getOfficialSymbol() );
            resultVO.setGeneName( gene.getOfficialName() );
            resultVO.setMetaPvalue( result.getMetaPvalue() );
            resultVO.setMetaQvalue( result.getMetaQvalue() );
            resultVO.setUpperTail( result.getUpperTail() );

            analysisVO.getResults().add( resultVO );
        }

        return analysisVO;
    }

    @Override
    public GeneDifferentialExpressionMetaAnalysisDetailValueObject findDetailMetaAnalysisById( long analysisId ) {
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = this.geneDiffExMetaAnalysisService.load( analysisId );

        final GeneDifferentialExpressionMetaAnalysisDetailValueObject analysisVO;

        if ( metaAnalysis == null ) {
            analysisVO = null;
        } else {
            analysisVO = new GeneDifferentialExpressionMetaAnalysisDetailValueObject();
            analysisVO.setIncludedResultSetsInfo(
                    this.geneDiffExMetaAnalysisService.findIncludedResultSetsInfoById( analysisId ) );
            analysisVO.setResults( this.geneDiffExMetaAnalysisService.findResultsById( analysisId ) );
        }

        return analysisVO;
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> loadAllMetaAnalyses() {
        Collection<GeneDifferentialExpressionMetaAnalysis> metaAnalyses = this.geneDiffExMetaAnalysisService.loadAll();

        Collection<Long> metaAnalysisIds = new HashSet<>( metaAnalyses.size() );
        for ( GeneDifferentialExpressionMetaAnalysis metaAnalysis : metaAnalyses ) {
            metaAnalysisIds.add( metaAnalysis.getId() );
        }

        Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> vos = this.geneDiffExMetaAnalysisService
                .findMetaAnalyses( metaAnalysisIds );
        for ( GeneDifferentialExpressionMetaAnalysisSummaryValueObject vo : vos ) {

            // Find meta-analysis so that its security settings can be copied to value object.
            // for ( GeneDifferentialExpressionMetaAnalysis metaAnalysis : metaAnalyses ) {
            // if ( vo.getId().equals( metaAnalysis.getId() ) ) {
            // boolean isEditable = false;
            //
            // // try {
            // // isEditable = this.securityService.isEditable( metaAnalysis );
            // // } catch ( AccessDeniedException e ) {
            // // // nothing to do
            // // }
            // //
            // // // these should get set automatically by the security interceptor.
            // // vo.setEditable( isEditable );
            // // vo.setOwnedByCurrentUser( this.securityService.isOwnedByCurrentUser( metaAnalysis ) );
            // // vo.setPublic( this.securityService.isPublic( metaAnalysis ) );
            // // vo.setShared( this.securityService.isShared( metaAnalysis ) );
            // break;
            // }
            // }
        }

        return vos;
    }
}

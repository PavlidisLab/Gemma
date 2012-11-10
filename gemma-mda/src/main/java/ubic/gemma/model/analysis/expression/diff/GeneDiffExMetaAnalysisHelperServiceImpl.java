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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject.IncludedResultSetDetail;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * TODO Document Me
 * 
 * @author frances
 * @version $Id$
 */
@Service
public class GeneDiffExMetaAnalysisHelperServiceImpl implements GeneDiffExMetaAnalysisHelperService {

    @Autowired
    private GeneDiffExMetaAnalysisDao geneDiffExMetaAnalysisDao;

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    
    @Override
    public GeneDifferentialExpressionMetaAnalysisDetailValueObject getMetaAnalysis(Long id) {
		GeneDifferentialExpressionMetaAnalysis metaAnalysis = this.geneDiffExMetaAnalysisService.load( id );

    	return convertToValueObject(metaAnalysis);
    }
    
    @Override
    public GeneDifferentialExpressionMetaAnalysisDetailValueObject convertToValueObject(GeneDifferentialExpressionMetaAnalysis metaAnalysis) {
		// TODO: Should throw exception if the meta-analysis is null.
		if (metaAnalysis == null) {
			return null;
		}
		
		GeneDifferentialExpressionMetaAnalysisDetailValueObject analysisVO = new GeneDifferentialExpressionMetaAnalysisDetailValueObject();
		
		analysisVO.setId(metaAnalysis.getId());
		analysisVO.setName(metaAnalysis.getName());
		analysisVO.setDescription(metaAnalysis.getDescription());
		analysisVO.setNumGenesAnalyzed(metaAnalysis.getNumGenesAnalyzed());
		analysisVO.setQvalueThresholdForStorage(metaAnalysis.getQvalueThresholdForStorage());
		
		Collection<ExpressionAnalysisResultSet> resultSetsIncluded = metaAnalysis.getResultSetsIncluded();
		
		analysisVO.setIncludedResultSetDetails(new HashSet<IncludedResultSetDetail>(resultSetsIncluded.size()));
		
		for (ExpressionAnalysisResultSet resultSetIncluded: resultSetsIncluded) {
			IncludedResultSetDetail includedResultSetDetail = analysisVO.new IncludedResultSetDetail();
			includedResultSetDetail.setExperimentId(resultSetIncluded.getAnalysis().getExperimentAnalyzed().getId());
			includedResultSetDetail.setAnalysisId(resultSetIncluded.getAnalysis().getId());
			includedResultSetDetail.setResultSetId(resultSetIncluded.getId());
			analysisVO.getIncludedResultSetDetails().add(includedResultSetDetail);
		}
		
		analysisVO.setResults(new HashSet<GeneDifferentialExpressionMetaAnalysisResultValueObject>());
		for (GeneDifferentialExpressionMetaAnalysisResult result : metaAnalysis.getResults()) {
			GeneDifferentialExpressionMetaAnalysisResultValueObject resultVO = new GeneDifferentialExpressionMetaAnalysisResultValueObject();
			
			resultVO.setId(result.getId());
			resultVO.setGene(new GeneValueObject(result.getGene()));
			resultVO.setMeanLogFoldChange(result.getMeanLogFoldChange());
			resultVO.setMetaPvalue(result.getMetaPvalue());
			resultVO.setMetaPvalueRank(result.getMetaPvalueRank());
			resultVO.setMetaQvalue(result.getMetaQvalue());
			resultVO.setNumResultsUsed(result.getResultsUsed().size());
// TODO: can be removed: it is even slower if I have this:			
//resultVO.setResultsUsedCount(this.geneDiffExMetaAnalysisDao.getNumResultsUsed(result));
			resultVO.setUpperTail(result.getUpperTail());
			
			
			analysisVO.getResults().add(resultVO);
		}
		
		// Sort by p value.
		List<GeneDifferentialExpressionMetaAnalysisResultValueObject> sortedResults = new ArrayList<GeneDifferentialExpressionMetaAnalysisResultValueObject>(analysisVO.getResults());
		Collections.sort( sortedResults, new Comparator<GeneDifferentialExpressionMetaAnalysisResultValueObject>(){
	        public int compare(GeneDifferentialExpressionMetaAnalysisResultValueObject result1, GeneDifferentialExpressionMetaAnalysisResultValueObject result2) {
	            return result1.getMetaPvalue().compareTo(result2.getMetaPvalue());
	        }} );
		analysisVO.setResults((sortedResults));
		
		return analysisVO;
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> getMyMetaAnalyses() {
    	Collection<GeneDifferentialExpressionMetaAnalysis> myAnalyses = this.geneDiffExMetaAnalysisService.loadMyAnalyses();
    	
    	Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> analysisVOs = new HashSet<GeneDifferentialExpressionMetaAnalysisSummaryValueObject>(myAnalyses.size());
    	
    	for (GeneDifferentialExpressionMetaAnalysis analysis: myAnalyses) {
        	GeneDifferentialExpressionMetaAnalysisSummaryValueObject summaryVO = new GeneDifferentialExpressionMetaAnalysisSummaryValueObject();
        	
			summaryVO.setId(analysis.getId());
			summaryVO.setName(analysis.getName());
			summaryVO.setDescription(analysis.getDescription());
			summaryVO.setNumGenesAnalyzed(analysis.getNumGenesAnalyzed());
			summaryVO.setNumResultSetsIncluded(this.geneDiffExMetaAnalysisDao.getNumResultSetsIncluded(analysis));
			summaryVO.setNumResults(this.geneDiffExMetaAnalysisDao.getNumResults(analysis));
    		
    		analysisVOs.add(summaryVO);
    	}
    	
    	return analysisVOs;
	}
}

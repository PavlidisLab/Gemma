/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.tasks.analysis.diffex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.expression.diff.DiffExMetaAnalyzerService;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResultValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisValueObject;

/**
 * A differential expression meta-analysis space task
 * 
 * @author frances
 * @version $Id$
 */
@Component
public class DiffExMetaAnalyzerTaskImpl implements DiffExMetaAnalyzerTask {
    private static Log log = LogFactory.getLog( DiffExMetaAnalyzerTaskImpl.class.getName() );

	@Autowired
	private DifferentialExpressionResultService differentialExpressionResultService;
	
	@Autowired
	private DiffExMetaAnalyzerService diffExMetaAnalyzerService;

	private Collection<ExpressionAnalysisResultSet> loadAnalysisResultSet(Collection<Long> analysisResultSetIds) {
		Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
		
		for (Long analysisResultSetId : analysisResultSetIds) {
			ExpressionAnalysisResultSet expressionAnalysisResultSet = this.differentialExpressionResultService.loadAnalysisResultSet(analysisResultSetId);
			
	        if ( expressionAnalysisResultSet == null ) {
	            log.warn( "No diff ex result set with ID=" + analysisResultSetId );
	            return null;
	        }
		
	        resultSets.add(expressionAnalysisResultSet);
		}
		return resultSets;
	}
	
    @Override
    @TaskMethod
    public TaskResult execute( DiffExMetaAnalyzerTaskCommand command ) {
		Collection<ExpressionAnalysisResultSet> resultSets = loadAnalysisResultSet(command.getAnalysisResultSetIds());
		
		GeneDifferentialExpressionMetaAnalysis metaAnalysis = this.diffExMetaAnalyzerService.analyze(resultSets, command.getName(), command.getDescription());
	
		final GeneDifferentialExpressionMetaAnalysisValueObject metaAnalysisVO;
		
		if (metaAnalysis == null) {
			metaAnalysisVO = null;
		} else {
			metaAnalysisVO = new GeneDifferentialExpressionMetaAnalysisValueObject(metaAnalysis);

			// if we are analyzing, but not saving, then we need to sort the results and then prepare
			// the requested number of results for displaying.
			if (command.getName() == null) {
				// Sort by p value.
				List<GeneDifferentialExpressionMetaAnalysisResultValueObject> results = new ArrayList<GeneDifferentialExpressionMetaAnalysisResultValueObject>(metaAnalysisVO.getResults());
				Collections.sort( results, new Comparator<GeneDifferentialExpressionMetaAnalysisResultValueObject>(){
			        public int compare(GeneDifferentialExpressionMetaAnalysisResultValueObject result1, GeneDifferentialExpressionMetaAnalysisResultValueObject result2) {
			            return result1.getMetaPvalue().compareTo(result2.getMetaPvalue());
			        }} );
			
				int toIndex = command.getResultSetCount() <= 0 ?
						results.size() :
						command.getResultSetCount();
							
				metaAnalysisVO.setResults(results.subList(0, Math.min(results.size(), toIndex)));
			}
		}
		
        return new TaskResult( command, metaAnalysisVO );
    }
}

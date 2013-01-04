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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.expression.diff.DiffExMetaAnalyzerService;
import ubic.gemma.analysis.expression.diff.GeneDiffExMetaAnalysisHelperService;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject;

/**
 * A differential expression meta-analysis space task
 * 
 * @author frances
 * @version $Id$
 */
@Component
public class DiffExMetaAnalyzerTaskImpl implements DiffExMetaAnalyzerTask {

    @Autowired
    private DiffExMetaAnalyzerService diffExMetaAnalyzerService;

    @Autowired
    private GeneDiffExMetaAnalysisHelperService geneDiffExMetaAnalysisHelperService;

    @Override
    @TaskMethod
    public TaskResult execute( DiffExMetaAnalyzerTaskCommand command ) {
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = this.diffExMetaAnalyzerService.analyze( command
                .getAnalysisResultSetIds() );

        if (metaAnalysis != null) {
        	metaAnalysis.setName( command.getName() );
        	metaAnalysis.setDescription( command.getDescription() );

	        if ( command.isPersist() ) {
	            metaAnalysis = this.diffExMetaAnalyzerService.persist( metaAnalysis );
	        }
        }

        GeneDifferentialExpressionMetaAnalysisDetailValueObject metaAnalysisVO = ( metaAnalysis == null ? null
                : this.geneDiffExMetaAnalysisHelperService.convertToValueObject( metaAnalysis ) );

        return new TaskResult( command, metaAnalysisVO );
    }
}

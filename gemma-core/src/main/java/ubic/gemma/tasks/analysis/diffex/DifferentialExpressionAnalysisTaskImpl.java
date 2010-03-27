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
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * A differential expression analysis spaces task
 * 
 * @author keshav
 * @version $Id$
 */
@Service
public class DifferentialExpressionAnalysisTaskImpl implements DifferentialExpressionAnalysisTask {

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.grid.javaspaces.task.diff.DifferentialExpressionAnalysisTask#execute(ubic.gemma.grid.javaspaces.task
     * .diff. SpacesDifferentialExpressionAnalysisCommand)
     */
    @TaskMethod
    public TaskResult execute( DifferentialExpressionAnalysisTaskCommand command ) {

        DifferentialExpressionAnalysis results = doAnalysis( command );

        /* Don't send the full analysis to space space. Instead, create a minimal result. */
        DifferentialExpressionAnalysis minimalResult = DifferentialExpressionAnalysis.Factory.newInstance();
        minimalResult.setName( results.getName() );
        minimalResult.setDescription( results.getDescription() );
        minimalResult.setAuditTrail( results.getAuditTrail() );

        TaskResult result = new TaskResult( command, minimalResult );

        return result;
    }

    /**
     * @param jsDiffAnalysisCommand
     * @return
     */
    private DifferentialExpressionAnalysis doAnalysis( DifferentialExpressionAnalysisTaskCommand jsDiffAnalysisCommand ) {
        ExpressionExperiment ee = jsDiffAnalysisCommand.getExpressionExperiment();

        expressionExperimentService.thawLite( ee );

        DifferentialExpressionAnalysis results;
        AnalysisType analysisType = jsDiffAnalysisCommand.getAnalysisType();
        if ( analysisType != null ) {
            assert jsDiffAnalysisCommand.getFactors() != null;
            results = differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee,
                    jsDiffAnalysisCommand.getFactors(), analysisType );
        } else {
            results = differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee );
        }
        return results;
    }

}

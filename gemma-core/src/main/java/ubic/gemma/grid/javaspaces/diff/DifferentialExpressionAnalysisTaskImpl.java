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
package ubic.gemma.grid.javaspaces.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * A differential expression analysis spaces task that can be passed into a space and executed by a worker.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisTaskImpl extends BaseSpacesTask implements
        DifferentialExpressionAnalysisTask {

    private Log log = LogFactory.getLog( DifferentialExpressionAnalysisTaskImpl.class );

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    /*
     * (non-Javadoc)
     * @seeubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisTask#execute(ubic.gemma.grid.javaspaces.diff.
     * SpacesDifferentialExpressionAnalysisCommand)
     */
    public TaskResult execute( DifferentialExpressionAnalysisTaskCommand command ) {

        SpacesProgressAppender spacesProgressAppender = super.initProgressAppender( this.getClass() );

        DifferentialExpressionAnalysis results = doAnalysis( command );

        TaskResult result = new TaskResult();

        /* Don't send the full analysis to space space. Instead, create a minimal result. */
        DifferentialExpressionAnalysis minimalResult = DifferentialExpressionAnalysis.Factory.newInstance();
        minimalResult.setName( results.getName() );
        minimalResult.setDescription( results.getDescription() );
        minimalResult.setAuditTrail( results.getAuditTrail() );

        result.setAnswer( minimalResult );

        result.setTaskID( super.taskId );
        log.info( "Task execution complete ... returning result for task with id " + result.getTaskID() );

        super.tidyProgress( spacesProgressAppender );

        return result;
    }

    private DifferentialExpressionAnalysis doAnalysis( DifferentialExpressionAnalysisTaskCommand jsDiffAnalysisCommand ) {
        ExpressionExperiment ee = jsDiffAnalysisCommand.getExpressionExperiment();
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

    /**
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalyzerService(
            DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService ) {
        this.differentialExpressionAnalyzerService = differentialExpressionAnalyzerService;
    }
}

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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.progress.TaskRunningService;

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
    private ExpressionExperimentService expressionExperimentService = null;

    private long counter = 0;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisTask#execute(ubic.gemma.grid.javaspaces.diff.SpacesDifferentialExpressionAnalysisCommand)
     */
    public SpacesResult execute( SpacesDifferentialExpressionAnalysisCommand jsDiffAnalysisCommand ) {

        super.initProgressAppender( this.getClass() );

        boolean forceAnalysis = jsDiffAnalysisCommand.isForceAnalysis();
        String accession = jsDiffAnalysisCommand.getAccession();

        SpacesResult result = new SpacesResult();

        ExpressionExperiment ee = expressionExperimentService.findByShortName( accession );
        expressionExperimentService.thaw( ee );

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalyzerService
                .getDifferentialExpressionAnalyses( ee, forceAnalysis );

        result.setAnswer( expressionAnalyses );

        counter++;
        result.setTaskID( counter );
        log.info( "Task execution complete ... returning result " + result.getAnswer() + " with id "
                + result.getTaskID() );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /**
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalyzerService(
            DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService ) {
        this.differentialExpressionAnalyzerService = differentialExpressionAnalyzerService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}

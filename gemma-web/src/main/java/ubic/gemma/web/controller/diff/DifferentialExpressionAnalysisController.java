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
package ubic.gemma.web.controller.diff;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisTask;
import ubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisTaskCommand;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to run differential expression analysis either locally or in a space.
 * 
 * @spring.bean id="differentialExpressionAnalysisController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="differentialExpressionAnalyzerService" ref="differentialExpressionAnalyzerService"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisController extends AbstractSpacesController<DifferentialExpressionAnalysis> {

    /**
     * Regular (local) job.
     */
    private class DiffAnalysisJob extends BaseControllerJob<DifferentialExpressionAnalysis> {

        /**
         * @param taskId
         * @param commandObj
         */
        public DiffAnalysisJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        public DifferentialExpressionAnalysis call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            DifferentialExpressionAnalysisTaskCommand diffAnalysisCommand = ( ( DifferentialExpressionAnalysisTaskCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + diffAnalysisCommand.getExpressionExperiment().getShortName() );

            return processJob( diffAnalysisCommand );
        }

        /*
         * (non-Javadoc)
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected DifferentialExpressionAnalysis processJob( TaskCommand c ) {
            DifferentialExpressionAnalysisTaskCommand dc = ( DifferentialExpressionAnalysisTaskCommand ) c;

            ExpressionExperiment ee = dc.getExpressionExperiment();
            expressionExperimentService.thawLite( ee );
            DifferentialExpressionAnalysis results = differentialExpressionAnalyzerService
                    .runDifferentialExpressionAnalyses( ee );
            return results;
        }
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class DiffAnalysisSpaceJob extends DiffAnalysisJob {

        final DifferentialExpressionAnalysisTask taskProxy = ( DifferentialExpressionAnalysisTask ) updatedContext
                .getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public DiffAnalysisSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        /*
         * (non-Javadoc)
         * @see
         * ubic.gemma.web.controller.diff.DifferentialExpressionAnalysisController.DiffAnalysisJob#processJob(ubic.gemma
         * .grid.javaspaces.diff.DifferentialExpressionAnalysisCommand)
         */
        @Override
        protected DifferentialExpressionAnalysis processJob( TaskCommand baseCommand ) {
            baseCommand.setTaskId( this.taskId );
            return ( DifferentialExpressionAnalysis ) process(
                    ( DifferentialExpressionAnalysisTaskCommand ) baseCommand ).getAnswer();
        }

        /**
         * @param diffCommand
         * @return
         */
        private TaskResult process( DifferentialExpressionAnalysisTaskCommand diffCommand ) {
            expressionExperimentService.thawLite( diffCommand.getExpressionExperiment() );
            TaskResult result = taskProxy.execute( diffCommand );
            return result;
        }

    }

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    private ExpressionExperimentService expressionExperimentService = null;

    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {
        /* this 'run' method is exported in the spring-beans.xml */

        ExpressionExperiment ee = expressionExperimentService.load( id );

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), DifferentialExpressionAnalysisTask.class
                .getName(), true );
    }

    public void setDifferentialExpressionAnalyzerService(
            DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService ) {
        this.differentialExpressionAnalyzerService = differentialExpressionAnalyzerService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<DifferentialExpressionAnalysis> getRunner( String jobId, Object command ) {
        return new DiffAnalysisJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<DifferentialExpressionAnalysis> getSpaceRunner( String jobId, Object command ) {
        return new DiffAnalysisSpaceJob( jobId, command );

    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.
     * HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        return "differentialExpressionAnalysis";
    }
}

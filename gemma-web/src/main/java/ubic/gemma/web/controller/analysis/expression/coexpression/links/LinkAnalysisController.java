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
package ubic.gemma.web.controller.analysis.expression.coexpression.links;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.analysis.coexpression.links.LinkAnalysisTask;
import ubic.gemma.grid.javaspaces.analysis.coexpression.links.LinkAnalysisTaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to preprocess expression data vectors.
 * 
 * @spring.bean id="linkAnalysisController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="linkAnalysisService" ref="linkAnalysisService"
 * @author keshav
 * @version $Id$
 */
public class LinkAnalysisController extends AbstractSpacesController<ModelAndView> {

    private ExpressionExperimentService expressionExperimentService = null;
    private LinkAnalysisService linkAnalysisService = null;

    public void setLinkAnalysisService( LinkAnalysisService linkAnalysisService ) {
        this.linkAnalysisService = linkAnalysisService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

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

        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }

        LinkAnalysisConfig lac = new LinkAnalysisConfig();
        FilterConfig fc = new FilterConfig();
        LinkAnalysisTaskCommand cmd = new LinkAnalysisTaskCommand( null, ee, lac, fc );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), LinkAnalysisTask.class.getName(), true );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String jobId, Object command ) {
        return new LinkAnalysisJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId, Object command ) {
        return new LinkAnalysisSpaceJob( jobId, command );
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class LinkAnalysisSpaceJob extends LinkAnalysisJob {

        final LinkAnalysisTask taskProxy = ( LinkAnalysisTask ) updatedContext.getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public LinkAnalysisSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        /*
         * (non-Javadoc)
         * @seeubic.gemma.web.controller.analysis.expression.coexpression.links.LinkAnalysisController.LinkAnalysisJob#
         * processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( TaskCommand baseCommand ) {
            assert this.taskId != null;
            baseCommand.setTaskId( this.taskId );
            LinkAnalysisTaskCommand vectorCommand = ( LinkAnalysisTaskCommand ) baseCommand;
            process( vectorCommand );
            return new ModelAndView( new RedirectView( "/Gemma" ) );
        }

        /**
         * @param command
         * @return
         */
        private TaskResult process( LinkAnalysisTaskCommand c ) {
            expressionExperimentService.thawLite( c.getExpressionExperiment() );
            TaskResult result = taskProxy.execute( c );
            return result;
        }

    }

    /**
     * @author keshav
     */
    private class LinkAnalysisJob extends BaseControllerJob<ModelAndView> {

        /**
         * @param taskId
         * @param commandObj
         */
        public LinkAnalysisJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        /*
         * (non-Javadoc)
         * @see java.util.concurrent.Callable#call()
         */
        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            LinkAnalysisTaskCommand linkAnalyisCommand = ( LinkAnalysisTaskCommand ) command;

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + linkAnalyisCommand.getExpressionExperiment().getShortName() );

            return processJob( linkAnalyisCommand );
        }

        /*
         * (non-Javadoc)
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( TaskCommand c ) {
            LinkAnalysisTaskCommand lac = ( LinkAnalysisTaskCommand ) c;
            expressionExperimentService.thawLite( lac.getExpressionExperiment() );
            linkAnalysisService.process( lac.getExpressionExperiment(), lac.getFilterConfig(), lac
                    .getLinkAnalysisConfig() );

            return new ModelAndView( new RedirectView( "/Gemma" ) );

            // throw new UnsupportedOperationException( "Cannot run locally at this time.  Run in a space." );
        }
    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.
     * HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }

}

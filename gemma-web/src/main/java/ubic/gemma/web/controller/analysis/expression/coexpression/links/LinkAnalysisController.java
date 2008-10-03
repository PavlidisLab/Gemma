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
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.analysis.coexpression.links.LinkAnalysisTask;
import ubic.gemma.grid.javaspaces.analysis.coexpression.links.SpacesLinkAnalysisCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseCommand;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to preprocess expression data vectors.
 * 
 * @spring.bean id="linkAnalysisController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @author keshav
 * @version $Id$
 */
public class LinkAnalysisController extends AbstractSpacesController {

    private ExpressionExperimentService expressionExperimentService = null;

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
        expressionExperimentService.thaw( ee );

        LinkAnalysisCommand cmd = new LinkAnalysisCommand();
        cmd.setExpressionExperiment( ee );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), LinkAnalysisTask.class.getName(), true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob getRunner( String jobId, Object command ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob getSpaceRunner( String jobId, Object command ) {
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
         * 
         * @see ubic.gemma.web.controller.analysis.expression.coexpression.links.LinkAnalysisController.LinkAnalysisJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( BaseCommand baseCommand ) {
            LinkAnalysisCommand vectorCommand = ( LinkAnalysisCommand ) baseCommand;
            process( vectorCommand );
            return new ModelAndView( new RedirectView( "/Gemma" ) );
        }

        /**
         * @param command
         * @return
         */
        private SpacesResult process( LinkAnalysisCommand command ) {
            SpacesLinkAnalysisCommand jsCommand = createCommandObject( command );
            SpacesResult result = taskProxy.execute( jsCommand );
            return result;
        }

        /**
         * @param command
         * @return
         */
        protected SpacesLinkAnalysisCommand createCommandObject( LinkAnalysisCommand command ) {
            LinkAnalysisConfig lac = new LinkAnalysisConfig();
            FilterConfig fc = new FilterConfig();
            return new SpacesLinkAnalysisCommand( taskId, command.getExpressionExperiment(), lac, fc );
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
         * 
         * @see java.util.concurrent.Callable#call()
         */
        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            LinkAnalysisCommand vectorCommand = ( ( LinkAnalysisCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + vectorCommand.getExpressionExperiment().getShortName() );

            return processJob( vectorCommand );
        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( BaseCommand command ) {
            throw new UnsupportedOperationException( "Cannot run locally at this time.  Run in a space." );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }

}

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
package ubic.gemma.web.controller.expression.arrayDesign;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.expression.arrayDesign.ArrayDesignProbeMapperTask;
import ubic.gemma.grid.javaspaces.expression.arrayDesign.SpacesArrayDesignProbeMapperCommand;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseCommand;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to run array design probe mapper either locally or in a space.
 * 
 * @spring.bean id="arrayDesignProbeMapperController"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignProbeMapperController extends AbstractSpacesController {

    private ArrayDesignService arrayDesignService = null;

    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {
        /* this 'run' method is exported in the spring-beans.xml */

        ArrayDesign ad = arrayDesignService.load( id );
        arrayDesignService.thaw( ad );

        ArrayDesignProbeMapperCommand cmd = new ArrayDesignProbeMapperCommand();
        cmd.setArrayDesign( ad );

        return super
                .run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ArrayDesignProbeMapperTask.class.getName(), true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String jobId, Object command ) {
        return new ArrayDesignProbeMapperJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId, Object command ) {
        return new ArrayDesignProbeMapperSpaceJob( jobId, command );

    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class ArrayDesignProbeMapperSpaceJob extends ArrayDesignProbeMapperJob {

        final ArrayDesignProbeMapperTask taskProxy = ( ArrayDesignProbeMapperTask ) updatedContext.getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public ArrayDesignProbeMapperSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignProbeMapperController.ArrayDesignProbeMapperJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( BaseCommand baseCommand ) {
            ArrayDesignProbeMapperCommand command = ( ArrayDesignProbeMapperCommand ) baseCommand;
            process( command );
            return new ModelAndView( new RedirectView( "/Gemma" ) );
        }

        /**
         * @param command
         * @return
         */
        private SpacesResult process( ArrayDesignProbeMapperCommand command ) {
            SpacesArrayDesignProbeMapperCommand jsCommand = createCommandObject( command );
            SpacesResult result = taskProxy.execute( jsCommand );
            return result;
        }

        protected SpacesArrayDesignProbeMapperCommand createCommandObject( ArrayDesignProbeMapperCommand command ) {
            return new SpacesArrayDesignProbeMapperCommand( taskId, command.isForceAnalysis(), command.getArrayDesign() );
        }

    }

    /**
     * Regular (local) job.
     */
    private class ArrayDesignProbeMapperJob extends BaseControllerJob<ModelAndView> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ArrayDesignProbeMapperJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            ArrayDesignProbeMapperCommand pmCommand = ( ( ArrayDesignProbeMapperCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + pmCommand.getArrayDesign().getShortName() );

            return processJob( pmCommand );
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
        return "arrayDesignProbeMapper";
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }
}

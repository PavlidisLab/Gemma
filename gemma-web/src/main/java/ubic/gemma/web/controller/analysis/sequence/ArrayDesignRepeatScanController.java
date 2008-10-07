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
package ubic.gemma.web.controller.analysis.sequence;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.analysis.sequence.ArrayDesignRepeatScanTask;
import ubic.gemma.grid.javaspaces.analysis.sequence.SpacesArrayDesignRepeatScanCommand;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseCommand;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to run array design repeat scan either locally or in a space.
 * 
 * @spring.bean id="arrayDesignRepeatScanController"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignRepeatScanController extends AbstractSpacesController {

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

        ArrayDesignRepeatScanCommand cmd = new ArrayDesignRepeatScanCommand();
        cmd.setArrayDesign( ad );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ArrayDesignRepeatScanTask.class.getName(), true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String jobId, Object command ) {
        return new ArrayDesignRepeatScanJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId, Object command ) {
        return new ArrayDesignRepeatScanSpaceJob( jobId, command );

    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class ArrayDesignRepeatScanSpaceJob extends ArrayDesignRepeatScanJob {

        final ArrayDesignRepeatScanTask taskProxy = ( ArrayDesignRepeatScanTask ) updatedContext.getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public ArrayDesignRepeatScanSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.web.controller.analysis.sequence.ArrayDesignRepeatScanController.ArrayDesignRepeatScanJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected ModelAndView processJob( BaseCommand baseCommand ) {
            ArrayDesignRepeatScanCommand command = ( ArrayDesignRepeatScanCommand ) baseCommand;
            process( command );
            return new ModelAndView( new RedirectView( "/Gemma" ) );
        }

        /**
         * @param command
         * @return
         */
        private SpacesResult process( ArrayDesignRepeatScanCommand command ) {
            SpacesArrayDesignRepeatScanCommand jsCommand = createCommandObject( command );
            SpacesResult result = taskProxy.execute( jsCommand );
            return result;
        }

        protected SpacesArrayDesignRepeatScanCommand createCommandObject( ArrayDesignRepeatScanCommand command ) {
            return new SpacesArrayDesignRepeatScanCommand( taskId, command.getArrayDesign() );
        }

    }

    /**
     * Regular (local) job.
     */
    private class ArrayDesignRepeatScanJob extends BaseControllerJob<ModelAndView> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ArrayDesignRepeatScanJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            ArrayDesignRepeatScanCommand repeatScanCommand = ( ( ArrayDesignRepeatScanCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + repeatScanCommand.getArrayDesign().getShortName() );

            return processJob( repeatScanCommand );
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
        return "arrayDesignRepeatScan";
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

}

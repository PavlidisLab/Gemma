/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormController;

/**
 * Controller that does nothing except wait a while and return redirect to progress bar. Used for tests.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Controller
public class MockLongJobController extends BackgroundProcessingFormController {

    /**
     * 
     */
    public static final int JOB_LENGTH = 2000;

    /*
     * (non-Javadoc)
     * @see
     * org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(javax.servlet.http.HttpServletRequest
     * , javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/mock.html")
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        Object die = request.getAttribute( "throw" );
        return startJob( die );

    }

    class WasteOfTime extends BackgroundControllerJob<ModelAndView> {

        Object die = null;

        public WasteOfTime( String taskId, Object command ) {
            super( taskId );
            this.command = command;
            this.die = command;
        }

        public ModelAndView call() throws Exception {

            ProgressJob job = init( "Doing something that will take a while" );

            long millis = System.currentTimeMillis();
            while ( System.currentTimeMillis() - millis < JOB_LENGTH ) {
                Thread.sleep( 500 );
                log.info( "Doing sumpin', done in " + ( JOB_LENGTH - ( System.currentTimeMillis() - millis ) )
                        + " milliseconds" );
                // ProgressManager.updateCurrentThreadsProgressJob( "just sayin' hi" );
                if ( this.die != null ) {
                    throw new RuntimeException( "Exception thrown on purpose." );
                }
            }

            log.info( "Done doin sumpin'" );
            ProgressManager.destroyProgressJob( job, false );

            Map<String, Object> model = new HashMap<String, Object>();
            model.put( "answer", "42" );
            return new ModelAndView( "view", model );
        }

    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, Object command, MessageUtil messenger ) {
        return new WasteOfTime( taskId, command );
    }
}

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

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormController;

/**
 * Controller that does nothing except wait a while and return redirect to progress bar. Used for tests.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="mockController"
 */
public class MockLongJobController extends BackgroundProcessingFormController {

    /**
     * 
     */
    public static final int JOB_LENGTH = 2000;
    static Log log = LogFactory.getLog( MockLongJobController.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    @SuppressWarnings("unused")
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        return startJob( null, request );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BackgroundProcessingFormController#getRunner(java.lang.String,
     *      org.acegisecurity.context.SecurityContext, javax.servlet.http.HttpServletRequest, java.lang.Object,
     *      ubic.gemma.web.util.MessageUtil)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String jobId, SecurityContext securityContext,
             Object command, MessageUtil messenger ) {
        return new BackgroundControllerJob<ModelAndView>( jobId, securityContext,  command, messenger ) {

            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );
                ProgressManager.createProgressJob( this.getTaskId(), "SomeUser", "Doin' sumpin'" );
                //
                // if ( this.getRequest().getAttribute( "throw" ) != null ) {
                // log.info( "I'm throwing an exception" );
                // throw new IllegalArgumentException( "I'm not happy." );
                //                }

                long millis = System.currentTimeMillis();
                while ( System.currentTimeMillis() - millis < JOB_LENGTH ) {
                    Thread.sleep( 500 );
                    log.info( "Doing sumpin', done in " + ( JOB_LENGTH - ( System.currentTimeMillis() - millis ) )
                            + " milliseconds" );

                }

                log.info( "Done doin sumpin" );

                Map<String, Object> model = new HashMap<String, Object>();
                model.put( "answer", "42" );
                return new ModelAndView( "view", model );
            }

        };
    }
}

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
package ubic.gemma.web.controller.demo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.web.controller.BaseFormController;

/**
 * Demo of long process method described at {@link http://forum.java.sun.com/thread.jspa?forumID=45&threadID=554223}.
 * Ported to Spring.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="longProcess" name="/progressbar.html"
 * @spring.property name="commandClass" value="ubic.gemma.web.controller.demo.ProcessParams"
 * @spring.property name="commandName" value="processParams"
 * @spring.property name="formView" value="/progressbar.html"
 * @spring.property name="successView" value="/wait.html"
 */
public class LongProcess extends BaseFormController {

    @SuppressWarnings("unused")
    @Override
    public ModelAndView onSubmit( final HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        final ProcessParams params = ( ProcessParams ) command;
        final HttpSession session = request.getSession();

        final double startAt = params.getStartAt();
        final double endAt = params.getEndAt();
        final long sleepTime = params.getSleepTime();
        // This will be tested to see if we are done or not
        session.setAttribute( "stillProcessing", Boolean.TRUE );
        Thread t = new Thread( new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() { // Do your real processing here
                for ( double i = 0.0; i < ( endAt - startAt ); i += 1.0 ) {
                    Integer percentDone = new Integer( ( int ) ( ( i / ( endAt - startAt ) ) * 100 ) + 1 );
                    // Sets a value to be used in the processing jsp
                    session.setAttribute( "percentDone", percentDone );
                    try {
                        Thread.sleep( sleepTime );
                    } catch ( InterruptedException ie ) {
                    }
                }
                // Done processing, let processing jsp know
                session.setAttribute( "stillProcessing", Boolean.FALSE );
            }
        } );
        t.start();
        // response.sendRedirect( response.encodeRedirectURL( "processing.jsp" ) );
        return new ModelAndView( getSuccessView() );
    }
}

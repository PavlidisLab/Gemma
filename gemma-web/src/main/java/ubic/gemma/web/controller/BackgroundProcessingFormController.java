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
package ubic.gemma.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * Extends this when the controller needs to run a long task (show a progress bar). To use it, implement getRunner and
 * call startJob in your onSubmit method.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BackgroundProcessingFormController extends BaseFormController {

    /**
     * @param eeLoadCommand
     */
    protected void startJob( Object command, HttpServletRequest request, String description ) {
        /*
         * all new threads need this to acccess protected resources (like services)
         */
        SecurityContext context = SecurityContextHolder.getContext();

        BackgroundControllerJob runner = getRunner( context, request, command, description );

        Thread job = new Thread( runner );
        request.getSession().setAttribute( "threadId", job.getId() ); // this needs to be set in case the thread needs
                                                                        // to be
        // canceled
        job.start();
    }

    /**
     * You have to implement this in your subclass.
     * 
     * @param securityContext
     * @param command from form
     * @param jobDescription
     * @return
     */
    protected abstract BackgroundControllerJob getRunner( SecurityContext securityContext, HttpServletRequest request,
            Object command, String jobDescription );

}

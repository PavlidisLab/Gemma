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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.web.util.MessageUtil;

/**
 * Extends this when the controller needs to run a long task (show a progress bar). To use it, implement getRunner and
 * call startJob in your onSubmit method.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BackgroundProcessingFormController extends BaseFormController {

    /**
     * Use this to access the Future job (as in, request.getAttribute(JOB_ATTRIBUTE)
     */
    public static final String JOB_ATTRIBUTE = "job";

    /**
     * @param startJob
     */
    protected void startJob( Object command, HttpServletRequest request ) {
        /*
         * all new threads need this to acccess protected resources (like services)
         */
        SecurityContext context = SecurityContextHolder.getContext();

        BackgroundControllerJob<ModelAndView> job = getRunner( context, request, command, this.getMessageUtil() );

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future task = executorService.submit( job );

        request.setAttribute( JOB_ATTRIBUTE, task );
    }

    /**
     * You have to implement this in your subclass.
     * 
     * @param securityContext
     * @param command from form
     * @return
     */
    protected abstract BackgroundControllerJob<ModelAndView> getRunner( SecurityContext securityContext,
            HttpServletRequest request, Object command, MessageUtil messenger );

}

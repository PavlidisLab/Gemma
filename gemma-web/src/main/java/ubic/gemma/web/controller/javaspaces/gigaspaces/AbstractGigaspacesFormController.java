/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller.javaspaces.gigaspaces;

import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServletRequest;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.javaspaces.gigaspaces.GemmaSpacesEnum;
import ubic.gemma.util.javaspaces.gigaspaces.GigaspacesUtil;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormController;
import ubic.gemma.web.controller.TaskRunningService;
import ubic.gemma.web.util.MessageUtil;

/**
 * Controllers requiring the capability to submit jobs to a compute server should extend this controller.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="abstractGigaspacesFormController"
 * @spring.property name="gigaspacesUtil" value="gigaspacesUtil"
 */
public abstract class AbstractGigaspacesFormController extends BackgroundProcessingFormController {

    private GigaspacesUtil gigaspacesUtil = null;

    /**
     * Runs the job in a {@link JavaSpace}.
     * 
     * @param jobId
     * @param securityContext
     * @param request
     * @param command
     * @param messenger
     * @return BackgroundControllerJob<ModelAndView>
     */
    abstract protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId,
            SecurityContext securityContext, HttpServletRequest request, Object command, MessageUtil messenger );

    /**
     * @return ApplicationContext
     */
    public ApplicationContext addGigaspacesToApplicationContext() {
        return gigaspacesUtil.addGigaspacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BackgroundProcessingFormController#startJob(java.lang.Object,
     *      javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected synchronized ModelAndView startJob( Object command, HttpServletRequest request ) {
        /*
         * all new threads need this to acccess protected resources (like services)
         */
        SecurityContext context = SecurityContextHolder.getContext();

        String taskId = TaskRunningService.generateTaskId();

        ApplicationContext updatedContext = addGigaspacesToApplicationContext();
        BackgroundControllerJob<ModelAndView> job = null;
        if ( updatedContext.containsBean( "gigaspacesTemplate" ) ) {
            job = getSpaceRunner( taskId, context, request, command, this.getMessageUtil() );
        } else {
            job = getRunner( taskId, context, request, command, this.getMessageUtil() );
        }

        assert taskId != null;
        request.getSession().setAttribute( JOB_ATTRIBUTE, taskId );

        taskRunningService.submitTask( taskId, new FutureTask<ModelAndView>( job ) );

        ModelAndView mnv = new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskid=" + taskId ) );
        mnv.addObject( "taskId", taskId );
        return mnv;
    }

    /**
     * @param gigaspacesUtil
     */
    public void setGigaspacesUtil( GigaspacesUtil gigaspacesUtil ) {
        this.gigaspacesUtil = gigaspacesUtil;
    }

}

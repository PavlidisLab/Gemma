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

import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServletRequest;

import org.compass.spring.web.mvc.AbstractCompassGpsCommandController;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.web.util.MessageUtil;

/**
 * Extends this when the controller needs to run a long task (show a progress bar). To use it, implement getRunner and
 * call startJob in your onSubmit method.
 * 
 * @author klc
 * 
 * @version $Id$
 */
public abstract class BackgroundProcessingCompassIndexController extends AbstractCompassGpsCommandController {

    /**
     * 
     */

    /**
     * Use this to access the task id in the request.
     */
    public final static String JOB_ATTRIBUTE = "taskId";

    TaskRunningService taskRunningService;
    private MessageUtil messageUtil;
    /**
     * @param taskRunningService the taskRunningService to set
     */
    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    
    /**
     * @param  BackgroundControllerJob<ModelAndView> job
     * @param request
     * @return task id
     * 
     * This allows the background controller job to be created outside and passed in effectively allowing one controller to create more than 1 job
     */
    protected synchronized String startJob( HttpServletRequest request,  BackgroundControllerJob<ModelAndView> job  ) {
        /*
         * all new threads need this to acccess protected resources (like services)
         */

        String taskId = TaskRunningService.generateTaskId();

        assert taskId != null;
        request.getSession().setAttribute( JOB_ATTRIBUTE, taskId );
        job.setTaskId(taskId);   

        taskRunningService.submitTask( taskId, new FutureTask<ModelAndView>( job ) );

        return taskId;
    }

     /**
     * @param messageUtil the messageUtil to set
     */
    public void setMessageUtil( MessageUtil messageUtil ) {
        this.messageUtil = messageUtil;
    }
    
    /**
     * @param messageUtil the messageUtil to set
     */
    public MessageUtil getMessageUtil( ) {
        return messageUtil;
    }



}

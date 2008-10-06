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

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.util.progress.TaskRunningService;

/**
 * Extends this when the controller needs to run a long task (show a progress bar). To use it, implement getRunner and
 * call startJob in your onSubmit method.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.property name="taskRunningService" ref="taskRunningService"
 */
public abstract class BackgroundProcessingMultiActionController extends BaseMultiActionController {

    /**
     * Use this to access the task id in the request.
     */
    public final static String JOB_ATTRIBUTE = "taskId";

    TaskRunningService taskRunningService;

    /**
     * @param taskRunningService the taskRunningService to set
     */
    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    /**
     * @param BackgroundControllerJob<ModelAndView> job
     * @return task id This allows the background controller job to be created outside and passed in effectively
     *         allowing one controller to create more than 1 job
     */
    protected synchronized ModelAndView startJob( BackgroundControllerJob job ) {
        String taskId = run( job );
        ModelAndView mnv = new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskId=" + taskId ) );
        mnv.addObject( "taskId", taskId );
        return mnv;
    }

    /**
     * For AJAX use. In your subclass, call this method with your job, which has to be created in a AJAX-accessible
     * method.
     * 
     * @param job
     * @return
     */
    protected String run( BackgroundControllerJob job ) {

        String taskId = TaskRunningService.generateTaskId();
        job.setTaskId( taskId );

        taskRunningService.submitTask( taskId, new FutureTask<ModelAndView>( job ) );
        return taskId;
    }

}
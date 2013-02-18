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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.job.SubmittedTask;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.webapp.TaskRunningService;

/**
 * Generic controller that looks for a finished job. Used when a job is done to get the result.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
@Controller
public class TaskCompletionController {

    @Autowired
    TaskRunningService taskRunningService;

    /**
     * AJAX
     * 
     * @param taskId
     * @return
     */
    public Object checkResult( String taskId ) throws Exception {
        SubmittedTask submittedTask = taskRunningService.getSubmittedTask(taskId);
        if ( submittedTask == null ) return null;

        TaskResult result = submittedTask.getResult();

        if ( result == null ) return null;

        Object answer = result.getAnswer();

        if ( answer instanceof ModelAndView ) {
            View view = ( ( ModelAndView ) answer ).getView();
            if ( view instanceof RedirectView ) {
                return ( ( RedirectView ) view ).getUrl();
            }
            return null;
        }

        if ( answer instanceof Exception ) {
            throw ( Exception ) answer;
        }

        return answer;
    }
}

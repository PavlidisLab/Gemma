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

package ubic.gemma.grid.javaspaces;

import java.util.concurrent.FutureTask;

import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.grid.javaspaces.util.SpacesEnum;
import ubic.gemma.grid.javaspaces.util.SpacesJobObserver;
import ubic.gemma.grid.javaspaces.util.SpacesUtil;
import ubic.gemma.grid.javaspaces.util.entry.SpacesProgressEntry;
import ubic.gemma.util.progress.BackgroundProgressJob;
import ubic.gemma.util.progress.TaskRunningService;

import com.j_spaces.core.client.NotifyModifiers;

/**
 * @author klc
 * @version $Id$
 */
public abstract class AbstractSpacesProgressService {

    protected static Log log = LogFactory.getLog( AbstractSpacesProgressService.class.getName() );
    protected TaskRunningService taskRunningService;

    protected ApplicationContext updatedContext = null;

    private SpacesUtil spacesUtil = null;

    /**
     * @return ApplicationContext
     */
    public ApplicationContext addGemmaSpacesToApplicationContext() {
        if ( spacesUtil == null ) spacesUtil = new SpacesUtil();

        return spacesUtil.addGemmaSpacesToApplicationContext( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /**
     * For use in AJAX-driven runs.
     * 
     * @param command
     * @param spaceUrl
     * @param taskName
     * @param runInWebapp
     * @return
     */
    public String run( Object command, String spaceUrl, String taskName, boolean runInWebapp ) {
        String taskId = null;

        updatedContext = addGemmaSpacesToApplicationContext();
        BackgroundProgressJob<ModelAndView> job = null;
        if ( updatedContext.containsBean( "gigaspacesTemplate" ) ) {

            taskId = SpacesUtil.getTaskIdFromTask( updatedContext, taskName );

            if ( !spacesUtil.canServiceTask( taskName, spaceUrl ) ) {
                // TODO Add sending of email to user.
                // User user = SecurityUtil.getUserFromUserDetails( (
                // UserDetails ) SecurityContextHolder.getContext()
                // .getAuthentication().getPrincipal() );
                // this.sendEmail( user, "Cannot service task " + taskName + "
                // on the compute server at this time.",
                // ConfigUtils.getBaseUrl() );
                //
                // throw new RuntimeException( "No workers are registered to
                // service task " + taskName
                // + " on the compute server at this time." );

                // Throwing an exception here brings down Gemma. All that is
                // needed is an ERROR message, ie gemma is fine its the space
                // thats problematic at this point

                log.error( "Cannot execute task in space.  No service registered for: " + taskName );
                return null;

            }
            /* register this "spaces client" to receive notifications */
            SpacesJobObserver javaSpacesJobObserver = new SpacesJobObserver( taskId );

            GigaSpacesTemplate template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

            template.addNotifyDelegatorListener( javaSpacesJobObserver, new SpacesProgressEntry(), null, true,
                    Lease.FOREVER, NotifyModifiers.NOTIFY_ALL );

            job = getSpaceRunner( taskId, command );
        } else if ( !updatedContext.containsBean( "gigaspacesTemplate" ) && !runInWebapp ) {
            throw new RuntimeException(
                    "This task must be run on the compute server, but the space is not running. Please try again later" );
        } else {
            taskId = TaskRunningService.generateTaskId();
            job = getRunner( taskId, command );
        }

        assert taskId != null;

        taskRunningService.submitTask( taskId, new FutureTask<ModelAndView>( job ) );
        return taskId;
    }

    public void setSpacesUtil( SpacesUtil spacesUtil ) {
        this.spacesUtil = spacesUtil;
    }

    /**
     * @param taskRunningService the taskRunningService to set
     */
    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    abstract protected BackgroundProgressJob<ModelAndView> getRunner( String jobId, Object command );

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
    abstract protected BackgroundProgressJob<ModelAndView> getSpaceRunner( String jobId, Object command );

    /**
     * @param spacesUtil
     */
    protected void injectSpacesUtil( SpacesUtil spacesUtil ) {
        this.spacesUtil = spacesUtil;
    }

    /**
     * Starts the job on a compute server resource if the space is running and the task can be serviced. If runInWebapp
     * is true, the task will be run in the webapp virtual machine. If false the task will only be run if the space is
     * started and workers that can service the task exist.
     * 
     * @param command
     * @param spaceUrl
     * @param taskName
     * @param runInWebapp
     * @return {@link ModelAndView}
     */
    protected synchronized ModelAndView startJob( Object command, String spaceUrl, String taskName, boolean runInWebapp ) {
        String taskId = run( command, spaceUrl, taskName, runInWebapp );

        ModelAndView mnv = new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskid=" + taskId ) );
        mnv.addObject( TaskRunningService.JOB_ATTRIBUTE, taskId );
        return mnv;
    }

}

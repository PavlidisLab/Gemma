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
package ubic.gemma.web.controller.grid;

import java.util.concurrent.FutureTask;

import net.jini.core.lease.Lease;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.AbstractUrlViewController;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import com.j_spaces.core.client.NotifyModifiers;

import ubic.gemma.grid.javaspaces.SpacesHelper;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.grid.javaspaces.SpacesJobObserver;
import ubic.gemma.util.grid.javaspaces.SpacesUtil;
import ubic.gemma.util.grid.javaspaces.entry.SpacesProgressEntry;
import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.web.controller.BackgroundControllerJob;

/**
 * Subclasses implement getRunner() and getSpaceRunner()
 * 
 * @spring.property name="spacesUtil" ref="spacesUtil"
 * @spring.property name="taskRunningService" ref="taskRunningService"
 * @author Paul
 * @version $Id$
 * @see BackgroundControllerJob
 */
public abstract class AbstractSpacesController<T> extends AbstractUrlViewController {
    protected TaskRunningService taskRunningService;
    protected SpacesUtil spacesUtil = null;
    protected ApplicationContext updatedContext = null;

    private static Log log = LogFactory.getLog( AbstractSpacesController.class.getName() );

    /**
     * This method can be exposed via AJAX to allow asynchronous calls. The method returns the task id immediately after
     * starting the background job in a different thread.
     * 
     * @param command The command object containing parameters.
     * @return the task id.
     */
    public String run( Object command ) {
        String taskId = TaskRunningService.generateTaskId();
        BackgroundControllerJob<T> job = getRunner( taskId, command );
        taskRunningService.submitTask( taskId, new FutureTask<T>( job ) );
        log.debug( "Started job, taskId: " + taskId );
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

    protected abstract BackgroundControllerJob<T> getRunner( String jobId, Object command );

    protected abstract BackgroundControllerJob<T> getSpaceRunner( String jobId, Object command );

    /**
     * @param spacesUtil
     */
    protected void injectSpacesUtil( SpacesUtil s ) {
        this.spacesUtil = s;
    }

    /**
     * @return ApplicationContext
     */
    public ApplicationContext addGemmaSpacesToApplicationContext() {
        if ( spacesUtil == null ) spacesUtil = new SpacesUtil();

        if ( SpacesUtil.isSpaceRunning( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() ) ) {
            return spacesUtil.addGemmaSpacesToApplicationContext( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
        }
        return null;
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
    protected String run( Object command, String spaceUrl, String taskName, boolean runInWebapp ) {

        String taskId = null;

        updatedContext = addGemmaSpacesToApplicationContext();
        BackgroundControllerJob<T> job = null;
        if ( updatedContext != null && updatedContext.containsBean( "gigaspacesTemplate" )
                && spacesUtil.canServiceTask( taskName, spaceUrl ) ) {

            taskId = SpacesHelper.getTaskIdFromTask( updatedContext, taskName );

            /* register this "spaces client" to receive notifications */
            SpacesJobObserver javaSpacesJobObserver = new SpacesJobObserver( taskId );

            GigaSpacesTemplate template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

            template.addNotifyDelegatorListener( javaSpacesJobObserver, new SpacesProgressEntry(), null, true,
                    Lease.FOREVER, NotifyModifiers.NOTIFY_ALL );

            job = getSpaceRunner( taskId, command );
        } else if ( !runInWebapp ) {
            throw new RuntimeException(
                    "This task must be run on the compute server, but the space is not running. Please try again later" );
        } else {
            taskId = TaskRunningService.generateTaskId();
            job = getRunner( taskId, command );
        }

        assert taskId != null;
        assert job != null;

        taskRunningService.submitTask( taskId, new FutureTask<T>( job ) );
        return taskId;
    }
}

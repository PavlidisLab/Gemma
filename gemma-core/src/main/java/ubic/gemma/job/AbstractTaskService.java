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

package ubic.gemma.job;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.grid.util.SpacesUtilImpl;
import ubic.gemma.job.grid.util.TaskNotGridEnabledException;

/**
 * Addresses the problem of running long-running jobs. They run in their own thread, either in-process (if configured)
 * or on the compute grid (if available). Subclasses provide a BackgroundJob for one or both of those situations.
 * <p>
 * Limitation: This is limited to only one businessInterface, though that interface could implement multiple methods or
 * otherwise have logic that determines exactly what to do with the command.
 * 
 * @author klc, paul
 * @version $Id$
 */
public abstract class AbstractTaskService {

    protected static Log log = LogFactory.getLog( AbstractTaskService.class.getName() );

    @Autowired
    protected TaskRunningService taskRunningService;

    /**
     * Used only to see if the space can service the task.
     */
    private Class<?> businessInterface;

    @Autowired
    private SpacesUtil spacesUtil = null;

    public void setBusinessInterface( Class<?> businessInterface ) {
        this.businessInterface = businessInterface;
    }

    /**
     * @param spacesUtil
     */
    public void setSpacesUtil( SpacesUtil spacesUtil ) {
        this.spacesUtil = spacesUtil;
    }

    /**
     * @param taskRunningService the taskRunningService to set
     */
    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    /**
     * Run the job in-process (without Javaspace). Return a task result with ranInSpace = false to signify this. The
     * type of job to run can be fixed or the implementer can write logic that determines the job from the command type.
     * 
     * @param command
     * @return
     */
    abstract protected BackgroundJob<?> getInProcessRunner( TaskCommand command );

    /**
     * @return
     */
    protected Object getProxy() {
        return this.spacesUtil.getProxy();
    }

    /**
     * Runs the job on the grid. The type of job to run can be fixed or the implementer can write logic that determines
     * the job from the command type.
     * 
     * @param command
     * @return BackgroundJob<ModelAndView>
     */
    abstract protected BackgroundJob<?> getSpaceRunner( TaskCommand command );

    /**
     * Entry point for clients. Run a task as a background process. It will be run on the grid if possible, or
     * in-process if configured.
     * <p>
     * NOTE: If you call this, and want to use the grid, you must set the businessInterface first!
     * 
     * @param command object to be passed as an argument to the runner.
     * @return the taskId.
     */
    protected final String run( TaskCommand command ) {
        assert command != null;
        String taskId = command.getTaskId();
        assert taskId != null;

        boolean gigaspacesRunning = SpacesUtilImpl.isSpaceRunning();

        BackgroundJob<?> job = null;

        if ( gigaspacesRunning && this.businessInterface != null ) {

            // Trying to get the name of the bean here. FIXME temporary kludge
            String taskName = StringUtils.uncapitalize( this.businessInterface.getSimpleName() ).replace( "Task",
                    "Worker" );

            if ( spacesUtil.canServiceTask( taskName ) ) {
                job = getSpaceRunner( command );
            } else {
                job = getInProcessRunner( command );
                if ( job == null || !command.isAllowedToRunInProcess() ) {
                    throw new TaskNotGridEnabledException( "No workers registered on grid for jobs of type " + taskName
                            + " and can't run in-process." );
                }
            }
        } else {
            job = getInProcessRunner( command );
            if ( job == null || !command.isAllowedToRunInProcess() ) {
                throw new TaskNotGridEnabledException(
                        "This task must be run on the compute grid, which is currently not available." );
            }
        }

        assert job != null;
        assert job.getTaskId() != null;

        startTask( job );

        return taskId;
    }

    /**
     * Alternative convenience entrypoint for clients who wish to determine the job to be run on their own. The choice
     * of space or inprocess is determined by the job parameter.
     * 
     * @param command
     * @param job
     */
    protected final String startTask( BackgroundJob<?> job ) {

        if ( job == null ) throw new IllegalArgumentException( "Job cannot be null" );

        try {
            taskRunningService.submitTask( job );
        } catch ( ConflictingTaskException e ) {
            throw new RuntimeException( "Sorry, it looks like you already have a task like that running (taskid="
                    + e.getCollidingCommand().getTaskId() + ", submitted time="
                    + e.getCollidingCommand().getSubmissionTime() + "  ) ", e );
        }
        return job.getTaskId();
    }

}

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

package ubic.gemma.job;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.job.progress.ProgressAppender;

/**
 * Implements a long-running task in its own thread. Implementations simply implement processJob.
 * 
 * @author klc
 * @version $Id$
 */
public abstract class BackgroundJob<T extends TaskCommand> implements Callable<TaskResult> {

    protected T command;
    protected Log log = LogFactory.getLog( this.getClass().getName() );
    protected SecurityContext securityContext;
    protected String taskId;

    /**
     * @param command
     */
    public BackgroundJob( T commandObj ) {
        assert commandObj != null;

        this.taskId = commandObj.getTaskId();
        assert this.taskId != null;

        this.command = commandObj;
        this.securityContext = SecurityContextHolder.getContext();

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Callable#call()
     * 
     * @see TaskMethodAdvice
     */
    @Override
    public TaskResult call() throws Exception {
        /*
         * Do any preprocessing here
         */
        TaskResult result = new TaskResult( this.getCommand(), null );
        ProgressAppender logAppender = null;
        try {

            if ( !this.getCommand().isWillRunOnGrid() ) {
                /*
                 * Set up a local logger. On the grid, logging is set up by the TaskMethodAdvice
                 */
                logAppender = new ProgressAppender( this.taskId );
                Logger logger = LogManager.getLogger( "ubic.gemma" );
                Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
                logger.addAppender( logAppender );
                baseCodeLogger.addAppender( logAppender );
            }

            result = this.processJob();

            /*
             * Do any postprocessing here.
             */

        } catch ( Exception e ) {
            result.setFailed( true );
            throw e; // cancellation for example.
        } finally {
            /*
             * Do any cleanup here.
             */
            if ( logAppender != null ) {
                Logger logger = LogManager.getLogger( "ubic.gemma" );
                Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
                logger.removeAppender( logAppender );
                baseCodeLogger.removeAppender( logAppender );
            }
        }
        return result;

    }

    public T getCommand() {
        return this.command;
    }

    /**
     * @return the taskId
     */
    public String getTaskId() {
        return this.taskId;
    }

    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    /**
     * Implement to do the work of the job. This will be run in a separate thread.
     * 
     * @return result
     */
    protected abstract TaskResult processJob();

}

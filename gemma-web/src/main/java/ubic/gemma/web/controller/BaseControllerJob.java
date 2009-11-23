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
package ubic.gemma.web.controller;

import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class BaseControllerJob<T> extends BackgroundControllerJob<T> {

    protected boolean spacesTaskQueued = false;

    public BaseControllerJob( String taskId, Object commandObj ) {
        super( taskId );
        this.setCommand( commandObj );
    }

    /**
     * @param queued True if the the spaces task has been queued.
     */
    public void setSpacesTaskQueued( boolean queued ) {
        this.spacesTaskQueued = queued;
    }

    /**
     * Creates the progress job and initializes it with the proper progress status.
     * 
     * @param name
     * @return
     */
    protected ProgressJob initializeProgressJob( String name ) {
        ProgressJob job = super.init( name );

        if ( spacesTaskQueued ) {
            ProgressManager.updateJob( this.taskId, "Queued task " + this.taskId );
        }

        return job;

    }

    /**
     * @param command
     * @return
     */
    protected abstract T processJob( TaskCommand c );

}

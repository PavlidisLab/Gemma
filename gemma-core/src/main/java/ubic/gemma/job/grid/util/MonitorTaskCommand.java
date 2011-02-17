/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.job.grid.util;

import ubic.gemma.job.TaskCommand;

/**
 * Useless command
 * 
 * @author paul
 * @version $Id$
 */
public class MonitorTaskCommand extends TaskCommand {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int runTimeMillis = 2000;
    
    public MonitorTaskCommand() {
        this.setPersistJobDetails( false );
    }
    

    /**
     * Set to tell the job to throw an exception.
     */
    private boolean fail = false;

    public void setRunTimeMillis( int runTimeMillis ) {
        this.runTimeMillis = runTimeMillis;
    }

    public int getRunTimeMillis() {
        return runTimeMillis;
    }

    public boolean isFail() {
        return this.fail;
    }

    public void setFail( boolean fail ) {
        this.fail = fail;
    }

}

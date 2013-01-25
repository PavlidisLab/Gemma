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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.job.TaskResult;

/**
 * Empty task that can be run to monitor the state of the Space.
 * 
 * @author paul
 * @version $Id$
 */
@Deprecated
@Component
@Scope("prototype")
public class MonitorTaskImpl implements MonitorTask {

    private static Log log = LogFactory.getLog( MonitorTaskImpl.class );

    private MonitorTaskCommand command;

    @Override
    public void setCommand(MonitorTaskCommand command) {
        this.command = command;
    }

    /*
         * (non-Javadoc)
         * @see ubic.gemma.grid.javaspaces.task.MonitorTask#execute(ubic.gemma.grid.javaspaces.task.MonitorTaskCommand)
         */
    @Override
    public TaskResult execute() {

        if ( command.isFail() ) {
            throw new RuntimeException( "I was asked to fail on purpose" );
        }

        /*
         * Task doesn't do anything. Just to prove we are alive.
         */
        int millis = 1000;
        for ( int i = 0; i < command.getRunTimeMillis() / millis; i++ ) {
            //log.info( command.getTaskId() + " " + i );
            try {
                Thread.sleep( millis );
            } catch ( InterruptedException e ) {
                log.warn( "Job " + command.getTaskId() + " was interrupted" );
                return new TaskResult( command, false );
            }
        }

        return new TaskResult( command, true );
    }

}

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
package ubic.gemma.job;

/**
 * Throw when user has tried to run two tasks at the same time that are in conflict. Typically this means two tasks of
 * the same type but this may be subject to business rules documented elsewhere
 * 
 * @author paul
 * @version $Id$
 * @see TaskRunningService
 */
public class ConflictingTaskException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private TaskCommand attemptedCommand;

    private TaskCommand collidingCommand;

    public ConflictingTaskException( TaskCommand attemptedCommand, TaskCommand collidingCommand ) {
        super();
        this.attemptedCommand = attemptedCommand;
        this.collidingCommand = collidingCommand;
    }

    /**
     * @return the attemptedCommand
     */
    public TaskCommand getAttemptedCommand() {
        return attemptedCommand;
    }

    /**
     * @return the collidingCommand
     */
    public TaskCommand getCollidingCommand() {
        return collidingCommand;
    }

}

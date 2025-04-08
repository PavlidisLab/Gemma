/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.job;

import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * @author anton
 */
public abstract class AbstractTask<C extends TaskCommand> implements Task<C> {

    private C taskCommand;

    public AbstractTask() {
    }

    public AbstractTask( C taskCommand ) {
        Assert.notNull( taskCommand, "The task command cannot be null." );
        this.taskCommand = taskCommand;
    }

    @Override
    public C getTaskCommand() {
        Assert.state( this.taskCommand != null, "No task command provided." );
        return this.taskCommand;
    }

    @Override
    public void setTaskCommand( C taskCommand ) {
        Assert.state( this.taskCommand == null, "The task command can only be set once." );
        Assert.notNull( taskCommand, "The task command cannot be null." );
        this.taskCommand = taskCommand;
    }

    /**
     * Create a new task result for this command with an answer.
     */
    protected TaskResult newTaskResult( @Nullable Serializable answer ) {
        return new TaskResult( taskCommand.getTaskId(), answer );
    }
}

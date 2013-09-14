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
package ubic.gemma.tasks;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;

/**
 * @author anton
 */
public abstract class AbstractTask<T extends TaskResult, C extends TaskCommand> implements Task<T, C> {
    protected C taskCommand;

    public AbstractTask() {
    }

    public AbstractTask( C taskCommand ) {
        assert taskCommand != null;
        this.taskCommand = taskCommand;
    }

    @Override
    public void setTaskCommand( C taskCommand ) {
        assert taskCommand != null;
        this.taskCommand = taskCommand;
    }

    @Override
    public C getTaskCommand() {
        return this.taskCommand;
    }
}

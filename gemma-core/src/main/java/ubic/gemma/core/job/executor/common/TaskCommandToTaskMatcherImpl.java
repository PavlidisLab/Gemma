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
package ubic.gemma.core.job.executor.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.tasks.Task;

/**
 * @author anton date: 08/02/13
 * @vesrion $Id$
 */
@Component
public class TaskCommandToTaskMatcherImpl implements TaskCommandToTaskMatcher {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Task<?, TaskCommand> match( TaskCommand taskCommand ) {
        Class<?> taskClass = taskCommand.getTaskClass();
        if ( taskClass == null )
            throw new IllegalArgumentException( "Task is not set for " + taskCommand.getClass().getSimpleName() );

        // TODO: Try using @Configurable and new operator in the future; could use "new" to get autowiring

        /*
         * Get instance of the bean that allows running the task. For remote tasks this is run on the worker, for local
         * tasks in process.
         */
        @SuppressWarnings("unchecked")
        Task<?, TaskCommand> task = ( Task<?, TaskCommand> ) applicationContext.getBean( taskClass );
        if ( task == null )
            throw new IllegalArgumentException( "Task bean is not found for " + taskClass.getSimpleName() );

        task.setTaskCommand( taskCommand );
        return task;
    }
}
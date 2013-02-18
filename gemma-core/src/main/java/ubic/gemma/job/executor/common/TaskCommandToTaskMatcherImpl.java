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
package ubic.gemma.job.executor.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.tasks.Task;

/**
 * author: anton
 * date: 08/02/13
 */
@Component
public class TaskCommandToTaskMatcherImpl implements TaskCommandToTaskMatcher {

    @Autowired ApplicationContext applicationContext;

    @Override
    public Task match( TaskCommand taskCommand ) {
        Class taskClass = taskCommand.getTaskClass();
        if (taskClass == null) throw new IllegalArgumentException( "Task is not set for "
                + taskCommand.getClass().getSimpleName() );

        // TODO: Try using @Configurable and new operator in the future.
        Task task = (Task) applicationContext.getBean( taskClass );
        if (task == null) throw new IllegalArgumentException( "Task bean is not found for "
                + taskClass.getSimpleName() );

        task.setCommand( taskCommand );
        return task;
    }
}

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
package ubic.gemma.javaspaces.gigaspaces;

import java.io.Serializable;

import net.jini.space.JavaSpace;

/**
 * This command class is used to allow communication of parameters for a task between a client and a compute server in a
 * {@link JavaSpace} Master-Worker environment.
 * <p>
 * This class should be extended to create a command object to pass parameters for a specific task. See
 * {@link JavaSpacesExpressionExperimentLoadCommand}.
 * 
 * @author keshav
 * @version $Id$
 */
public class JavaSpacesCommand implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String taskId = null;

    /**
     * @param taskId
     */
    public JavaSpacesCommand() {
        super();
    }

    /**
     * @param taskId
     */
    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    /**
     * @return
     */
    public String getTaskId() {
        return taskId;
    }

}

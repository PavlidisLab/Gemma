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
package ubic.gemma.grid.javaspaces;

import java.io.Serializable;

/**
 * This class describes the result of the task executed by a worker.
 * <p>
 * The is used in the Master Worker pattern using the GigaSpaces Spring based remote invocation.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesResult implements Serializable {
    /**
     * The task id
     */
    private long taskID; // requestor
    /**
     * The answer
     */
    private Object answer = null; // result

    /**
     * Constructor
     */
    public SpacesResult() {
    }

    public Object getAnswer() {
        return answer;
    }

    public void setAnswer( Object answer ) {
        this.answer = answer;
    }

    public long getTaskID() {
        return taskID;
    }

    public void setTaskID( long taskID ) {
        this.taskID = taskID;
    }
}

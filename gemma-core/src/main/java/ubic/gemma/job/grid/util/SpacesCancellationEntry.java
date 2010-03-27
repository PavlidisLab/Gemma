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
package ubic.gemma.job.grid.util;

/**
 * @author keshav
 * @version $Id$
 */
public class SpacesCancellationEntry extends SpacesGenericEntry {
    public SpacesCancellationEntry() {
    }

    public SpacesCancellationEntry( String workerRegistrationId ) {
        super();
        this.workerRegistrationId = workerRegistrationId;
    }

    public String workerRegistrationId;

    public String taskId = null;

    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setWorkerRegistrationId( String workerRegistrationId ) {
        this.workerRegistrationId = workerRegistrationId;
    }

    public String getWorkerRegistrationId() {
        return workerRegistrationId;
    }

}

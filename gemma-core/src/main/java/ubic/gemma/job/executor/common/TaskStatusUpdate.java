/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.job.executor.common;

import ubic.gemma.job.SubmittedTask;

import java.io.Serializable;
import java.util.Date;

/**
 * TODO: document me
 */
public class TaskStatusUpdate implements Serializable {

    //TODO: maybe add message as well. ex: Cancelled due to running for too long.
    private SubmittedTask.Status status;
    private Date statusChangeTime;

    public TaskStatusUpdate( SubmittedTask.Status status ) {
        this.status = status;
        this.statusChangeTime = new Date();
    }

    public TaskStatusUpdate( SubmittedTask.Status status, Date statusChangeTime ) {
        this.status = status;
        this.statusChangeTime = statusChangeTime;
    }

    public SubmittedTask.Status getStatus() {
        return status;
    }

    public Date getStatusChangeTime() {
        return statusChangeTime;
    }
}

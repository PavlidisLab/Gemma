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
package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.Task;

/**
 * @author anton
 * @vesrion $Id$
 */
public class BioAssayOutlierProcessingTaskCommand extends TaskCommand {
    private boolean revert;

    public boolean isRevert() {
        return revert;
    }

    private Collection<Long> bioAssayIds;

    public Collection<Long> getBioAssayIds() {
        return bioAssayIds;
    }

    public void setBioAssayIds( Collection<Long> bioAssayIds ) {
        this.bioAssayIds = bioAssayIds;
    }

    public BioAssayOutlierProcessingTaskCommand( Collection<Long> ids ) {
        this.setBioAssayIds( ids );
    }

    public BioAssayOutlierProcessingTaskCommand( Collection<Long> ids, boolean revertAsOutlier ) {
        this( ids );
        this.revert = revertAsOutlier;
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return BioAssayOutlierProcessingTask.class;
    }
}

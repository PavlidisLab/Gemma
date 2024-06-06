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
package ubic.gemma.core.tasks.analysis.expression;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;

import java.util.Collection;

/**
 * @author anton
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class BioAssayOutlierProcessingTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 3083077516188614790L;
    private boolean revert;
    private Collection<Long> bioAssayIds;

    public BioAssayOutlierProcessingTaskCommand( Collection<Long> ids ) {
        this.setBioAssayIds( ids );
    }

    public BioAssayOutlierProcessingTaskCommand( Collection<Long> ids, boolean revertAsOutlier ) {
        this( ids );
        this.revert = revertAsOutlier;
    }

    public boolean isRevert() {
        return revert;
    }

    public Collection<Long> getBioAssayIds() {
        return bioAssayIds;
    }

    public void setBioAssayIds( Collection<Long> bioAssayIds ) {
        this.bioAssayIds = bioAssayIds;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return BioAssayOutlierProcessingTask.class;
    }
}

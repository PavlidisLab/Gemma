/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.tasks.analysis.expression;

import ubic.gemma.core.job.Task;
import ubic.gemma.core.job.TaskCommand;

/**
 * @author paul
 *
 */
public class UpdatePubMedCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private Long entityId;
    private String pubmedId;

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return null;
    }

    public UpdatePubMedCommand() {
        super();
    }

    public UpdatePubMedCommand( Long entityId ) {
        this.entityId = entityId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId( Long entityId ) {
        this.entityId = entityId;
    }

    public String getPubmedId() {
        return pubmedId;
    }

    public void setPubmedId( String pubmedId ) {
        this.pubmedId = pubmedId;
    }
}

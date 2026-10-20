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

package ubic.gemma.core.tasks.maintenance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.core.job.Task;
import ubic.gemma.core.job.TaskCommand;

/**
 * Command for the asynchronous mass-indexer task; one boolean per indexable entity root.
 * <p>
 * Mirrors the pre-strip {@code IndexerTaskCommand} (HS 5 era). The field names are
 * deliberately kept in their modern form (e.g. {@code indexDatasets} not {@code indexEE})
 * to match what the HS 5 codebase landed on right before the Phase-2 strip.
 *
 * @author klc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IndexerTaskCommand extends TaskCommand {

    /**
     * Mass indexing can take a long time on a full Gemma dataset (millions of rows
     * across the indexed roots); give the task runner up to 5 hours before it gives up.
     */
    private static final int INDEXER_MAX_RUNTIME_MINUTES = 300;

    private boolean indexPlatforms;
    private boolean indexPublications;
    private boolean indexBioSequences;
    private boolean indexDatasets;
    private boolean indexDatasetGroups;
    private boolean indexGenes;
    private boolean indexGeneGroups;
    private boolean indexDesignElements;

    public IndexerTaskCommand() {
        this.setMaxRuntimeMillis( IndexerTaskCommand.INDEXER_MAX_RUNTIME_MINUTES * 60L * 1000L );
    }

    @Override
    public Class<? extends Task<?>> getTaskClass() {
        return IndexerTask.class;
    }
}

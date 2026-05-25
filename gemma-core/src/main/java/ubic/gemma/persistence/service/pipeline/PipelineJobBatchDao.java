/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.pipeline;

import org.springframework.lang.Nullable;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.persistence.service.BaseDao;

import java.util.List;

public interface PipelineJobBatchDao extends BaseDao<PipelineJobBatch> {

    /**
     * Batches submitted by the given curator, optionally filtered to a single
     * batch state. Ordered by {@code submittedAt} descending.
     *
     * @param contactId the curator's {@code Contact.id}
     * @param state     filter by batch state, or {@code null} for all
     * @param limit     max rows; {@code <= 0} treated as no limit
     */
    List<PipelineJobBatch> findByOwner( Long contactId, @Nullable PipelineJobBatch.BatchState state, int limit );
}

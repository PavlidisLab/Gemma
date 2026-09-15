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
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Date;
import java.util.List;

public interface PipelineJobEventDao extends BaseDao<PipelineJobEvent> {

    /**
     * Events on the given job, ordered by {@code occurredAt} ascending.
     * Used by the live-progress streaming endpoint (SSE) and by post-mortem
     * inspection.
     *
     * @param job   the job
     * @param since cutoff: only events with {@code occurredAt > since}.
     *              Nullable — null returns the full history.
     * @param limit max rows; {@code <= 0} treated as no limit
     */
    List<PipelineJobEvent> findByJob( PipelineJob job, @Nullable Date since, int limit );
}

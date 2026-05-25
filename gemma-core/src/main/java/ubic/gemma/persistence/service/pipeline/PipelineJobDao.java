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
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.SchedulerKind;
import ubic.gemma.persistence.service.BaseDao;

import java.util.List;

public interface PipelineJobDao extends BaseDao<PipelineJob> {

    List<PipelineJob> findByBatch( Long batchId );

    /**
     * Non-terminal jobs targeting the given experiment. Used to gate
     * re-submission ("don't enqueue an EE that already has a running job").
     */
    List<PipelineJob> findActiveByExperiment( Long experimentId );

    /**
     * Look up a job by its scheduler-side handle. Used by the push-callback
     * endpoint when the callback URL carries only the scheduler id (the
     * primary path uses the embedded Gemma-side job id; this is the fallback).
     */
    @Nullable
    PipelineJob findBySchedulerHandle( SchedulerKind kind, String schedulerHandle );

    /**
     * Jobs in {@code state} whose {@code lastEventAt} is older than
     * {@code staleSinceCutoff} (typically now() − staleMinutes). Used by the
     * reconciler poll loop: caller calls {@code scheduler.poll(handle)} for
     * each and reconciles state.
     *
     * @param state            target state (typically {@code QUEUED} or {@code RUNNING})
     * @param staleSinceCutoff jobs with {@code lastEventAt <= cutoff} match
     *                         (jobs with null {@code lastEventAt} also match,
     *                         caller decides if that's interesting)
     * @param limit            cap on rows returned per reconciler tick
     */
    List<PipelineJob> findStaleJobs( JobState state, java.util.Date staleSinceCutoff, int limit );

    /**
     * Convenience for the batch service: jobs in the batch in the given
     * states. Used to derive batch-level state transitions.
     */
    List<PipelineJob> findByBatchAndStates( PipelineJobBatch batch, java.util.Collection<JobState> states );
}

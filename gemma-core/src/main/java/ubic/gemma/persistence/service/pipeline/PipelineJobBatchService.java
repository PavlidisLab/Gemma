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
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.PipelineJobEvent;

import java.util.Collection;
import java.util.List;

/**
 * Curator-driven pipeline batch submissions. One submit call creates a
 * {@link PipelineJobBatch} with N child {@link PipelineJob} rows (one per
 * experiment) and dispatches each child to the configured
 * {@link ubic.gemma.core.pipeline.PipelineScheduler}.
 *
 * <p>Push callbacks from the scheduler-side pipeline land on the internal
 * REST endpoint and call {@link #recordEvent}.</p>
 */
public interface PipelineJobBatchService {

    /**
     * Create a batch + one job per experiment, dispatch each to the configured
     * scheduler. Returns the batch with children attached.
     *
     * @param pipeline   pipeline name the scheduler will resolve
     * @param experiments experiments to run; one job per
     * @param paramsJson  pipeline-specific params, serialized (caller's
     *                    responsibility to version)
     * @param note        optional curator-supplied note
     */
    PipelineJobBatch submit( String pipeline, Collection<ExpressionExperiment> experiments,
            Contact submittedBy, @Nullable String paramsJson, @Nullable String note );

    PipelineJobBatch get( Long batchId );

    List<PipelineJobBatch> findByOwner( Long contactId, @Nullable PipelineJobBatch.BatchState state, int limit );

    /**
     * Request cancellation of every non-terminal job in the batch. Each job's
     * state moves to {@code CANCELLING}; the scheduler ack moves it to
     * {@code CANCELLED}.
     */
    void cancelBatch( Long batchId );

    /**
     * Request cancellation of one job mid-batch.
     */
    void cancelJob( Long jobId );

    /**
     * Append an event from the scheduler-side pipeline. Updates job state
     * derived from the event kind (completed → DONE, error → FAILED, killed
     * → CANCELLED, progress → no state change, stage → no state change).
     * Returns the persisted event.
     */
    PipelineJobEvent recordEvent( Long jobId, String kind, @Nullable String payloadJson );

    /**
     * For the reconciler: jobs in non-terminal state whose {@code lastEventAt}
     * is older than {@code staleMinutes}. The reconciler iterates these and
     * calls {@code scheduler.poll(handle)} for each.
     */
    List<PipelineJob> findStaleJobs( int staleMinutes, int limit );
}

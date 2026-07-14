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
package ubic.gemma.core.pipeline;

import ubic.gemma.model.pipeline.SchedulerKind;

/**
 * Abstraction over an external pipeline scheduler (Luigi or Nextflow today;
 * a different impl tomorrow if needed). Switching schedulers is a
 * Spring-profile flip, not a code change.
 *
 * <p>Each impl is responsible for:</p>
 * <ul>
 *   <li>Translating Gemma's pipeline name + params into the scheduler's
 *       wire shape.</li>
 *   <li>Round-tripping a Gemma-side job id so push callbacks can identify
 *       the originating row.</li>
 *   <li>Mapping scheduler-side state back to {@link ubic.gemma.model.pipeline.JobState}
 *       in {@link #poll(SchedulerHandle)} (used by the reconciler when push
 *       events are missing).</li>
 * </ul>
 *
 * <p>Push notifications from the pipeline land at the internal events
 * endpoint and are written directly by the service layer, NOT by the
 * scheduler impl — the impl is only the dispatch + poll + cancel side.</p>
 */
public interface PipelineScheduler {

    /**
     * Which scheduler is this. Persisted on {@code PipelineJob.schedulerKind};
     * used to disambiguate {@link SchedulerHandle}s in storage.
     */
    SchedulerKind kind();

    /**
     * Submit one job to the scheduler. The returned {@link SchedulerHandle}'s
     * {@code id} is the scheduler-side primary key (Luigi task id, Nextflow
     * workflow id, etc.) and is stored on {@code PipelineJob.schedulerHandle}.
     *
     * @throws PipelineSchedulerException if submission fails (network, auth,
     *                                    quota, malformed payload, etc.).
     *                                    Caller maps to {@code JobState.FAILED}
     *                                    with the message persisted to
     *                                    {@code PipelineJob.errorMessage}.
     */
    SchedulerHandle submit( SubmitRequest req ) throws PipelineSchedulerException;

    /**
     * Best-effort fetch of current scheduler-side state. Used by the
     * {@code @Scheduled} reconciler when a job has gone too long without
     * a push event. Returns {@code null} if the scheduler doesn't recognize
     * the handle (job was purged from scheduler-side history, etc.) — caller
     * treats {@code null} as terminal-unknown and surfaces the gap.
     */
    JobSnapshot poll( SchedulerHandle handle ) throws PipelineSchedulerException;

    /**
     * Request cancellation of a running job. Cooperative — the scheduler
     * acknowledges; the job goes to {@link ubic.gemma.model.pipeline.JobState#CANCELLING}
     * pending the next push event or poll confirming {@code CANCELLED}.
     */
    void cancel( SchedulerHandle handle ) throws PipelineSchedulerException;

    // -----------------------------------------------------------------------
    // Optional capabilities (§3.5). Default = unsupported; only schedulers that
    // can serve logs / output files from the runtime workdir override these.
    // The service proxies through them; nothing is persisted in Gemma.
    // -----------------------------------------------------------------------

    /** Whether this scheduler can serve job logs via {@link #readLog}. */
    default boolean supportsLog() {
        return false;
    }

    /**
     * Read an incremental slice of a job's log — the bytes {@code [offset, offset+limit)} decoded as
     * text, plus the cursor to continue from. Enables {@code tail -f} without re-fetching (§3.5).
     *
     * @throws UnsupportedOperationException if {@link #supportsLog()} is false
     */
    default LogChunk readLog( SchedulerHandle handle, long offset, int limit ) throws PipelineSchedulerException {
        throw new UnsupportedOperationException( "readLog not supported by scheduler " + kind() );
    }

    /** Whether this scheduler can serve output artifacts via {@link #readArtifact}. */
    default boolean supportsArtifacts() {
        return false;
    }

    /**
     * Stream a whitelisted output file from the job's workdir (e.g. {@code web_summary.html}). The
     * caller is responsible for rejecting unsafe / non-whitelisted names before calling.
     *
     * @throws UnsupportedOperationException if {@link #supportsArtifacts()} is false
     */
    default Artifact readArtifact( SchedulerHandle handle, String name ) throws PipelineSchedulerException {
        throw new UnsupportedOperationException( "readArtifact not supported by scheduler " + kind() );
    }
}

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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.pipeline.PipelineScheduler;
import ubic.gemma.core.pipeline.PipelineSchedulerException;
import ubic.gemma.core.pipeline.SchedulerHandle;
import ubic.gemma.core.pipeline.SubmitRequest;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedPipelineRunEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineBatchCancelledEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineBatchClosedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineBatchSubmittedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineRunEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.BatchRollup;
import ubic.gemma.model.pipeline.FailureClass;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
public class PipelineJobBatchServiceImpl implements PipelineJobBatchService {

    private static final EnumSet<JobState> ACTIVE_STATES = EnumSet.of(
            JobState.PENDING, JobState.QUEUED, JobState.RUNNING, JobState.CANCELLING );

    /**
     * Event kinds that reflect ephemeral live operational state (progress %, reconciler
     * heartbeats). Under the delegated model (§3.1) the orchestrator owns this, so Gemma
     * records it only in the overwrite-in-place snapshot columns, never as an append-only
     * {@code PIPELINE_JOB_EVENT} row. Everything not listed here is a durable milestone.
     */
    private static final java.util.Set<String> SNAPSHOT_ONLY_KINDS = java.util.Set.of( "progress", "heartbeat" );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PipelineJobBatchDao batchDao;

    @Autowired
    private PipelineJobDao jobDao;

    @Autowired
    private PipelineJobEventDao eventDao;

    @Autowired
    private AuditTrailService auditTrailService;

    /**
     * Optional. Wired only when one of the {@code scheduler-*} Spring profiles
     * is active; otherwise null and {@link #submit} fails fast on the missing
     * scheduler. Tests inject a mock.
     */
    @Autowired(required = false)
    private PipelineScheduler scheduler;

    @Override
    @Transactional
    public PipelineJobBatch submit( String pipeline, Collection<ExpressionExperiment> experiments,
            Contact submittedBy, @Nullable String paramsJson, @Nullable String note ) {
        if ( experiments.isEmpty() ) {
            throw new IllegalArgumentException( "submit requires at least one experiment" );
        }
        if ( submittedBy == null ) {
            throw new IllegalArgumentException( "submittedBy is required" );
        }
        if ( scheduler == null ) {
            throw new IllegalStateException( "no PipelineScheduler bean configured; activate a scheduler-* Spring profile" );
        }
        PipelineJobBatch batch = new PipelineJobBatch();
        batch.setName( buildBatchName( pipeline, experiments.size() ) );
        batch.setDescription( note );
        batch.setPipeline( pipeline );
        batch.setSubmittedBy( submittedBy );
        batch.setSubmittedAt( new Date() );
        batch.setParamsJson( paramsJson );
        batch.setState( PipelineJobBatch.BatchState.OPEN );
        // Create children before persisting the batch so the cascade picks them up.
        for ( ExpressionExperiment ee : experiments ) {
            PipelineJob job = new PipelineJob();
            job.setBatch( batch );
            job.setExperiment( ee );
            job.setState( JobState.PENDING );
            batch.getJobs().add( job );
        }
        batch = batchDao.save( batch );
        // Dispatch each child. The scheduler is opaque; we just need its handle.
        for ( PipelineJob job : batch.getJobs() ) {
            dispatchOne( job, pipeline, paramsJson );
        }
        // Typed AuditEvent on the batch's own trail (the @Audited annotation
        // can't fire on submit() because the auditable doesn't exist as an
        // argument — it's the return value).
        auditTrailService.addUpdateEvent( batch, PipelineBatchSubmittedEvent.class,
                "Submitted " + experiments.size() + " jobs to pipeline '" + pipeline + "'" );
        return batch;
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineJobBatch get( Long batchId ) {
        return batchDao.load( batchId );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineJobBatch> findByOwner( Long contactId, @Nullable PipelineJobBatch.BatchState state, int limit ) {
        return batchDao.findByOwner( contactId, state, limit );
    }

    @Override
    @Transactional
    public void cancelBatch( Long batchId ) {
        PipelineJobBatch batch = batchDao.load( batchId );
        if ( batch == null ) {
            throw new IllegalArgumentException( "no batch " + batchId );
        }
        batch.setKillRequestedAt( new Date() );
        List<PipelineJob> active = jobDao.findByBatchAndStates( batch, ACTIVE_STATES );
        for ( PipelineJob job : active ) {
            requestSchedulerCancel( job );
            transitionTo( job, JobState.CANCELLING, "batch cancel" );
        }
        batchDao.update( batch );
        // @Audited can't fire here — first arg is Long, not Auditable. Emit
        // imperatively (same pattern as cancelJob).
        auditTrailService.addUpdateEvent( batch, PipelineBatchCancelledEvent.class,
                "Batch cancellation requested (" + active.size() + " active jobs)" );
    }

    @Override
    @Transactional
    public void cancelJob( Long jobId ) {
        PipelineJob job = jobDao.load( jobId );
        if ( job == null ) {
            throw new IllegalArgumentException( "no job " + jobId );
        }
        if ( job.getState().isTerminal() ) {
            log.info( "cancelJob({}): already terminal in state {}; no-op", jobId, job.getState() );
            return;
        }
        requestSchedulerCancel( job );
        transitionTo( job, JobState.CANCELLING, "job cancel" );
        // Emit a batch-level audit event for the timeline; @Audited can't fire on
        // cancelJob because the first arg is a Long, not the Auditable.
        auditTrailService.addUpdateEvent( job.getBatch(), PipelineBatchCancelledEvent.class,
                "Cancelled job " + jobId + " (EE " + job.getExperiment().getId() + ")" );
    }

    @Override
    @Transactional
    public PipelineJobEvent recordEvent( Long jobId, String kind, @Nullable String payloadJson ) {
        PipelineJob job = jobDao.load( jobId );
        if ( job == null ) {
            throw new IllegalArgumentException( "no job " + jobId );
        }
        Date now = new Date();
        PipelineJobEvent event = new PipelineJobEvent();
        event.setJob( job );
        event.setOccurredAt( now );
        event.setKind( kind );
        event.setPayloadJson( payloadJson );
        // Delegated model (§3.1): high-churn kinds mirror live operational state the
        // runtime already owns — they update the overwrite-in-place snapshot but are NOT
        // persisted as rows. Everything else (stage, completed, error, killed, unknown) is
        // a durable milestone/terminal fact and gets a row. Returned event is transient
        // (null id) for snapshot-only kinds — the caller still gets the recorded shape back.
        if ( !SNAPSHOT_ONLY_KINDS.contains( kind ) ) {
            event = eventDao.create( event );
        }
        // Overwrite-in-place snapshot — always, for every kind (keeps the UI's live view
        // current and bumps LAST_EVENT_AT so the reconciler won't immediately re-poll).
        job.setLastEventAt( now );
        job.setLastEventKind( kind );
        if ( "progress".equals( kind ) || "stage".equals( kind ) ) {
            job.setLastProgressJson( payloadJson );
            if ( job.getState() == JobState.QUEUED ) {
                job.setState( JobState.RUNNING );
                job.setStartedAt( now );
            }
        }
        if ( "completed".equals( kind ) ) {
            terminate( job, JobState.DONE, now, null );
        } else if ( "error".equals( kind ) ) {
            job.setFailureClass( parseFailureClass( payloadJson ) );
            terminate( job, JobState.FAILED, now, payloadJson );
        } else if ( "killed".equals( kind ) ) {
            terminate( job, JobState.CANCELLED, now, null );
        }
        jobDao.update( job );
        maybeCloseBatch( job.getBatch() );
        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineJobEvent> findEvents( Long jobId, @Nullable Date since, int limit ) {
        PipelineJob job = jobDao.load( jobId );
        if ( job == null ) {
            return Collections.emptyList();
        }
        return eventDao.findByJob( job, since, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineJob> findStaleJobs( int staleMinutes, int limit ) {
        Date cutoff = new Date( System.currentTimeMillis() - ( long ) staleMinutes * 60_000L );
        // Two non-terminal states get polled: QUEUED + RUNNING. PENDING is pre-
        // dispatch (we own the timing). CANCELLING is fine to poll too; the
        // scheduler ack will land via the next push event in normal operation.
        List<PipelineJob> result = new ArrayList<>();
        for ( JobState s : Arrays.asList( JobState.QUEUED, JobState.RUNNING, JobState.CANCELLING ) ) {
            if ( result.size() >= limit && limit > 0 ) break;
            int remaining = limit > 0 ? limit - result.size() : Integer.MAX_VALUE;
            result.addAll( jobDao.findStaleJobs( s, cutoff, remaining ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchRollup computeRollup( Long batchId ) {
        PipelineJobBatch batch = batchDao.load( batchId );
        if ( batch == null ) {
            throw new IllegalArgumentException( "no batch " + batchId );
        }
        BatchRollup r = new BatchRollup();
        int failedCurrent = 0;
        boolean allTerminal = true;
        for ( PipelineJob job : currentAttempts( batchId ) ) {
            r.total++;
            if ( !job.getState().isTerminal() ) {
                allTerminal = false;
            }
            switch ( job.getState() ) {
                case PENDING: r.pending++; break;
                case QUEUED: r.queued++; break;
                case RUNNING: r.running++; break;
                case DONE: r.done++; break;
                case CANCELLED: r.cancelled++; break;
                case FAILED:
                    r.failed++;
                    failedCurrent++;
                    if ( job.getFailureClass() == FailureClass.TRANSIENT ) {
                        r.failedRetryable++;
                    } else {
                        r.failedPermanent++;
                    }
                    break;
                case CANCELLING: break; // active; contributes to total + allTerminal=false only
            }
        }
        r.needsAttention = batch.getState() == PipelineJobBatch.BatchState.OPEN && failedCurrent > 0;
        r.terminal = r.total > 0 && allTerminal;
        return r;
    }

    @Override
    @Transactional
    public BatchRollup retryFailed( Long batchId, RetrySpec spec ) {
        if ( scheduler == null ) {
            throw new IllegalStateException( "no PipelineScheduler bean configured; activate a scheduler-* Spring profile" );
        }
        if ( spec == null ) {
            spec = new RetrySpec();
        }
        PipelineJobBatch batch = batchDao.load( batchId );
        if ( batch == null ) {
            throw new IllegalArgumentException( "no batch " + batchId );
        }
        int minted = 0;
        for ( PipelineJob job : currentAttempts( batchId ) ) {
            if ( job.getState() != JobState.FAILED ) {
                continue;
            }
            if ( spec.onlyRetryable && job.getFailureClass() != FailureClass.TRANSIENT ) {
                continue;
            }
            if ( spec.jobIds != null && !spec.jobIds.contains( job.getId() ) ) {
                continue;
            }
            mintRetry( job, spec.paramsOverrideJson );
            minted++;
        }
        if ( minted > 0 ) {
            reopenIfClosed( batch );
        }
        log.info( "retryFailed(batch {}): minted {} retries", batchId, minted );
        return computeRollup( batchId );
    }

    @Override
    @Transactional
    public BatchRollup retryJob( Long jobId, RetrySpec spec ) {
        if ( scheduler == null ) {
            throw new IllegalStateException( "no PipelineScheduler bean configured; activate a scheduler-* Spring profile" );
        }
        if ( spec == null ) {
            spec = new RetrySpec();
        }
        PipelineJob job = jobDao.load( jobId );
        if ( job == null ) {
            throw new IllegalArgumentException( "no job " + jobId );
        }
        Long batchId = job.getBatch().getId();
        // Idempotency: only the current attempt is retryable; a job that's already superseded
        // means a retry is in flight (or done) — no-op.
        if ( job.getSupersededBy() != null ) {
            log.info( "retryJob({}): already superseded by job {}; no-op", jobId, job.getSupersededBy().getId() );
            return computeRollup( batchId );
        }
        if ( !job.getState().isTerminal() ) {
            throw new IllegalArgumentException( "job " + jobId + " is not terminal (" + job.getState() + "); nothing to retry" );
        }
        mintRetry( job, spec.paramsOverrideJson );
        reopenIfClosed( job.getBatch() );
        return computeRollup( batchId );
    }

    // -----------------------------------------------------------------------
    // internals
    // -----------------------------------------------------------------------

    /** Current attempts of a batch: the rows not yet superseded by a retry. */
    private List<PipelineJob> currentAttempts( Long batchId ) {
        List<PipelineJob> current = new ArrayList<>();
        for ( PipelineJob job : jobDao.findByBatch( batchId ) ) {
            if ( job.getSupersededBy() == null ) {
                current.add( job );
            }
        }
        return current;
    }

    /**
     * Mint attempt N+1 for the same (batch, experiment), link it to its predecessor, and dispatch.
     * The old job is left terminal/immutable — only its {@code supersededBy} pointer is set.
     */
    private PipelineJob mintRetry( PipelineJob old, @Nullable String paramsOverrideJson ) {
        PipelineJobBatch batch = old.getBatch();
        String params = paramsOverrideJson != null ? paramsOverrideJson : old.getParamsJson();
        PipelineJob attempt = new PipelineJob();
        attempt.setBatch( batch );
        attempt.setExperiment( old.getExperiment() );
        attempt.setState( JobState.PENDING );
        attempt.setAttempt( old.getAttempt() + 1 );
        attempt.setRetryOf( old );
        attempt.setParamsJson( params );
        // Persist directly — NOT via batch.getJobs().add(): the (batch,ee) business-key equals
        // would collide the transient new attempt with the frozen predecessor in the Set.
        attempt = jobDao.create( attempt );
        old.setSupersededBy( attempt );
        jobDao.update( old );
        dispatchOne( attempt, batch.getPipeline(), params );
        return attempt;
    }

    private void reopenIfClosed( PipelineJobBatch batch ) {
        if ( batch.getState() == PipelineJobBatch.BatchState.CLOSED ) {
            batch.setState( PipelineJobBatch.BatchState.OPEN );
            batch.setClosedAt( null );
            batchDao.update( batch );
        }
    }

    /** Pipeline-reported failure class from the {@code error} payload; UNKNOWN if absent/unparseable (D9). */
    private static FailureClass parseFailureClass( @Nullable String payloadJson ) {
        if ( payloadJson == null || payloadJson.isBlank() ) {
            return FailureClass.UNKNOWN;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree( payloadJson );
            JsonNode fc = node.get( "failureClass" );
            if ( fc != null && fc.isTextual() ) {
                return FailureClass.valueOf( fc.asText().trim().toUpperCase( java.util.Locale.ROOT ) );
            }
        } catch ( Exception e ) {
            log.debug( "could not parse failureClass from error payload: {}", e.getMessage() );
        }
        return FailureClass.UNKNOWN;
    }

    private void dispatchOne( PipelineJob job, String pipeline, @Nullable String paramsJson ) {
        // Record the params this attempt runs with (per-attempt provenance).
        job.setParamsJson( paramsJson );
        try {
            SchedulerHandle handle = scheduler.submit( new SubmitRequest(
                    job.getId(), pipeline, job.getExperiment().getId(), paramsJson ) );
            job.setSchedulerKind( handle.getKind() );
            job.setSchedulerHandle( handle.getId() );
            job.setState( JobState.QUEUED );
            job.setSubmittedAt( new Date() );
            job.setLastEventAt( job.getSubmittedAt() );
            job.setLastEventKind( "submitted" );
            jobDao.update( job );
        } catch ( PipelineSchedulerException e ) {
            log.warn( "scheduler.submit failed for job {}: {}", job.getId(), e.getMessage() );
            job.setState( JobState.FAILED );
            job.setErrorMessage( "dispatch failed: " + e.getMessage() );
            job.setFinishedAt( new Date() );
            jobDao.update( job );
            writeTerminalEeEvent( job, FailedPipelineRunEvent.class, "Dispatch to scheduler failed: " + e.getMessage() );
        }
    }

    private void requestSchedulerCancel( PipelineJob job ) {
        if ( job.getSchedulerKind() == null || job.getSchedulerHandle() == null ) {
            // never dispatched (PENDING or dispatch-failed). Nothing to cancel scheduler-side.
            return;
        }
        try {
            scheduler.cancel( new SchedulerHandle( job.getSchedulerKind(), job.getSchedulerHandle() ) );
        } catch ( PipelineSchedulerException e ) {
            log.warn( "scheduler.cancel failed for job {}: {}", job.getId(), e.getMessage() );
            // Don't propagate — the cancel is best-effort. The job stays in
            // CANCELLING; the reconciler poll loop will retry.
        }
    }

    private void transitionTo( PipelineJob job, JobState target, String reason ) {
        log.debug( "job {} transition {} -> {} ({})", job.getId(), job.getState(), target, reason );
        job.setState( target );
        if ( target.isTerminal() ) {
            if ( job.getFinishedAt() == null ) job.setFinishedAt( new Date() );
        }
        jobDao.update( job );
    }

    private void terminate( PipelineJob job, JobState target, Date when, @Nullable String detail ) {
        job.setState( target );
        job.setFinishedAt( when );
        if ( target == JobState.FAILED && detail != null ) {
            job.setErrorMessage( detail );
        }
        // Typed AuditEvent on the EXPERIMENT — durable provenance.
        Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType> eventClass =
                target == JobState.DONE ? PipelineRunEvent.class : FailedPipelineRunEvent.class;
        writeTerminalEeEvent( job, eventClass, buildEeEventNote( job, target ) );
    }

    private void writeTerminalEeEvent( PipelineJob job,
            Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType> eventClass,
            String note ) {
        ExpressionExperiment ee = job.getExperiment();
        if ( ee == null ) return;
        auditTrailService.addUpdateEvent( ee, eventClass, note );
    }

    private String buildEeEventNote( PipelineJob job, JobState target ) {
        return String.format( "Pipeline '%s' (batch %d, job %d) %s",
                job.getBatch() != null ? job.getBatch().getPipeline() : "?",
                job.getBatch() != null && job.getBatch().getId() != null ? job.getBatch().getId() : 0L,
                job.getId() != null ? job.getId() : 0L,
                target.name() );
    }

    private void maybeCloseBatch( PipelineJobBatch batch ) {
        if ( batch == null || batch.getState() != PipelineJobBatch.BatchState.OPEN ) {
            return;
        }
        boolean anyActive = false, anyFailed = false;
        int current = 0;
        for ( PipelineJob job : currentAttempts( batch.getId() ) ) {
            current++;
            if ( ACTIVE_STATES.contains( job.getState() ) ) {
                anyActive = true;
            }
            if ( job.getState() == JobState.FAILED ) {
                anyFailed = true;
            }
        }
        // Don't close if work is still in flight, or if a FAILED current attempt is awaiting
        // mop-up: §3.2 — a batch is not "done" just because every job reached a terminal state;
        // failures keep it OPEN (needs-attention) until retried or the curator closes it.
        if ( anyActive || anyFailed ) {
            return;
        }
        batch.setState( PipelineJobBatch.BatchState.CLOSED );
        batch.setClosedAt( new Date() );
        batchDao.update( batch );
        auditTrailService.addUpdateEvent( batch, PipelineBatchClosedEvent.class,
                "All " + current + " current attempts terminal without failures" );
    }

    private String buildBatchName( String pipeline, int n ) {
        return pipeline + " batch of " + n + " (" + new java.text.SimpleDateFormat( "yyyy-MM-dd HH:mm" ).format( new Date() ) + ")";
    }
}

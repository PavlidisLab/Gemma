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
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedPipelineRunEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineBatchCancelledEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineBatchClosedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineBatchSubmittedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PipelineRunEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

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
    @Audited(value = PipelineBatchCancelledEvent.class, message = "Batch cancellation requested")
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
        PipelineJobEvent event = new PipelineJobEvent();
        event.setJob( job );
        event.setOccurredAt( new Date() );
        event.setKind( kind );
        event.setPayloadJson( payloadJson );
        event = eventDao.create( event );
        job.setLastEventAt( event.getOccurredAt() );
        job.setLastEventKind( kind );
        if ( "progress".equals( kind ) || "stage".equals( kind ) ) {
            job.setLastProgressJson( payloadJson );
            if ( job.getState() == JobState.QUEUED ) {
                job.setState( JobState.RUNNING );
                job.setStartedAt( event.getOccurredAt() );
            }
        }
        if ( "completed".equals( kind ) ) {
            terminate( job, JobState.DONE, event.getOccurredAt(), null );
        } else if ( "error".equals( kind ) ) {
            terminate( job, JobState.FAILED, event.getOccurredAt(), payloadJson );
        } else if ( "killed".equals( kind ) ) {
            terminate( job, JobState.CANCELLED, event.getOccurredAt(), null );
        }
        jobDao.update( job );
        maybeCloseBatch( job.getBatch() );
        return event;
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

    // -----------------------------------------------------------------------
    // internals
    // -----------------------------------------------------------------------

    private void dispatchOne( PipelineJob job, String pipeline, @Nullable String paramsJson ) {
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
        List<PipelineJob> stillActive = jobDao.findByBatchAndStates( batch, ACTIVE_STATES );
        if ( !stillActive.isEmpty() ) {
            return;
        }
        // All children terminal. Close the batch + emit milestone event.
        batch.setState( PipelineJobBatch.BatchState.CLOSED );
        batch.setClosedAt( new Date() );
        batchDao.update( batch );
        auditTrailService.addUpdateEvent( batch, PipelineBatchClosedEvent.class,
                "All " + batch.getJobs().size() + " jobs reached terminal state" );
    }

    private String buildBatchName( String pipeline, int n ) {
        return pipeline + " batch of " + n + " (" + new java.text.SimpleDateFormat( "yyyy-MM-dd HH:mm" ).format( new Date() ) + ")";
    }
}

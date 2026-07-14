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
package ubic.gemma.model.pipeline;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * One pipeline run against one experiment, dispatched to an external scheduler.
 *
 * <p>{@link #schedulerKind} and {@link #schedulerHandle} together are the
 * scheduler-side primary key — opaque to Gemma, populated when the dispatch call
 * returns. They stay null while the job is {@link JobState#PENDING}.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "PIPELINE_JOB", indexes = {
        @Index(name = "IDX_PIPELINE_JOB_BATCH", columnList = "BATCH_FK"),
        @Index(name = "IDX_PIPELINE_JOB_EXPERIMENT", columnList = "EXPERIMENT_FK"),
        @Index(name = "IDX_PIPELINE_JOB_STATE", columnList = "STATE"),
        @Index(name = "IDX_PIPELINE_JOB_LAST_EVENT", columnList = "LAST_EVENT_AT")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PipelineJob extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BATCH_FK", nullable = false, columnDefinition = "BIGINT")
    private PipelineJobBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXPERIMENT_FK", nullable = false, columnDefinition = "BIGINT")
    private ExpressionExperiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE", nullable = false, columnDefinition = "VARCHAR(16)")
    private JobState state = JobState.PENDING;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "SCHEDULER_KIND", columnDefinition = "VARCHAR(16)")
    private SchedulerKind schedulerKind;

    @Nullable
    @Column(name = "SCHEDULER_HANDLE", columnDefinition = "VARCHAR(255)")
    private String schedulerHandle;

    @Nullable
    @Column(name = "SUBMITTED_AT", columnDefinition = "DATETIME(3)")
    private Date submittedAt;

    @Nullable
    @Column(name = "STARTED_AT", columnDefinition = "DATETIME(3)")
    private Date startedAt;

    @Nullable
    @Column(name = "FINISHED_AT", columnDefinition = "DATETIME(3)")
    private Date finishedAt;

    @Nullable
    @Column(name = "LAST_EVENT_AT", columnDefinition = "DATETIME(3)")
    private Date lastEventAt;

    @Nullable
    @Column(name = "LAST_EVENT_KIND", columnDefinition = "VARCHAR(32)")
    private String lastEventKind;

    @Lob
    @Nullable
    @Column(name = "LAST_PROGRESS_JSON", columnDefinition = "text")
    private String lastProgressJson;

    @Lob
    @Nullable
    @Column(name = "ERROR_MESSAGE", columnDefinition = "text")
    private String errorMessage;

    // -----------------------------------------------------------------------
    // Attempt chain (§3.2): a retry mints a NEW PipelineJob for the same
    // (batch, experiment); the failed job is immutable history. The current
    // attempt for a (batch, ee) is the row with supersededBy == null.
    // -----------------------------------------------------------------------

    /** 1-based attempt number for this (batch, experiment); denormalized for display + sort. */
    @Column(name = "ATTEMPT", nullable = false, columnDefinition = "INT")
    private int attempt = 1;

    /** The previous attempt this one retries (null on the first attempt). Walk back for the chain. */
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RETRY_OF_FK", columnDefinition = "BIGINT")
    private PipelineJob retryOf;

    /**
     * The retry that replaced this attempt (null while this is the current attempt). Monotonic —
     * set once, never cleared — so {@code supersededBy == null} is an O(1) is-current check.
     */
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUPERSEDED_BY_FK", columnDefinition = "BIGINT")
    private PipelineJob supersededBy;

    /** Failure classification (set on FAILED); drives auto-retry eligibility. */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "FAILURE_CLASS", columnDefinition = "VARCHAR(16)")
    private FailureClass failureClass;

    /** Params this attempt was dispatched with — per-attempt provenance ("bumped mem", swapped accession). */
    @Lob
    @Nullable
    @Column(name = "PARAMS_JSON", columnDefinition = "longtext")
    private String paramsJson;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt")
    private Set<PipelineJobEvent> events = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(
                batch != null ? batch.getId() : null,
                experiment != null ? experiment.getId() : null );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) return true;
        if ( !( object instanceof PipelineJob ) ) return false;
        PipelineJob that = ( PipelineJob ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return Objects.equals(
                this.batch != null ? this.batch.getId() : null,
                that.batch != null ? that.batch.getId() : null )
                && Objects.equals(
                this.experiment != null ? this.experiment.getId() : null,
                that.experiment != null ? that.experiment.getId() : null );
    }
}

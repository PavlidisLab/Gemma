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

import jakarta.persistence.AttributeOverride;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * One curator-initiated submission of a pipeline against a set of experiments.
 *
 * <p>A batch lives even after all child {@link PipelineJob}s reach a terminal state,
 * so the curator can review the run history. {@code state} on the batch is a coarse
 * lifecycle marker (OPEN while jobs remain non-terminal, CLOSED when all are
 * terminal, CANCELLED if curator clicked batch-wide cancel).</p>
 */
@Getter
@Setter
@Entity
@Table(name = "PIPELINE_JOB_BATCH", indexes = {
        @Index(name = "IDX_PIPELINE_JOB_BATCH_PIPELINE_STATE", columnList = "PIPELINE,STATE"),
        @Index(name = "IDX_PIPELINE_JOB_BATCH_SUBMITTED_BY", columnList = "SUBMITTED_BY_FK")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@AttributeOverride(name = "name", column = @Column(name = "NAME", nullable = false, columnDefinition = "VARCHAR(255)"))
public class PipelineJobBatch extends AbstractAuditable {

    /**
     * Coarse batch lifecycle. Distinct from child {@link JobState} — a batch can be
     * {@code OPEN} with some children {@code DONE} and others still {@code RUNNING}.
     */
    public enum BatchState {
        OPEN,
        CLOSED,
        CANCELLED
    }

    @Column(name = "PIPELINE", nullable = false, columnDefinition = "VARCHAR(64)")
    private String pipeline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUBMITTED_BY_FK", nullable = false, columnDefinition = "BIGINT")
    private Contact submittedBy;

    @Column(name = "SUBMITTED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date submittedAt = new Date();

    @Lob
    @Nullable
    @Column(name = "PARAMS_JSON", columnDefinition = "longtext")
    private String paramsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE", nullable = false, columnDefinition = "VARCHAR(16)")
    private BatchState state = BatchState.OPEN;

    @Nullable
    @Column(name = "KILL_REQUESTED_AT", columnDefinition = "DATETIME(3)")
    private Date killRequestedAt;

    @Nullable
    @Column(name = "CLOSED_AT", columnDefinition = "DATETIME(3)")
    private Date closedAt;

    // free-form curator note lives on AbstractDescribable.description (inherited)
    // human-readable title (e.g. "RNA-seq batch of 100 EEs 2026-05-24") lives on .name

    @OneToMany(mappedBy = "batch", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PipelineJob> jobs = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash( pipeline, submittedAt );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) return true;
        if ( !( object instanceof PipelineJobBatch ) ) return false;
        PipelineJobBatch that = ( PipelineJobBatch ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return Objects.equals( this.pipeline, that.pipeline )
                && Objects.equals( this.submittedAt, that.submittedAt )
                && Objects.equals(
                this.submittedBy != null ? this.submittedBy.getId() : null,
                that.submittedBy != null ? that.submittedBy.getId() : null );
    }
}

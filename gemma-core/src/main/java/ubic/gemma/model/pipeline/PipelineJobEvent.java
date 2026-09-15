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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Date;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One progress / state / log record reported by the scheduler-side pipeline.
 *
 * <p>{@link #kind} is a free-form string (not an enum) so new event kinds can be
 * added by the scheduler/pipeline without a Gemma migration. The consumer side
 * coerces unknown kinds to a default rendering.</p>
 *
 * <p>Conventional kinds: {@code progress}, {@code stage}, {@code stderr},
 * {@code killed}, {@code error}, {@code completed}.</p>
 */
@Entity
@Table(name = "PIPELINE_JOB_EVENT",
        indexes = @Index(name = "IDX_PIPELINE_JOB_EVENT_JOB_AT", columnList = "JOB_FK, OCCURRED_AT"))
@Getter
@Setter
public class PipelineJobEvent extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    // V18 declares FK_PIPELINE_JOB_EVENT_JOB ON DELETE CASCADE; keep the mapping in step so a Hibernate-generated schema cascades too
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "JOB_FK", nullable = false, columnDefinition = "BIGINT")
    private PipelineJob job;

    @Column(name = "OCCURRED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date occurredAt = new Date();

    @Column(name = "KIND", nullable = false, columnDefinition = "VARCHAR(32)")
    private String kind;

    @Lob
    @Nullable
    @Column(name = "PAYLOAD_JSON", columnDefinition = "text")
    private String payloadJson;

    @Override
    public int hashCode() {
        return Objects.hash(
                job != null ? job.getId() : null,
                occurredAt,
                kind );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) return true;
        if ( !( object instanceof PipelineJobEvent ) ) return false;
        PipelineJobEvent that = ( PipelineJobEvent ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return Objects.equals(
                this.job != null ? this.job.getId() : null,
                that.job != null ? that.job.getId() : null )
                && Objects.equals( this.occurredAt, that.occurredAt )
                && Objects.equals( this.kind, that.kind );
    }
}

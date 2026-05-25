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

import lombok.Getter;
import lombok.Setter;
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

    private String pipeline;

    private Contact submittedBy;

    private Date submittedAt = new Date();

    @Nullable
    private String paramsJson;

    private BatchState state = BatchState.OPEN;

    @Nullable
    private Date killRequestedAt;

    @Nullable
    private Date closedAt;

    // free-form curator note lives on AbstractDescribable.description (inherited)
    // human-readable title (e.g. "RNA-seq batch of 100 EEs 2026-05-24") lives on .name

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

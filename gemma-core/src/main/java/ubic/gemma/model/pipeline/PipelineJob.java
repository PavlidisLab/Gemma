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
public class PipelineJob extends AbstractIdentifiable {

    private PipelineJobBatch batch;

    private ExpressionExperiment experiment;

    private JobState state = JobState.PENDING;

    @Nullable
    private SchedulerKind schedulerKind;

    @Nullable
    private String schedulerHandle;

    @Nullable
    private Date submittedAt;

    @Nullable
    private Date startedAt;

    @Nullable
    private Date finishedAt;

    @Nullable
    private Date lastEventAt;

    @Nullable
    private String lastEventKind;

    @Nullable
    private String lastProgressJson;

    @Nullable
    private String errorMessage;

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

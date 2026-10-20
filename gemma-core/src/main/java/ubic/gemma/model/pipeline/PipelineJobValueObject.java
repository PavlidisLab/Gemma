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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Date;

/**
 * Value object projection of {@link PipelineJob} for the REST surface.
 *
 * <h2>Why this exists</h2>
 *
 * {@link PipelineJob#getExperiment()} is a lazy {@code @ManyToOne} to the
 * {@code ExpressionExperiment} entity. Returning the entity published that whole object graph and
 * risked serializing it; {@link PipelineJobBatchValueObject} has the detail. The experiment is
 * carried here as an id, which a caller resolves through {@code GET /datasets/{id}} when it needs
 * more than that.
 *
 * <p>The event log is not embedded either. It is unbounded and paged separately at
 * {@code GET /admin/pipeline/batches/{batchId}/jobs/{jobId}/events}.
 *
 * @author phase3
 * @see PipelineJobEventValueObject
 */
@Data
public class PipelineJobValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the job.")
    private Long id;

    @Schema(description = "Identifier of the batch this job belongs to.")
    private Long batchId;

    @Schema(description = "Identifier of the experiment the pipeline is running against. Resolve it through `GET /datasets/{id}`; the job does not carry the experiment itself.")
    private Long experimentId;

    @Schema(description = "Where the job is in its lifecycle. Independent of the batch's own state — a batch is OPEN while any of its jobs is still non-terminal.")
    private JobState state;

    @Nullable
    @Schema(description = "Which scheduler picked the job up. Null while the job is still PENDING, because nothing has claimed it yet.")
    private SchedulerKind schedulerKind;

    @Nullable
    @Schema(description = "The scheduler's own handle for the job, for correlating with its logs. Null while the job is still PENDING.")
    private String schedulerHandle;

    @Nullable
    @Schema(description = "When the job was handed to a scheduler. Null while it is still PENDING.")
    private Date submittedAt;

    @Nullable
    @Schema(description = "When the scheduler reported the job as started. Null until it does.")
    private Date startedAt;

    @Nullable
    @Schema(description = "When the job reached a terminal state, whether it succeeded or failed. Null while it is still running.")
    private Date finishedAt;

    @Nullable
    @Schema(description = "When the most recent event arrived. A job whose `lastEventAt` has not moved for a long time is what the stale-job sweep looks for.")
    private Date lastEventAt;

    @Nullable
    @Schema(description = "The `kind` of the most recent event, denormalized onto the job so a listing does not have to read the event log.")
    private String lastEventKind;

    @Nullable
    @Schema(description = "The most recent progress payload, verbatim as the scheduler sent it. A JSON document held as a string, so it is not parsed or validated on the way in.")
    private String lastProgressJson;

    @Nullable
    @Schema(description = "Why the job failed, as reported by the scheduler. Null unless it did.")
    private String errorMessage;

    /**
     * Project a job.
     * <p>
     * Reads the batch and experiment identifiers off their proxies rather than the entities, which
     * does not initialize either — the same thing {@link PipelineJob#equals(Object)} relies on. That
     * is what keeps this callable for a job loaded without its associations fetched.
     */
    public static PipelineJobValueObject from( PipelineJob job ) {
        PipelineJobValueObject vo = new PipelineJobValueObject();
        vo.id = job.getId();
        vo.batchId = job.getBatch() != null ? job.getBatch().getId() : null;
        vo.experimentId = job.getExperiment() != null ? job.getExperiment().getId() : null;
        vo.state = job.getState();
        vo.schedulerKind = job.getSchedulerKind();
        vo.schedulerHandle = job.getSchedulerHandle();
        vo.submittedAt = job.getSubmittedAt();
        vo.startedAt = job.getStartedAt();
        vo.finishedAt = job.getFinishedAt();
        vo.lastEventAt = job.getLastEventAt();
        vo.lastEventKind = job.getLastEventKind();
        vo.lastProgressJson = job.getLastProgressJson();
        vo.errorMessage = job.getErrorMessage();
        return vo;
    }
}

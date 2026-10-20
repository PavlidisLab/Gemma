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
 * Value object projection of {@link PipelineJobEvent} for the REST surface.
 *
 * <h2>Why this exists</h2>
 *
 * The endpoints used to return the entity. {@link PipelineJobEvent#getJob()} is a lazy
 * {@code @ManyToOne}, so serializing one walked into {@link PipelineJob} and from there into the
 * whole experiment graph — see {@link PipelineJobBatchValueObject} for what that cost the
 * specification. This carries the owning job as an id instead.
 *
 * @author phase3
 * @see PipelineJobValueObject
 */
@Data
public class PipelineJobEventValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the event.")
    private Long id;

    @Schema(description = "Identifier of the job this event was recorded against.")
    private Long jobId;

    @Schema(description = "When the scheduler reported the event, not when Gemma stored it.")
    private Date occurredAt;

    @Schema(description = "What happened, as the scheduler names it — for example `started`, `progress` or `failed`. Free-form: Gemma stores what the pipeline sends and does not validate it against a fixed set.")
    private String kind;

    @Nullable
    @Schema(description = "The event's payload, verbatim as the scheduler sent it. A JSON document held as a string, so it is not parsed or validated on the way in. Null for an event that carried no payload.")
    private String payloadJson;

    /**
     * Project an event.
     * <p>
     * Reads the owning job's identifier off the proxy rather than the job itself, which does not
     * initialize it — the same thing {@link PipelineJob#equals(Object)} relies on.
     */
    public static PipelineJobEventValueObject from( PipelineJobEvent event ) {
        PipelineJobEventValueObject vo = new PipelineJobEventValueObject();
        vo.id = event.getId();
        vo.jobId = event.getJob() != null ? event.getJob().getId() : null;
        vo.occurredAt = event.getOccurredAt();
        vo.kind = event.getKind();
        vo.payloadJson = event.getPayloadJson();
        return vo;
    }
}

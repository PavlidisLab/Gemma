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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Value object projection of {@link PipelineJobBatch} for the REST surface.
 *
 * <h2>Why this exists</h2>
 *
 * The admin pipeline endpoints returned the entity. Two lazy associations made that a problem:
 * {@link PipelineJobBatch#getJobs()}, and {@link PipelineJob#getExperiment()} beneath it. Nothing
 * initialized either before the response left the resource method, and no {@code @JsonIgnore}
 * stopped Jackson walking them.
 *
 * <p>In the published specification the second one was the expensive half. Following
 * {@code experiment} pulled the {@code ExpressionExperiment} entity in, and from there
 * {@code BioAssay}, {@code BioMaterial}, {@code FactorValue}, {@code Statement}, the expression
 * vectors and the rest of the graph: 44 entity schemas and 466 properties that reached the document
 * through this one field and nowhere else.
 *
 * <p>A batch carries its jobs because the endpoint that serves one is documented as returning the
 * rolled-up state. Each job carries its experiment as an id; see {@link PipelineJobValueObject}.
 *
 * @author phase3
 * @see PipelineJobValueObject
 * @see PipelineJobEventValueObject
 */
@Data
public class PipelineJobBatchValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifier of the batch.")
    private Long id;

    @Nullable
    @Schema(description = "Human-readable title for the run, for example \"RNA-seq batch of 100 EEs 2026-05-24\".")
    private String name;

    @Nullable
    @Schema(description = "The curator's free-form note about the run.")
    private String description;

    @Schema(description = "Which pipeline the batch runs. One of the names `GET /admin/pipeline/registry` lists, when this instance has a registry configured.")
    private String pipeline;

    @Nullable
    @Schema(description = "Identifier of the curator who submitted the batch.")
    private Long submittedById;

    @Schema(description = "When the batch was submitted.")
    private Date submittedAt;

    @Nullable
    @Schema(description = "Parameters handed to the pipeline, verbatim as submitted. A JSON document held as a string, so it is not parsed or validated on the way in.")
    private String paramsJson;

    @Schema(description = "Coarse lifecycle marker for the batch: OPEN while any job is non-terminal, CLOSED once they all are, CANCELLED if a curator cancelled the batch. Distinct from the state of any individual job.")
    private PipelineJobBatch.BatchState state;

    @Nullable
    @Schema(description = "When a curator asked for the batch to be cancelled. Set as soon as the request is recorded, so it can precede the jobs actually stopping.")
    private Date killRequestedAt;

    @Nullable
    @Schema(description = "When the batch closed. Null while it is still open.")
    private Date closedAt;

    @Schema(description = "The batch's jobs, one per experiment it was submitted against. Empty for a batch whose jobs were not fetched.")
    private List<PipelineJobValueObject> jobs = new ArrayList<>();

    /**
     * Project a batch <em>without</em> its jobs.
     * <p>
     * For a caller that only needs the batch's own fields. {@link #getJobs()} is left empty rather
     * than null, so a client does not have to distinguish "not fetched" from "no jobs" — a batch
     * always has at least one.
     */
    public static PipelineJobBatchValueObject from( PipelineJobBatch batch ) {
        PipelineJobBatchValueObject vo = new PipelineJobBatchValueObject();
        vo.id = batch.getId();
        vo.name = batch.getName();
        vo.description = batch.getDescription();
        vo.pipeline = batch.getPipeline();
        vo.submittedById = batch.getSubmittedBy() != null ? batch.getSubmittedBy().getId() : null;
        vo.submittedAt = batch.getSubmittedAt();
        vo.paramsJson = batch.getParamsJson();
        vo.state = batch.getState();
        vo.killRequestedAt = batch.getKillRequestedAt();
        vo.closedAt = batch.getClosedAt();
        return vo;
    }

    /**
     * Project a batch together with its jobs.
     * <p>
     * 🛑 Reads {@link PipelineJobBatch#getJobs()}, so it has to run inside the transaction that
     * loaded the batch. Calling it from a resource method is the bug this class was written to fix.
     */
    public static PipelineJobBatchValueObject withJobs( PipelineJobBatch batch ) {
        PipelineJobBatchValueObject vo = from( batch );
        for ( PipelineJob job : batch.getJobs() ) {
            vo.jobs.add( PipelineJobValueObject.from( job ) );
        }
        vo.jobs.sort( ( a, b ) -> {
            if ( a.getId() == null || b.getId() == null ) {
                return 0;
            }
            return a.getId().compareTo( b.getId() );
        } );
        return vo;
    }
}

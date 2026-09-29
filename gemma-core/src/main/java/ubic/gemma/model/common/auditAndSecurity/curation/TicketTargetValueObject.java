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
package ubic.gemma.model.common.auditAndSecurity.curation;

import lombok.Data;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * Value object projection of {@link TicketTarget} for the REST surface
 * (Phase B-2 of {@code AUDIT_AS_WORKFLOW_RECCE.md}).
 *
 * <p>The {@code displayLabel} and {@code displayName} fields are optional
 * server-resolved hints — populated by the resource layer via a side-join
 * to the targeted entity (e.g. {@code ExpressionExperiment.shortName} +
 * {@code .name} for {@link TicketTargetType#EXPRESSION_EXPERIMENT}). They
 * let the dashboard render a meaningful card without a follow-up fetch.
 * Producers that don't have a cheap join leave them {@code null}; the UI
 * falls back to the bare {@code targetId}.</p>
 *
 * @author paul
 */
@Data
public class TicketTargetValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private TicketTargetType targetType;
    private Long targetId;
    private TicketTargetStatus status = TicketTargetStatus.NOT_DONE;
    private ScreeningResult screeningResult;
    private String screeningResultReason;

    /**
     * This target's own task — what put THIS experiment on the ticket and what is being asked of the
     * curator. Opaque JSON text, served verbatim; see {@link TicketTarget#getPayload()}.
     */
    @Nullable
    private String payload;

    /** Which schema {@link #payload} follows, or null when the writer declared none. */
    @Nullable
    private Integer payloadSchemaVersion;

    /**
     * Short display label for the target (e.g. an EE's {@code shortName} like
     * {@code GSE12345}). {@code null} when no cheap join is available.
     */
    @Nullable
    private String displayLabel;

    /**
     * Human-readable name for the target (e.g. an EE's full {@code name}).
     * {@code null} when no cheap join is available.
     */
    @Nullable
    private String displayName;

    public TicketTargetValueObject() {
    }

    public static TicketTargetValueObject from( TicketTarget t ) {
        TicketTargetValueObject vo = new TicketTargetValueObject();
        vo.id = t.getId();
        vo.targetType = t.getTargetType();
        vo.targetId = t.getTargetId();
        vo.status = t.getStatus() != null ? t.getStatus() : TicketTargetStatus.NOT_DONE;
        vo.screeningResult = t.getScreeningResult();
        vo.screeningResultReason = t.getScreeningResultReason();
        vo.payload = t.getPayload();
        vo.payloadSchemaVersion = t.getPayloadSchemaVersion();
        return vo;
    }

    public ScreeningResult getScreeningResult() {
        return screeningResult;
    }

    public void setScreeningResult( ScreeningResult screeningResult ) {
        this.screeningResult = screeningResult;
    }

    public String getScreeningResultReason() {
        return screeningResultReason;
    }

    public void setScreeningResultReason( String screeningResultReason ) {
        this.screeningResultReason = screeningResultReason;
    }
}

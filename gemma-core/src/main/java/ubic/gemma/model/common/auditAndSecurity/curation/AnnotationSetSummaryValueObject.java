/*
 * The Gemma project
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

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.experiment.AgentCurationKind;

/**
 * Thin metadata projection of an {@link AnnotationSet} row, served by the
 * REST surface when {@code ?shape=meta} is requested. Omits the heavy
 * {@code payloadJson} so list-page renders stay sub-KB per row.
 *
 * <p>{@link #payloadSize} is {@code CHAR_LENGTH(PAYLOAD_JSON)} — the UI
 * uses it as a fetch-heavy-or-skip signal. May be {@code null} when the
 * projection's {@code length()} call could not be computed on the
 * underlying database.</p>
 */
@Getter
@Setter
public class AnnotationSetSummaryValueObject extends IdentifiableValueObject<AnnotationSet> {

    private static final long serialVersionUID = 1L;

    private AnnotationSetRole role;
    private AnnotationSetSource source;
    @Nullable
    private AgentCurationKind kind;
    private String runId;
    @Nullable
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
    @Nullable
    private Date finalizedAt;
    @Nullable
    private String finalizedBy;
    @Nullable
    private String agentVersion;
    @Nullable
    private String model;
    @Nullable
    private String runSha;
    @Nullable
    private String agentName;
    @Nullable
    private Date ranAt;
    private Long investigationId;
    @Nullable
    private Long parentId;
    /**
     * Size of the omitted {@code payloadJson} in characters. UI uses this
     * to gate a fetch-full vs skip decision. Null when the database could
     * not compute it (e.g. CLOB length unsupported in some projection
     * paths).
     */
    @Nullable
    private Long payloadSize;

    public AnnotationSetSummaryValueObject() {
        super();
    }

    /**
     * Projection constructor used by the HQL {@code SELECT NEW ...} query
     * in {@code AnnotationSetDaoImpl}.
     */
    public AnnotationSetSummaryValueObject( Long id, AnnotationSetRole role, AnnotationSetSource source,
            @Nullable AgentCurationKind kind, String runId, @Nullable String createdBy,
            Date createdAt, Date updatedAt,
            @Nullable Date finalizedAt, @Nullable String finalizedBy,
            @Nullable String agentVersion, @Nullable String model,
            @Nullable String runSha, @Nullable String agentName, @Nullable Date ranAt,
            Long investigationId, @Nullable Long parentId, @Nullable Long payloadSize ) {
        super( id );
        this.role = role;
        this.source = source;
        this.kind = kind;
        this.runId = runId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.finalizedAt = finalizedAt;
        this.finalizedBy = finalizedBy;
        this.agentVersion = agentVersion;
        this.model = model;
        this.runSha = runSha;
        this.agentName = agentName;
        this.ranAt = ranAt;
        this.investigationId = investigationId;
        this.parentId = parentId;
        this.payloadSize = payloadSize;
    }
}

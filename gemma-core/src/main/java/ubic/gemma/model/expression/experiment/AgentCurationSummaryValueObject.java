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
package ubic.gemma.model.expression.experiment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.IdentifiableValueObject;

import java.util.Date;

/**
 * Thin metadata projection of an {@link AgentProposal} row, served by the
 * REST surface when {@code ?shape=meta} is requested. Omits the heavy
 * {@code payloadJson} (50–100 KB typical) so list-page renders stay sub-KB
 * per row.
 *
 * <p>{@link #payloadSize} is {@code CHAR_LENGTH(PAYLOAD_JSON)} — the UI uses
 * it as a fetch-heavy-or-skip signal. May be {@code null} when the
 * projection's {@code length()} call could not be computed on the underlying
 * database.</p>
 *
 * <p>See {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md} §3.</p>
 */
@Getter
@Setter
public class AgentCurationSummaryValueObject extends IdentifiableValueObject<AgentProposal> {

    private static final long serialVersionUID = 1L;

    private AgentCurationKind kind;
    private String runId;
    @Nullable
    private String agentVersion;
    @Nullable
    private String model;
    @Nullable
    private Date ranAt;
    private Long investigationId;
    /**
     * Size of the omitted {@code payloadJson} in characters. UI uses this to
     * gate a fetch-full vs skip decision. Null when the database could not
     * compute it (e.g. CLOB length unsupported in some projection paths).
     */
    @Nullable
    private Long payloadSize;

    public AgentCurationSummaryValueObject() {
        super();
    }

    /**
     * Projection constructor used by the HQL {@code SELECT NEW ...} query in
     * {@code AgentProposalDaoImpl.findSummariesByInvestigation}.
     */
    public AgentCurationSummaryValueObject( Long id, AgentCurationKind kind, String runId,
            @Nullable String agentVersion, @Nullable String model, @Nullable Date ranAt,
            Long investigationId, @Nullable Long payloadSize ) {
        super( id );
        this.kind = kind;
        this.runId = runId;
        this.agentVersion = agentVersion;
        this.model = model;
        this.ranAt = ranAt;
        this.investigationId = investigationId;
        this.payloadSize = payloadSize;
    }
}

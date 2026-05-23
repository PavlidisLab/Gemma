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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.persistence.service.BaseDao;

import java.util.List;

/**
 * DAO for {@link AgentProposal} rows. The contract is small because
 * proposals are append-only and addressed by either id or
 * {@code (investigation, kind, runId)} (the idempotency key).
 */
public interface AgentProposalDao extends BaseDao<AgentProposal> {

    /**
     * Find an existing row for the {@code (investigation, kind, runId)} triple,
     * or {@code null}. Used by the service to make
     * {@code POST /preboarded/{id}/proposals} idempotent on retry. The
     * {@code kind} parameter lets a forward-looking proposal and a post-hoc
     * audit coexist on the same investigation with the same {@code runId}.
     */
    @Nullable
    AgentProposal findByInvestigationAndKindAndRunId( Investigation investigation,
            AgentCurationKind kind, String runId );

    /**
     * All proposals attached to the given investigation, newest first.
     */
    List<AgentProposal> findByInvestigation( Investigation investigation );

    /**
     * Thin metadata projection of all proposals attached to the given
     * investigation, newest first. The {@code payloadJson} column is NOT
     * loaded; the projection emits {@code length(payloadJson)} as
     * {@code payloadSize} so the UI can decide whether to fetch the full
     * row. See {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md} §3.
     *
     * @param investigation target investigation. Required.
     * @param kindFilter    optional kind filter; {@code null} means "all
     *                      kinds" (both proposal and audit rows).
     */
    List<AgentCurationSummaryValueObject> findSummariesByInvestigation( Investigation investigation,
            @Nullable AgentCurationKind kindFilter );

    /**
     * The most recent proposal attached to the given investigation, or
     * {@code null} if none exist. "Most recent" by {@code ranAt}, falling
     * back to id when {@code ranAt} ties.
     */
    @Nullable
    AgentProposal findLatestByInvestigation( Investigation investigation );

    /**
     * Count proposals attached to the given investigation.
     */
    long countByInvestigation( Investigation investigation );

    /**
     * Rebind every proposal currently attached to {@code from} so it points at
     * {@code to} instead. Returns the number of rows rebound. Used by the
     * preboarded-promotion flow: preboarded rows do NOT carry the loaded EE's
     * curatable artifacts, so the agent's historical proposals must follow
     * the EE rather than stay on the preboarded.
     */
    int rebindInvestigation( Investigation from, Investigation to );

    /**
     * Cross-experiment thin metadata projection: every {@link AgentProposal}
     * matching the supplied filter, newest first, sliced by
     * {@code offset / limit}.
     *
     * @param kindFilter        optional kind filter; {@code null} means "all
     *                          kinds".
     * @param investigationIds  optional restriction to a set of investigation
     *                          ids; {@code null} or empty means "all
     *                          investigations the caller can see" (no
     *                          additional filter — ACL is enforced upstream
     *                          via @PreAuthorize on the REST handler).
     * @param offset            zero-based starting offset.
     * @param limit             max rows to return.
     */
    List<AgentCurationSummaryValueObject> listSummaries( @Nullable AgentCurationKind kindFilter,
            @Nullable List<Long> investigationIds, int offset, int limit );

    /**
     * Cross-experiment count: number of {@link AgentProposal} rows that match
     * the supplied filter. The counterpart of
     * {@link #listSummaries(AgentCurationKind, List, int, int)} — used to
     * populate {@code totalElements} on a paginated response.
     */
    long countSummaries( @Nullable AgentCurationKind kindFilter,
            @Nullable List<Long> investigationIds );
}

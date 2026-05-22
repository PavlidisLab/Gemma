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
}

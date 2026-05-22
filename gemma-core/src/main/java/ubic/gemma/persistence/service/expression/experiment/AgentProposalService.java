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
import ubic.gemma.model.expression.experiment.AgentProposal;

import java.util.Date;
import java.util.List;

/**
 * Service surface for append-only {@link AgentProposal} rows. Drives both
 * the public {@code /preboarded/{id}/proposals} endpoint and the private
 * {@code /datasets/{id}/curation-proposals} endpoint
 * (see {@code STATUS_CURATION_PROPOSALS.md} for the consolidation
 * decision).
 *
 * <p>{@link #attach(Investigation, String, String, String, Date, String)}
 * is idempotent on {@code (investigation, runId)}: a retry with the same
 * pair returns the existing row instead of creating a duplicate. The
 * REST layer reports the existing row as 200 OK rather than 201 Created
 * in that case.</p>
 */
public interface AgentProposalService {

    /**
     * Attach (or return existing) a proposal to the given investigation.
     *
     * @param investigation  target. Required; may be either a
     *                       {@code PreboardedExperiment} or an
     *                       {@code ExpressionExperiment}.
     * @param runId          the agent runner's unique id for this run.
     *                       Required.
     * @param agentVersion   optional agent runner release version.
     * @param model          optional LLM identifier.
     * @param ranAt          optional run completion timestamp; defaults to
     *                       {@code new Date()} if null.
     * @param payloadJson    optional structured proposal payload as JSON.
     * @return the persisted proposal — either freshly created or the
     *         pre-existing row for the same {@code (investigation, runId)}.
     */
    AttachedProposal attach( Investigation investigation, String runId,
            @Nullable String agentVersion,
            @Nullable String model,
            @Nullable Date ranAt,
            @Nullable String payloadJson );

    /**
     * @return all proposals attached to the given investigation, newest first.
     */
    List<AgentProposal> findByInvestigation( Investigation investigation );

    /**
     * @return the most recent proposal attached to the given investigation,
     *         or {@code null} if none exist.
     */
    @Nullable
    AgentProposal findLatestByInvestigation( Investigation investigation );

    /**
     * @return the proposal with the given id, or {@code null}.
     */
    @Nullable
    AgentProposal load( Long id );

    /**
     * @return the number of proposals attached to the given investigation.
     */
    long countByInvestigation( Investigation investigation );

    /**
     * Rebind every proposal currently attached to {@code from} so it points
     * at {@code to}. Called by the preboarded-promotion path. Returns the
     * number of rows rebound.
     */
    int rebindInvestigation( Investigation from, Investigation to );

    /**
     * Return value of {@link #attach(Investigation, String, String, String, Date, String)}.
     * Carries the persisted proposal plus a flag noting whether the row was
     * created (true) or returned as-existing (false) — the REST layer uses
     * it to decide between 201 Created and 200 OK.
     */
    class AttachedProposal {
        private final AgentProposal proposal;
        private final boolean created;

        public AttachedProposal( AgentProposal proposal, boolean created ) {
            this.proposal = proposal;
            this.created = created;
        }

        public AgentProposal getProposal() {
            return proposal;
        }

        public boolean isCreated() {
            return created;
        }
    }
}

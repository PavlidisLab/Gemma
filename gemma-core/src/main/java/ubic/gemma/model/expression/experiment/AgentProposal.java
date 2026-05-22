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

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Date;
import java.util.Objects;

/**
 * Append-only record of a curation-agents proposal payload.
 *
 * <p>One row per agent run. Attached to an {@link Investigation} (either a
 * {@code PreboardingExperiment} pre-load or an {@code ExpressionExperiment}
 * post-load); the FK is rebound from preboarding to EE at promotion time.</p>
 *
 * <p>The {@code payloadJson} column carries the full structured proposal the
 * agent produced (factors, FVs, sample assignments, tags, etc.). It's stored
 * as a MySQL {@code JSON} column on prod and an H2 {@code CLOB} in the test
 * profile (see Flyway V11 / V13). The schema is intentionally opaque on the
 * Java side: the agent owns the payload shape; Gemma persists it verbatim.</p>
 *
 * <p>Idempotency is on {@code (investigation, runId)}: re-uploading the same
 * {@code runId}'s payload is a no-op that returns the existing row. The
 * unique constraint enforces it.</p>
 *
 * <p>See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"The decided shape"
 * and {@code STATUS_CURATION_PROPOSALS.md} for the consolidation decision
 * (one entity feeds both {@code /preboarding/{id}/proposals} and
 * {@code /datasets/{id}/curation-proposals}).</p>
 */
public class AgentProposal extends AbstractIdentifiable {

    private Investigation investigation;
    private String runId;
    private String agentVersion;
    private String model;
    private Date ranAt;
    private String payloadJson;

    public AgentProposal() {
    }

    /**
     * @return the {@link Investigation} this proposal targets. Never null on
     *         persisted rows. Rebound from preboarding to EE at promotion time.
     */
    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation( Investigation investigation ) {
        this.investigation = investigation;
    }

    /**
     * @return the agent runner's unique id for the run that produced this
     *         proposal. Together with {@link #investigation} this is the
     *         idempotency key for {@code POST /preboarding/{id}/proposals}.
     */
    public String getRunId() {
        return runId;
    }

    public void setRunId( String runId ) {
        this.runId = runId;
    }

    /**
     * @return the agent runner's release version (e.g. {@code "0.8.0"}).
     */
    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion( String agentVersion ) {
        this.agentVersion = agentVersion;
    }

    /**
     * @return the LLM identifier the agent used (e.g.
     *         {@code "claude-opus-4-7-1m"}).
     */
    public String getModel() {
        return model;
    }

    public void setModel( String model ) {
        this.model = model;
    }

    /**
     * @return when the agent run that produced this proposal completed.
     */
    public Date getRanAt() {
        return ranAt;
    }

    public void setRanAt( Date ranAt ) {
        this.ranAt = ranAt;
    }

    /**
     * @return the full structured proposal payload as a JSON string. The
     *         shape is owned by the agent; Gemma does not interpret the
     *         payload (except optionally during promotion's
     *         {@code apply_latest_proposal} pass).
     */
    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson( String payloadJson ) {
        this.payloadJson = payloadJson;
    }

    @Override
    public int hashCode() {
        return Objects.hash( investigation, runId );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof AgentProposal ) ) return false;
        AgentProposal other = ( AgentProposal ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( investigation, other.investigation )
                && Objects.equals( runId, other.runId );
    }
}

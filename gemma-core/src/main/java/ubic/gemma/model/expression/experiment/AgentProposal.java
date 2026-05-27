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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;
import java.util.Objects;

/**
 * Append-only record of a curation-agents proposal payload.
 *
 * <p>One row per agent run. Attached to an {@link Investigation} (either a
 * {@code PreboardedExperiment} pre-load or an {@code ExpressionExperiment}
 * post-load); the FK is rebound from preboarded to EE at promotion time.</p>
 *
 * <p>The {@code payloadJson} column carries the full structured proposal the
 * agent produced (factors, FVs, sample assignments, tags, etc.). It's stored
 * as a MySQL {@code JSON} column on prod and an H2 {@code CLOB} in the test
 * profile (see Flyway V11 / V13). The schema is intentionally opaque on the
 * Java side: the agent owns the payload shape; Gemma persists it verbatim.</p>
 *
 * <p>Idempotency is on {@code (investigation, kind, runId)}: re-uploading the
 * same {@code runId}'s payload (for the same {@code kind}) is a no-op that
 * returns the existing row. The unique constraint enforces it. The
 * {@code kind} discriminator distinguishes forward-looking proposals from
 * post-hoc audits — see {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md}.</p>
 *
 * <p>See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"The decided shape"
 * and {@code STATUS_CURATION_PROPOSALS.md} for the consolidation decision
 * (one entity feeds both {@code /preboarded/{id}/proposals} and
 * {@code /datasets/{id}/curation-proposals}).</p>
 */
@Entity
@Table(name = "AGENT_PROPOSAL",
        uniqueConstraints = @UniqueConstraint(name = "UK_AGENT_PROPOSAL_INVESTIGATION_KIND_RUN",
                columnNames = { "INVESTIGATION_FK", "KIND", "RUN_ID" }),
        indexes = @Index(name = "IDX_AGENT_PROPOSAL_INVESTIGATION", columnList = "INVESTIGATION_FK"))
public class AgentProposal extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVESTIGATION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_AGENT_PROPOSAL_INVESTIGATION"))
    private Investigation investigation;
    @Enumerated(EnumType.STRING)
    @Column(name = "KIND", nullable = false, columnDefinition = "VARCHAR(16)")
    private AgentCurationKind kind = AgentCurationKind.PROPOSAL;
    @Column(name = "RUN_ID", nullable = false, columnDefinition = "VARCHAR(255)")
    private String runId;
    @Column(name = "AGENT_VERSION", columnDefinition = "VARCHAR(255)")
    private String agentVersion;
    @Column(name = "MODEL", columnDefinition = "VARCHAR(255)")
    private String model;
    @Column(name = "RAN_AT", columnDefinition = "DATETIME")
    private Date ranAt;
    @Lob
    @Column(name = "PAYLOAD_JSON", columnDefinition = "LONGTEXT")
    private String payloadJson;

    /**
     * Lifecycle status. Defaults to {@code OPEN} (agent emitted, no curator
     * action yet). {@code FINALIZED} after the curator finalizes; {@code
     * REOPENED} if the curator un-finalizes (re-opens the edit surface).
     * Stored as a free-form {@code VARCHAR(32)} string rather than an enum
     * so wire vocabulary can evolve without a schema migration; see Flyway
     * mysql/V15 + h2/V17.
     */
    @Column(name = "STATUS", nullable = false, columnDefinition = "VARCHAR(32)")
    private String status = "OPEN";

    /**
     * Curator-chosen disposition (allow-list validated at the REST handler
     * boundary): {@code accept}, {@code accepted_with_edits}, {@code reject},
     * {@code edit}, {@code park}. Null until the curator dispositions the
     * row. See {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md} §4.1.
     */
    @Column(name = "DISPOSITION", columnDefinition = "VARCHAR(32)")
    private String disposition;

    /** Optional free-text curator note attached to the disposition. */
    @Lob
    @Column(name = "DISPOSITION_NOTE", columnDefinition = "TEXT")
    private String dispositionNote;

    /** Timestamp the row was finalized; reset to {@code null} on reopen. */
    @Column(name = "FINALIZED_AT", columnDefinition = "DATETIME(3)")
    private Date finalizedAt;

    /**
     * Last-touched timestamp. MySQL's {@code ON UPDATE CURRENT_TIMESTAMP(3)}
     * keeps it fresh on prod; the service layer stamps it on save() so H2
     * (no {@code ON UPDATE} semantics) stays behaviourally aligned.
     */
    @Column(name = "LAST_UPDATED", columnDefinition = "DATETIME(3)")
    private Date lastUpdated;

    public AgentProposal() {
    }

    /**
     * @return the discriminator that distinguishes a forward-looking proposal
     *         (default) from a post-hoc audit. Never null; defaults to
     *         {@link AgentCurationKind#PROPOSAL} so existing call sites that
     *         predate the discriminator continue to write proposal rows.
     *         See {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md} §2.
     */
    public AgentCurationKind getKind() {
        return kind;
    }

    public void setKind( AgentCurationKind kind ) {
        this.kind = kind;
    }

    /**
     * @return the {@link Investigation} this proposal targets. Never null on
     *         persisted rows. Rebound from preboarded to EE at promotion time.
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
     *         idempotency key for {@code POST /preboarded/{id}/proposals}.
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

    public String getStatus() {
        return status;
    }

    public void setStatus( String status ) {
        this.status = status;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition( String disposition ) {
        this.disposition = disposition;
    }

    public String getDispositionNote() {
        return dispositionNote;
    }

    public void setDispositionNote( String dispositionNote ) {
        this.dispositionNote = dispositionNote;
    }

    public Date getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt( Date finalizedAt ) {
        this.finalizedAt = finalizedAt;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public int hashCode() {
        return Objects.hash( investigation, kind, runId );
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
                && Objects.equals( kind, other.kind )
                && Objects.equals( runId, other.runId );
    }
}

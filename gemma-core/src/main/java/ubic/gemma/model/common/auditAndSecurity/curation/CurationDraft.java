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

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.experiment.AgentProposal;

import java.util.Date;
import java.util.Objects;

/**
 * Per-{@code (investigation, curator)} mutable curation draft buffer.
 *
 * <p>Unifies the "in-progress curator edits" role and the "per-proposal
 * element disposition tracking" role into a single row. The curator's full
 * WIP payload lives in {@link #payloadJson} (JSON owned by the client); if
 * the draft was seeded from an {@link AgentProposal}, the original proposal
 * payload is captured verbatim in {@link #proposalSnapshotJson} so the
 * disposition of each proposal element (retained / edited / rejected /
 * parked) can be DERIVED at read time by diffing the two JSON blobs — only
 * {@link #parkedElements} requires explicit storage (since "pending" and
 * "parked" both leave the payload unchanged).</p>
 *
 * <p>One row per {@code (investigation, curator)} (enforced by a UNIQUE
 * constraint). FKs cascade-delete on investigation or curator removal so the
 * buffer row never outlives its targets. The {@code proposal} FK is
 * {@code ON DELETE SET NULL}: if a proposal row is dropped, the draft
 * retains the curator's WIP payload but loses the snapshot reference.</p>
 *
 * <p>Lifecycle: {@code finalizedAt} is stamped when the curator says "I'm
 * done reviewing" but has NOT yet committed the design / annotations. The
 * commit endpoint (see {@code DraftsWebService#commit}) calls the existing
 * {@code applyDesignChange} / {@code replaceAnnotations} paths and then
 * deletes this row on success.</p>
 *
 * <p>Draft state changes are NOT audited — they're buffer state. The commit
 * step downstream emits typed audit events through the existing
 * design/annotation write endpoints.</p>
 */
public class CurationDraft extends AbstractIdentifiable {

    private Investigation investigation;
    private Contact curator;
    private String payloadJson;
    private AgentProposal proposal;
    private String proposalSnapshotJson;
    private String parkedElements;
    private Date startedAt;
    private Date lastEditedAt;
    private Date finalizedAt;

    public CurationDraft() {
    }

    /**
     * @return the {@link Investigation} this draft targets. Required.
     */
    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation( Investigation investigation ) {
        this.investigation = investigation;
    }

    /**
     * @return the curator (a {@link Contact}, typically a
     *         {@link ubic.gemma.model.common.auditAndSecurity.User User})
     *         who owns this draft. Required.
     */
    public Contact getCurator() {
        return curator;
    }

    public void setCurator( Contact curator ) {
        this.curator = curator;
    }

    /**
     * @return the curator's full WIP payload as a JSON string. The schema is
     *         opaque on the Java side; the curation-UI owns the shape
     *         (ExperimentalDesignValueObject + tags + whatever else).
     */
    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson( String payloadJson ) {
        this.payloadJson = payloadJson;
    }

    /**
     * @return the {@link AgentProposal} this draft was seeded from, or
     *         {@code null} if the curator started from scratch.
     */
    public AgentProposal getProposal() {
        return proposal;
    }

    public void setProposal( AgentProposal proposal ) {
        this.proposal = proposal;
    }

    /**
     * @return a snapshot of {@link AgentProposal#getPayloadJson()} captured
     *         at seed-time. Used to diff against {@link #payloadJson} when
     *         deriving per-element dispositions. {@code null} when no
     *         proposal seeded the draft.
     */
    public String getProposalSnapshotJson() {
        return proposalSnapshotJson;
    }

    public void setProposalSnapshotJson( String proposalSnapshotJson ) {
        this.proposalSnapshotJson = proposalSnapshotJson;
    }

    /**
     * @return a JSON array of opaque element keys the curator has parked
     *         (e.g. {@code ["factor:42:0","tag:42:3"]}). {@code null} or
     *         empty array means nothing parked.
     */
    public String getParkedElements() {
        return parkedElements;
    }

    public void setParkedElements( String parkedElements ) {
        this.parkedElements = parkedElements;
    }

    /**
     * @return when the draft was first created.
     */
    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt( Date startedAt ) {
        this.startedAt = startedAt;
    }

    /**
     * @return when the draft was most recently mutated. Updated on every
     *         {@code saveOrUpdate}.
     */
    public Date getLastEditedAt() {
        return lastEditedAt;
    }

    public void setLastEditedAt( Date lastEditedAt ) {
        this.lastEditedAt = lastEditedAt;
    }

    /**
     * @return when the curator stamped "done reviewing" on the draft, or
     *         {@code null} if still in-flight. Finalize is lighter-weight
     *         than commit: the row is still present, but downstream tooling
     *         can treat it as ready-to-push.
     */
    public Date getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt( Date finalizedAt ) {
        this.finalizedAt = finalizedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash( investigation, curator );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof CurationDraft ) ) return false;
        CurationDraft other = ( CurationDraft ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( investigation, other.investigation )
                && Objects.equals( curator, other.curator );
    }
}

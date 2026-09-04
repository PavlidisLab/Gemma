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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;
import java.util.Objects;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * A "curation hypothesis" attached to an {@link Investigation}: a JSON
 * description of one annotation set in one of three lifecycle shapes
 * (see {@link AnnotationSetRole}).
 *
 * <p><b>Relationship to the applied annotations.</b> The authoritative
 * record of an experiment's annotations lives on the EE entity graph
 * (Characteristic, Statement, FactorValue, &hellip;). {@link AnnotationSet}
 * is the curation log alongside that graph &mdash; a side-table of
 * hypotheses, drafts, and snapshots that may or may not have been
 * applied. Reading "the current annotations on EE X" still means walking
 * the EE; reading "what did the agent propose / what is the curator
 * drafting / what was the state at time T" is what this table answers.</p>
 *
 * <p><b>Roles.</b></p>
 * <ul>
 *   <li>{@link AnnotationSetRole#PROPOSAL} &mdash; immutable hypothesis
 *       emitted by an agent run (or external import). The
 *       {@link #kind kind} sub-discriminator distinguishes a
 *       forward-looking proposal from a post-hoc audit.</li>
 *   <li>{@link AnnotationSetRole#DRAFT} &mdash; mutable curator WIP buffer.
 *       Typically seeded from a {@code PROPOSAL} via {@link #parent};
 *       per-element disposition is derived at read time by diffing
 *       {@link #payloadJson} against the parent's payload.
 *       {@link #parkedElements} is the sidecar for opaque element keys
 *       the curator has chosen to park.</li>
 *   <li>{@link AnnotationSetRole#SNAPSHOT} &mdash; immutable capture of
 *       the experiment's annotation state at a point in time. A SNAPSHOT
 *       with {@link #finalizedAt} set is the "polished" view the curator
 *       has blessed as canonical; without it, the row is a raw capture
 *       (e.g. for diffing across runs).</li>
 * </ul>
 *
 * <p><b>Idempotency.</b> {@code UNIQUE(investigation, role, runId)}
 * carries three different semantics depending on role:</p>
 * <ul>
 *   <li>PROPOSAL &mdash; {@code runId} is the agent runner's unique
 *       identifier for the run; re-posting the same run is a no-op
 *       returning the existing row (matches the
 *       {@code AgentProposal} contract).</li>
 *   <li>DRAFT &mdash; {@code runId} is derived as
 *       {@code "draft-{createdBy}"} so the constraint enforces
 *       "one DRAFT per (investigation, curator)".</li>
 *   <li>SNAPSHOT &mdash; {@code runId} is a generated UUID at create
 *       time; SNAPSHOTs are append-only.</li>
 * </ul>
 *
 * <p>FK to {@link Investigation} cascades on delete; FK to
 * {@link #parent} is {@code ON DELETE SET NULL} so a parent retirement
 * leaves the descendant payload intact (sans lineage baseline).</p>
 */
@Entity
@Table(name = "ANNOTATION_SET",
        uniqueConstraints = @UniqueConstraint(name = "UK_ANNOTATION_SET_INVESTIGATION_ROLE_RUN",
                columnNames = { "INVESTIGATION_FK", "ROLE", "RUN_ID" }),
        indexes = {
                @Index(name = "IDX_ANNOTATION_SET_INVESTIGATION_ROLE", columnList = "INVESTIGATION_FK,ROLE"),
                @Index(name = "IDX_ANNOTATION_SET_PARENT", columnList = "PARENT_FK"),
                @Index(name = "IDX_ANNOTATION_SET_RUN", columnList = "RUN_ID")
        })
public class AnnotationSet extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    // V20 declares FK_ANNOTATION_SET_INVESTIGATION ON DELETE CASCADE; keep the mapping in step so a Hibernate-generated schema cascades too
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "INVESTIGATION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_ANNOTATION_SET_INVESTIGATION"))
    private Investigation investigation;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, columnDefinition = "VARCHAR(32)")
    private AnnotationSetRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE", nullable = false, columnDefinition = "VARCHAR(32)")
    private AnnotationSetSource source;

    /**
     * Sub-discriminator on {@code PROPOSAL} rows: distinguishes a
     * forward-looking proposal from a post-hoc audit. {@code null} for
     * {@code DRAFT} / {@code SNAPSHOT} rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "KIND", columnDefinition = "VARCHAR(32)")
    private AgentCurationKind kind;

    @Column(name = "RUN_ID", nullable = false, columnDefinition = "VARCHAR(255)")
    private String runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_ANNOTATION_SET_PARENT"))
    private AnnotationSet parent;

    /**
     * Username (for {@code CURATOR} source) or agent run identifier (for
     * {@code AGENT} source). Free-form; carries the producer's display
     * identity. Persisted as {@code VARCHAR(255)} rather than an FK to
     * {@code CONTACT} so external imports (CRAFT, partner labs) can
     * record provenance without a Gemma {@code Contact} row.
     */
    @Column(name = "CREATED_BY", columnDefinition = "VARCHAR(255)")
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date createdAt;

    @Column(name = "UPDATED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date updatedAt;

    /**
     * For {@code DRAFT}: stamped when the curator marks the buffer as
     * done editing. For {@code SNAPSHOT}: stamped when the row is
     * blessed as the "polished" canonical view. {@code null} on
     * {@code PROPOSAL} rows (proposals are immutable by definition; the
     * lifecycle here doesn't apply).
     */
    @Column(name = "FINALIZED_AT", columnDefinition = "DATETIME(3)")
    private Date finalizedAt;

    @Column(name = "FINALIZED_BY", columnDefinition = "VARCHAR(255)")
    private String finalizedBy;

    /**
     * The curator's closing note on this finalization — why the review ended
     * the way it did.
     * <p>
     * Cleared on reopen: a note explains one closure, so carrying it across
     * would attach one closure's words to the next. {@code null} means no note
     * was written; a blank one is normalized to {@code null} on write so
     * "did they say anything" has a single answer.
     */
    @Column(name = "FINALIZED_NOTES", columnDefinition = "VARCHAR(2048)")
    private String finalizedNotes;

    @Column(name = "AGENT_VERSION", columnDefinition = "VARCHAR(255)")
    private String agentVersion;

    @Column(name = "MODEL", columnDefinition = "VARCHAR(255)")
    private String model;

    /**
     * The producing repository's git head sha for the run.
     * <p>
     * Not redundant with {@link #model}: the curation side has measured behaviour differences between shas at
     * one model, so the model alone does not identify the build that wrote an annotation. Deliberately separate
     * from {@link #agentVersion}, which names a release rather than a commit.
     */
    @Column(name = "RUN_SHA", columnDefinition = "VARCHAR(255)")
    private String runSha;

    /**
     * Which specialist agent produced this — {@code cell_type}, {@code disease}, {@code strain}, and so on.
     * <p>
     * "The agent" is a fleet, and the useful answer to "which agent proposed this?" names the member rather than
     * the fleet. Null for curator-authored rows and for producers that do not report one.
     */
    @Column(name = "AGENT_NAME", columnDefinition = "VARCHAR(255)")
    private String agentName;

    @Column(name = "RAN_AT", columnDefinition = "DATETIME")
    private Date ranAt;

    /**
     * The structured annotation payload as a JSON string. The shape is
     * owned by the producer (the curation-agents client for {@code AGENT}
     * source; the curation-UI for {@code CURATOR}); Gemma does not interpret
     * it. MySQL stores this as a native {@code JSON} column (V20), H2 as
     * {@code CLOB} (H2 has no JSON type).
     * <p>
     * Not byte-preserved: MySQL normalises a {@code JSON} value on write,
     * stripping insignificant whitespace and reordering object keys, so
     * {@code {"v":2}} reads back as {@code {"v": 2}}. The document round-trips,
     * the bytes do not. Compare payloads as parsed JSON, never as strings.
     * <p>
     * The JDBC type is pinned explicitly rather than left to {@code @Lob}.
     * {@code @Lob} resolves to {@code Types#CLOB}, but Connector/J reports a
     * MySQL {@code JSON} column as {@code Types#LONGVARCHAR}, so the two
     * disagree and schema validation fails with "found [json
     * (Types#LONGVARCHAR)], but expecting [longtext (Types#CLOB)]".
     * {@code columnDefinition} does not help: it drives DDL generation, not
     * the type validation compares.
     * <p>
     * Only gemma-staging sets {@code hbm2ddl.auto=validate} — production and
     * developer instances leave it empty and take no schema action — so a
     * divergence here is invisible everywhere else until it takes that one
     * instance down at startup. It went unnoticed for three months that way.
     * Keep the annotation, V20 and the H2 sibling (V21) in step.
     */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "PAYLOAD_JSON", columnDefinition = "JSON")
    private String payloadJson;

    /**
     * For {@code DRAFT} rows only: JSON array of opaque element keys the
     * curator has parked (e.g. {@code ["factor:42:0","tag:42:3"]}).
     * Diff-derived dispositions can't distinguish "pending review" from
     * "parked" because both leave the payload unchanged, so parked
     * status needs explicit storage. {@code null} on
     * {@code PROPOSAL} / {@code SNAPSHOT} rows.
     */
    @Lob
    @Column(name = "PARKED_ELEMENTS", columnDefinition = "LONGTEXT")
    private String parkedElements;

    public AnnotationSet() {
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation( Investigation investigation ) {
        this.investigation = investigation;
    }

    public AnnotationSetRole getRole() {
        return role;
    }

    public void setRole( AnnotationSetRole role ) {
        this.role = role;
    }

    public AnnotationSetSource getSource() {
        return source;
    }

    public void setSource( AnnotationSetSource source ) {
        this.source = source;
    }

    public AgentCurationKind getKind() {
        return kind;
    }

    public void setKind( AgentCurationKind kind ) {
        this.kind = kind;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId( String runId ) {
        this.runId = runId;
    }

    public AnnotationSet getParent() {
        return parent;
    }

    public void setParent( AnnotationSet parent ) {
        this.parent = parent;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy( String createdBy ) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt( Date createdAt ) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt( Date updatedAt ) {
        this.updatedAt = updatedAt;
    }

    public Date getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt( Date finalizedAt ) {
        this.finalizedAt = finalizedAt;
    }

    public String getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy( String finalizedBy ) {
        this.finalizedBy = finalizedBy;
    }

    public String getFinalizedNotes() {
        return finalizedNotes;
    }

    public void setFinalizedNotes( String finalizedNotes ) {
        this.finalizedNotes = finalizedNotes;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion( String agentVersion ) {
        this.agentVersion = agentVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel( String model ) {
        this.model = model;
    }

    public String getRunSha() {
        return runSha;
    }

    public void setRunSha( String runSha ) {
        this.runSha = runSha;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName( String agentName ) {
        this.agentName = agentName;
    }

    public Date getRanAt() {
        return ranAt;
    }

    public void setRanAt( Date ranAt ) {
        this.ranAt = ranAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson( String payloadJson ) {
        this.payloadJson = payloadJson;
    }

    public String getParkedElements() {
        return parkedElements;
    }

    public void setParkedElements( String parkedElements ) {
        this.parkedElements = parkedElements;
    }

    @Override
    public int hashCode() {
        return Objects.hash( investigation, role, runId );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof AnnotationSet ) ) return false;
        AnnotationSet other = ( AnnotationSet ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( investigation, other.investigation )
                && Objects.equals( role, other.role )
                && Objects.equals( runId, other.runId );
    }
}

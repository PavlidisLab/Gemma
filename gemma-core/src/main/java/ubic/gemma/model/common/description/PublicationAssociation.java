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
package ubic.gemma.model.common.description;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Date;
import java.util.Objects;

/**
 * An evidenced claim about whether a {@link BibliographicReference} belongs to an
 * {@link Investigation} — who says so, on what basis, and whether they are affirming or denying it.
 *
 * <p><b>Why this exists.</b> The experiment&harr;publication link was the one assertion in the model
 * with no evidence slot. Gemma could record "the publication for this experiment is X" and nothing
 * else: not that Y had been considered and ruled out, not that X came from GEO's own cross-link
 * rather than from anyone reading the paper, not who decided or when. Everything a curator learned
 * while adjudicating a link was therefore thrown away, and every rejection had to be remembered
 * somewhere outside Gemma. Annotations already carry
 * {@link Characteristic#getEvidenceCode() evidenceCode} and
 * {@link Characteristic#getSupportingEvidence() supportingEvidence}; this gives the publication link
 * the same treatment.</p>
 *
 * <p><b>Relationship to the link itself.</b> This table does not replace
 * {@link Investigation#getPrimaryPublication()} / {@link Investigation#getOtherRelevantPublications()}
 * — production Gemma 1.32.x shares this database and reads both, so they stay exactly as they are and
 * this arrives as a pure {@code CREATE TABLE}. Treat the two as one record with two halves kept in
 * step by {@code PublicationAssociationService}: every {@link PublicationAssociationStatus#ACCEPTED}
 * row has a matching link, and {@link PublicationAssociationStatus#REJECTED} rows exist here only,
 * because a rejection with a live link would show up in Gemma 1.x as an ordinary publication of the
 * dataset. At the 1.x cutover the legacy structures become derivable from {@link #getRole()} and can
 * be dropped.</p>
 *
 * <p><b>The converse does not hold, and should not be assumed.</b> Not every link has a row. The
 * migration backfilled the links that existed when it ran, and the curation write path, the GEO
 * importer and the publication CLIs record one from here on — but a few writers still set
 * {@link Investigation#setPrimaryPublication} directly and do not: experiment splitting copies the
 * parent's publication to each split, and the CELLxGENE and simple-metadata loaders take theirs from
 * the source file. A link with no row means only that nothing was recorded about where it came from —
 * the same standing as a {@link PublicationAssociationSource#LEGACY} row — which is why the read path
 * returns a null association rather than inventing one.</p>
 *
 * <p><b>Precedence, not write order.</b> {@link PublicationAssociationSource#getRank()} decides who
 * wins when two writers disagree: curator &gt; the upstream record &gt; an agent &gt; an unexplained
 * legacy row. A nightly GEO re-fetch that re-proposes a link a curator rejected is refused at the
 * service, not filtered out afterwards by a list someone has to maintain. This is the property whose
 * absence let a correction made on 2026-08-13 get silently reverted by a cache rebuild on 08-14.</p>
 *
 * <p>The FK to {@link Investigation} is {@code ON DELETE CASCADE}; the FK to the reference is not, so
 * a shared {@link BibliographicReference} cannot be removed out from under a live assertion.</p>
 *
 * @see PublicationAssociationStatus
 * @see PublicationAssociationSource
 */
@Entity
@Table(name = "PUBLICATION_ASSOCIATION",
        uniqueConstraints = @UniqueConstraint(name = "UK_PUBLICATION_ASSOCIATION_INVESTIGATION_PUBLICATION",
                columnNames = { "INVESTIGATION_FK", "PUBLICATION_FK" }),
        indexes = {
                @Index(name = "IDX_PUBLICATION_ASSOCIATION_INVESTIGATION_STATUS", columnList = "INVESTIGATION_FK,STATUS"),
                @Index(name = "IDX_PUBLICATION_ASSOCIATION_PUBLICATION", columnList = "PUBLICATION_FK"),
                @Index(name = "IDX_PUBLICATION_ASSOCIATION_SOURCE", columnList = "SOURCE")
        })
public class PublicationAssociation extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVESTIGATION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_PUBLICATION_ASSOCIATION_INVESTIGATION"))
    private Investigation investigation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PUBLICATION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_PUBLICATION_ASSOCIATION_PUBLICATION"))
    private BibliographicReference publication;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, columnDefinition = "VARCHAR(16)")
    private PublicationAssociationStatus status;

    /**
     * Which slot an {@link PublicationAssociationStatus#ACCEPTED} row occupies. {@code null} on
     * {@link PublicationAssociationStatus#REJECTED} rows — "rejected as primary" versus "rejected as
     * other-relevant" is not a distinction anyone needs.
     */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", columnDefinition = "VARCHAR(16)")
    private PublicationAssociationRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE", nullable = false, columnDefinition = "VARCHAR(32)")
    private PublicationAssociationSource source;

    /**
     * The one-line, quotable basis for the claim, in a form safe to show a curator without further
     * processing: "GEO {@code !Series_pubmed_id}", "series title matches the paper title", "the paper
     * cites this accession under Data Availability". Null when the writer offered none — which is
     * itself worth seeing, and is the state every {@link PublicationAssociationSource#LEGACY} row is
     * in.
     */
    @Nullable
    @Column(name = "EVIDENCE", columnDefinition = "VARCHAR(1000)")
    private String evidence;

    /**
     * Opaque JSON array of structured evidence items backing {@link #evidence} — the same
     * agents-emitted {@code FindingEvidence} shape ({@code [{"quote":…,"source":…,"location":…}, …]})
     * that {@link Characteristic#getSupportingEvidence()} carries. Stored verbatim; Gemma neither
     * parses nor queries it, so the agents repo owns the schema and can evolve it without a Gemma
     * migration.
     */
    @Nullable
    @Column(name = "SUPPORTING_EVIDENCE", columnDefinition = "TEXT")
    private String supportingEvidence;

    /**
     * How the claim was arrived at, reusing the vocabulary annotations already use. The values that
     * carry their weight here are {@link GOEvidenceCode#IC} (a curator's own inference),
     * {@link GOEvidenceCode#TAS} (stated in a traceable source — GEO's link, the paper's data
     * availability section) and {@link GOEvidenceCode#IEA} (produced by software with no human
     * check).
     */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "EVIDENCE_CODE", columnDefinition = "VARCHAR(255)")
    private GOEvidenceCode evidenceCode;

    /**
     * Self-reported confidence in {@code [0,1]} for machine assertions, so a weak finder hit can be
     * surfaced for review rather than accepted in silence. Null for human rulings, where a number
     * would be theatre.
     */
    @Nullable
    @Column(name = "CONFIDENCE", columnDefinition = "DOUBLE")
    private Double confidence;

    /**
     * Username for a curator, or the agent run identifier for a machine assertion. Free-form
     * {@code VARCHAR} rather than an FK to {@code CONTACT}, matching
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet#getCreatedBy()}, so
     * importers and external tools can record provenance without a Gemma account.
     */
    @Nullable
    @Column(name = "ASSERTED_BY", columnDefinition = "VARCHAR(255)")
    private String assertedBy;

    /**
     * When the claim was made. Not merely audit dressing: it is how a stale machine assertion becomes
     * visible as stale, and it breaks ties between two writers of equal rank.
     */
    @Column(name = "ASSERTED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date assertedAt;

    public PublicationAssociation() {
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation( Investigation investigation ) {
        this.investigation = investigation;
    }

    public BibliographicReference getPublication() {
        return publication;
    }

    public void setPublication( BibliographicReference publication ) {
        this.publication = publication;
    }

    public PublicationAssociationStatus getStatus() {
        return status;
    }

    public void setStatus( PublicationAssociationStatus status ) {
        this.status = status;
    }

    @Nullable
    public PublicationAssociationRole getRole() {
        return role;
    }

    public void setRole( @Nullable PublicationAssociationRole role ) {
        this.role = role;
    }

    public PublicationAssociationSource getSource() {
        return source;
    }

    public void setSource( PublicationAssociationSource source ) {
        this.source = source;
    }

    @Nullable
    public String getEvidence() {
        return evidence;
    }

    public void setEvidence( @Nullable String evidence ) {
        this.evidence = evidence;
    }

    @Nullable
    public String getSupportingEvidence() {
        return supportingEvidence;
    }

    public void setSupportingEvidence( @Nullable String supportingEvidence ) {
        this.supportingEvidence = supportingEvidence;
    }

    @Nullable
    public GOEvidenceCode getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( @Nullable GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    @Nullable
    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence( @Nullable Double confidence ) {
        this.confidence = confidence;
    }

    @Nullable
    public String getAssertedBy() {
        return assertedBy;
    }

    public void setAssertedBy( @Nullable String assertedBy ) {
        this.assertedBy = assertedBy;
    }

    public Date getAssertedAt() {
        return assertedAt;
    }

    public void setAssertedAt( Date assertedAt ) {
        this.assertedAt = assertedAt;
    }

    /**
     * @return whether this row affirms the link (as opposed to recording that it was ruled out).
     */
    public boolean isAccepted() {
        return status == PublicationAssociationStatus.ACCEPTED;
    }

    @Override
    public int hashCode() {
        return Objects.hash( investigation, publication );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof PublicationAssociation ) ) return false;
        PublicationAssociation other = ( PublicationAssociation ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( investigation, other.investigation )
                && Objects.equals( publication, other.publication );
    }

    @Override
    public String toString() {
        return "PublicationAssociation[" + status
                + ( role != null ? "/" + role : "" )
                + " pub=" + ( publication != null ? publication.getId() : "null" )
                + " ee=" + ( investigation != null ? investigation.getId() : "null" )
                + " by=" + source + "]";
    }
}

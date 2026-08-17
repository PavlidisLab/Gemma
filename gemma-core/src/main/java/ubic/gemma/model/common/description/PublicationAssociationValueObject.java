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

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

import java.util.Date;

/**
 * Wire form of a {@link PublicationAssociation}: why a publication is (or is not) attached to a
 * dataset, and on whose authority.
 *
 * <p>Enums serialize as their lowercase external form ({@code "curator"}, {@code "rejected"}) rather
 * than as Java constant names, matching how {@code AnnotationSetSource} and friends already appear on
 * the wire.</p>
 */
@Schema(description = "Why a publication is attached to a dataset, or why it was ruled out: the authority behind the claim, the evidence given for it, and when it was made.")
public class PublicationAssociationValueObject {

    @Schema(description = "Whether the publication is affirmed for this dataset or was considered and ruled out.",
            allowableValues = { "accepted", "rejected" })
    private String status;

    @Schema(description = "Which slot an accepted publication occupies. Null on a rejected entry.",
            allowableValues = { "primary", "other_relevant" })
    @Nullable
    private String role;

    @Schema(description = "Who asserted this. Precedence when two writers disagree runs curator > geo_submitter_link / external_import > agent > legacy: a lower authority cannot displace a higher one, so an automated re-fetch cannot undo a curator's ruling.",
            allowableValues = { "curator", "geo_submitter_link", "external_import", "agent", "legacy" })
    private String source;

    @Schema(description = "The one-line quotable basis for the claim, e.g. \"series title matches the paper title\" or \"GEO !Series_pubmed_id\". Null when the writer gave none.")
    @Nullable
    private String evidence;

    @Schema(description = "Structured evidence items backing the one-line basis, as an opaque JSON array in the curation agents' FindingEvidence shape. Gemma stores it verbatim and does not parse it.")
    @Nullable
    private String supportingEvidence;

    @Schema(description = "How the claim was arrived at, in the same vocabulary annotations use: IC (curator inference), TAS (stated in a traceable source), IEA (produced by software, unchecked), IIA (carried in from imported data).")
    @Nullable
    private String evidenceCode;

    @Schema(description = "Self-reported confidence in [0,1] for machine assertions. Null for human rulings.")
    @Nullable
    private Double confidence;

    @Schema(description = "Username of the curator, or the run identifier of the agent, that made the claim.")
    @Nullable
    private String assertedBy;

    @Schema(description = "When the claim was made. A machine assertion much older than the dataset's last curation is a hint that it should be revisited.")
    private Date assertedAt;

    public PublicationAssociationValueObject() {
    }

    public PublicationAssociationValueObject( PublicationAssociation pa ) {
        this.status = pa.getStatus() != null ? pa.getStatus().getDbValue() : null;
        this.role = pa.getRole() != null ? pa.getRole().getDbValue() : null;
        this.source = pa.getSource() != null ? pa.getSource().getDbValue() : null;
        this.evidence = pa.getEvidence();
        this.supportingEvidence = pa.getSupportingEvidence();
        this.evidenceCode = pa.getEvidenceCode() != null ? pa.getEvidenceCode().name() : null;
        this.confidence = pa.getConfidence();
        this.assertedBy = pa.getAssertedBy();
        this.assertedAt = pa.getAssertedAt();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus( String status ) {
        this.status = status;
    }

    @Nullable
    public String getRole() {
        return role;
    }

    public void setRole( @Nullable String role ) {
        this.role = role;
    }

    public String getSource() {
        return source;
    }

    public void setSource( String source ) {
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
    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( @Nullable String evidenceCode ) {
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
}

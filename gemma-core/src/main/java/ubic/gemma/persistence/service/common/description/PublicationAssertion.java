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
package ubic.gemma.persistence.service.common.description;

import org.springframework.lang.Nullable;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.PublicationAssociationSource;

/**
 * One incoming claim about a publication: the reference, plus who is claiming it and on what basis.
 *
 * <p>A write carrier rather than an entity — callers assemble these and hand them to
 * {@link PublicationAssociationService}, which decides whether the claim is allowed to land and turns
 * it into (or merges it with) a
 * {@link ubic.gemma.model.common.description.PublicationAssociation} row. Whether the claim is an
 * acceptance or a rejection is not carried here: it comes from which argument of
 * {@link PublicationAssociationService#reconcile} the assertion is passed as, so a caller cannot
 * accidentally put a rejection in the accepted list.</p>
 *
 * <p>{@link #getSource()} is mandatory and has no default. Guessing an authority would defeat the
 * ranking that the whole design rests on — a finder's link silently recorded as a curator's would
 * outrank the curator it is meant to defer to.</p>
 */
public class PublicationAssertion {

    private final BibliographicReference publication;
    private final PublicationAssociationSource source;
    @Nullable
    private final String evidence;
    @Nullable
    private final String supportingEvidence;
    @Nullable
    private final GOEvidenceCode evidenceCode;
    @Nullable
    private final Double confidence;
    @Nullable
    private final String assertedBy;

    /**
     * @param publication        the reference being claimed. Required, and already persistent — the
     *                           caller resolves PubMed ids / DOIs first.
     * @param source             who is making the claim. Required; see the class javadoc.
     * @param evidence           the one-line quotable basis, or {@code null} if none was given.
     * @param supportingEvidence opaque JSON array of structured evidence items, or {@code null}.
     * @param evidenceCode       how the claim was arrived at, or {@code null}.
     * @param confidence         self-reported confidence in {@code [0,1]} for machine claims, else
     *                           {@code null}.
     * @param assertedBy         username or agent run id, or {@code null} to let the service stamp the
     *                           authenticated user.
     */
    public PublicationAssertion( BibliographicReference publication, PublicationAssociationSource source,
            @Nullable String evidence, @Nullable String supportingEvidence, @Nullable GOEvidenceCode evidenceCode,
            @Nullable Double confidence, @Nullable String assertedBy ) {
        this.publication = publication;
        this.source = source;
        this.evidence = evidence;
        this.supportingEvidence = supportingEvidence;
        this.evidenceCode = evidenceCode;
        this.confidence = confidence;
        this.assertedBy = assertedBy;
    }

    /**
     * A claim with a reference and an authority but no stated basis — what the pre-evidence write path
     * amounts to, and what the plain three-argument
     * {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#updatePublications}
     * produces for its callers.
     */
    public PublicationAssertion( BibliographicReference publication, PublicationAssociationSource source ) {
        this( publication, source, null, null, null, null, null );
    }

    public BibliographicReference getPublication() {
        return publication;
    }

    public PublicationAssociationSource getSource() {
        return source;
    }

    @Nullable
    public String getEvidence() {
        return evidence;
    }

    @Nullable
    public String getSupportingEvidence() {
        return supportingEvidence;
    }

    @Nullable
    public GOEvidenceCode getEvidenceCode() {
        return evidenceCode;
    }

    @Nullable
    public Double getConfidence() {
        return confidence;
    }

    @Nullable
    public String getAssertedBy() {
        return assertedBy;
    }

    /**
     * @return whether this claim states any basis at all. A claim with none is still recorded — "no
     *         reason given" is information — but it does not overwrite a stated one.
     */
    public boolean hasEvidence() {
        return evidence != null || supportingEvidence != null || evidenceCode != null;
    }

    @Override
    public String toString() {
        return "PublicationAssertion[pub=" + ( publication != null ? publication.getId() : "null" )
                + " source=" + source + ( evidence != null ? " evidence=\"" + evidence + "\"" : "" ) + "]";
    }
}

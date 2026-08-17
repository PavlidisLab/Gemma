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

/**
 * Who asserted a {@link PublicationAssociation} — the authority behind the claim, as distinct from the
 * {@link PublicationAssociation#getEvidence() evidence} they gave for it.
 * <p>
 * <b>The rank is the point.</b> Precedence between competing claims about the same
 * (experiment, publication) pair is decided by {@link #getRank()}, not by write order: a source may
 * only displace an assertion whose rank is less than or equal to its own. That is what lets an
 * automated re-fetch coexist with a human ruling instead of silently reverting it — the property a
 * denylist cannot offer, because a denylist is only honoured by the code paths that remember to read
 * it, while a rank comparison is made at the one point every writer passes through
 * ({@code PublicationAssociationService.assertAssociation}).
 * <p>
 * Concretely: GEO's {@code !Series_pubmed_id} for GSE227854 names the wrong one of the submitter's two
 * NAR 2024 papers. A curator {@link PublicationAssociationStatus#REJECTED rejects} it at rank
 * {@code 40}; the nightly GEO refresh re-proposes it at rank {@code 30} and is refused. No exclusion
 * file involved.
 */
public enum PublicationAssociationSource {

    /**
     * A human ruling. Outranks everything, including the upstream record — a curator who has read both
     * candidate papers is a better authority on which one produced the data than the submitter's own
     * cross-link.
     */
    CURATOR( 40 ),

    /**
     * GEO's {@code !Series_pubmed_id}, as written by the submitter and carried through
     * {@code GeoConverterImpl.convertPubMedIds}. Usually right, occasionally the submitter pasted the
     * wrong one of their own papers.
     */
    GEO_SUBMITTER_LINK( 30 ),

    /**
     * The upstream record's own link from a non-GEO loader (CELLxGENE collection DOI, ArrayExpress,
     * the simple-metadata loader). Same standing as {@link #GEO_SUBMITTER_LINK}: it is what the source
     * said, not what we concluded.
     */
    EXTERNAL_IMPORT( 30 ),

    /**
     * A machine assertion from a curation agent (pub_finder and friends). Outranked by both the
     * upstream record and a curator, so a finder can propose freely without being able to overwrite
     * either.
     */
    AGENT( 20 ),

    /**
     * Provenance unknown — the row predates this table and was backfilled from the bare
     * {@code INVESTIGATION.PRIMARY_PUBLICATION_FK} / {@code RELEVANT_PUBLICATIONS} link, which records
     * no authority at all. Lowest rank so anything that later states a reason wins, and so
     * "how many links still have no recorded basis?" is one {@code WHERE} clause.
     */
    LEGACY( 10 );

    private final int rank;

    PublicationAssociationSource( int rank ) {
        this.rank = rank;
    }

    /**
     * @return the authority rank; higher wins. See the class javadoc — ties go to the later write, so
     *         two curators in sequence behave the way anyone would expect.
     */
    public int getRank() {
        return rank;
    }

    /**
     * @return whether an assertion from this source may displace one currently held by {@code held}.
     */
    public boolean outranks( PublicationAssociationSource held ) {
        return this.rank >= held.rank;
    }

    /**
     * @return the lowercase external form, for use in JSON DTOs / API surfaces.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the lowercase external form back into the enum. Accepts either case.
     */
    public static PublicationAssociationSource fromDbValue( String v ) {
        return PublicationAssociationSource.valueOf( v.toUpperCase() );
    }
}

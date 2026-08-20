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
package ubic.gemma.core.ontology.model;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * One cross-reference an ontology declares from its own term to an identifier in another resource,
 * <b>with the mapping qualifier kept</b>.
 *
 * <p>The qualifier is the whole reason this type exists. MONDO annotates most of its cross-references
 * as exact / narrow / broad (an OWL axiom annotation on the {@code oboInOwl:hasDbXref} assertion, or a
 * SKOS mapping predicate), and the flat list of strings served today drops it. A narrow cross-reference
 * resolved as though it were exact is a wrong disease reported with full confidence, so the qualifier
 * has to survive the read for the mapping to be usable at all.</p>
 *
 * <p>Cross-references are many-to-many in both directions: one MONDO term carries several foreign
 * identifiers, and one foreign identifier can be claimed by more than one MONDO term. Nothing here
 * tries to collapse that.</p>
 *
 * @see ubic.gemma.core.ontology.providers.OntologyService#getCrossReferences()
 */
public class OntologyXref {

    /**
     * How the foreign identifier stands to the term that declares the cross-reference, <b>read from the
     * declaring term's point of view</b>: {@link #NARROW} means the foreign term is narrower than this
     * one, {@link #BROAD} that it is wider.
     *
     * <p>Getting that direction backwards is the failure this enum exists to prevent, because the wrong
     * answer is still a plausible disease and nothing downstream can tell.</p>
     */
    public enum Strength {
        /** Asserted equivalent — OBO {@code MONDO:equivalentTo}, or SKOS {@code exactMatch}. */
        EXACT,
        /** The foreign term names something narrower than the declaring term. */
        NARROW,
        /** The foreign term names something wider than the declaring term. */
        BROAD,
        /** Associated, with no claim that either subsumes the other. */
        RELATED,
        /**
         * A bare {@code oboInOwl:hasDbXref} with no qualifying axiom annotation.
         *
         * <p>Treated as substitutable, because on an OBO disease class a bare cross-reference is the
         * equivalence claim — the explicit {@code MONDO:equivalentTo} source marks the same thing where
         * MONDO happens to have recorded a provenance.</p>
         */
        UNSPECIFIED;

        /**
         * Whether the foreign identifier may be swapped for the declaring term without changing what is
         * being asserted.
         *
         * <p>Only {@link #EXACT} and {@link #UNSPECIFIED} may. Substituting across a narrow or broad
         * mapping silently changes the disease.</p>
         */
        public boolean isSubstitutable() {
            return this == EXACT || this == UNSPECIFIED;
        }
    }

    /**
     * Normalize a foreign identifier to {@code PREFIX:local}.
     *
     * <p>Prefix case varies within a single artifact — {@code NCIT:} and {@code NCIt:} both occur — so
     * the prefix is upper-cased and the local part left exactly as written (identifiers are not case
     * normalized; some resources use mixed-case local parts). An OBO PURL is accepted too, since one
     * ontology's cross-reference target arrives as a CURIE and another's as a URI and both have to key
     * the same index.</p>
     *
     * @return the normalized CURIE, or {@code null} if there is nothing usable in the input
     */
    @Nullable
    public static String normalizeCurie( @Nullable String identifier ) {
        if ( identifier == null ) {
            return null;
        }
        String s = identifier.trim();
        if ( s.isEmpty() ) {
            return null;
        }
        if ( s.startsWith( "http://" ) || s.startsWith( "https://" ) ) {
            int slash = s.lastIndexOf( '/' );
            int hash = s.lastIndexOf( '#' );
            String localName = s.substring( Math.max( slash, hash ) + 1 );
            int underscore = localName.indexOf( '_' );
            if ( underscore <= 0 || underscore == localName.length() - 1 ) {
                return null;
            }
            return localName.substring( 0, underscore ).toUpperCase()
                    + ":" + localName.substring( underscore + 1 );
        }
        int colon = s.indexOf( ':' );
        if ( colon <= 0 || colon == s.length() - 1 ) {
            return null;
        }
        return s.substring( 0, colon ).toUpperCase() + ":" + s.substring( colon + 1 ).trim();
    }

    private final String termUri;
    private final String curie;
    private final Strength strength;
    @Nullable
    private final String termLabel;

    public OntologyXref( String termUri, String curie, Strength strength ) {
        this( termUri, curie, strength, null );
    }

    public OntologyXref( String termUri, String curie, Strength strength, @Nullable String termLabel ) {
        this.termUri = termUri;
        this.curie = curie;
        this.strength = strength;
        this.termLabel = termLabel;
    }

    /**
     * The URI of the term that declares this cross-reference (e.g. a MONDO class).
     */
    public String getTermUri() {
        return termUri;
    }

    /**
     * The foreign identifier, normalized by {@link #normalizeCurie(String)}.
     */
    public String getCurie() {
        return curie;
    }

    /**
     * The prefix of {@link #getCurie()} — {@code DOID}, {@code NCIT}, {@code UMLS}, … — upper-cased.
     */
    public String getPrefix() {
        int colon = curie.indexOf( ':' );
        return colon > 0 ? curie.substring( 0, colon ) : curie;
    }

    public Strength getStrength() {
        return strength;
    }

    /**
     * The declaring term's label, as the read that produced this cross-reference had it; null when that
     * read had no label to give.
     *
     * <p>Carried because a caller inverting these to translate a foreign identifier immediately needs to
     * <i>name</i> what it translated to, and asking the loaded model for that name reintroduces the very
     * artifact difference reading the source was meant to escape. Measured 2026-08-18: with the xrefs
     * read from the full MONDO source, the DOID to MONDO translation succeeded for every CLO restriction
     * and 977 rows were dropped anyway, because the labels were still coming from the corpus-seeded slim
     * that by construction omits the diseases Gemma does not yet annotate. Whatever answers "which term"
     * answers "called what" in the same pass.</p>
     *
     * <p>🛑 Deliberately not part of {@link #equals(Object)}: a label is how a mapping reads, not which
     * mapping it is, and the same mapping read from a labelled and an unlabelled model is one mapping.</p>
     */
    @Nullable
    public String getTermLabel() {
        return termLabel;
    }

    @Override
    public int hashCode() {
        return Objects.hash( termUri, curie, strength );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof OntologyXref ) ) {
            return false;
        }
        OntologyXref other = ( OntologyXref ) o;
        return Objects.equals( termUri, other.termUri )
                && Objects.equals( curie, other.curie )
                && strength == other.strength;
    }

    @Override
    public String toString() {
        return termUri + " -" + strength + "-> " + curie;
    }
}

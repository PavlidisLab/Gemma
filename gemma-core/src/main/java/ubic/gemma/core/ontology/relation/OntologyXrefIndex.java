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
package ubic.gemma.core.ontology.relation;

import ubic.gemma.core.ontology.model.OntologyXref;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The cross-references of an ontology, read backwards: <b>foreign identifier → the terms that claim
 * it</b>.
 *
 * <p>This is what unblocks reading another ontology's assertions at all. CLO states which disease a
 * cell line derives from in {@code DOID}; Gemma annotates in {@code MONDO} and does not load DOID.
 * MONDO already carries the cross-references and Gemma already loads them — they have simply never
 * been inverted, so the only thing CLO's disease targets could be compared against was a label, which
 * is how {@code B-cell} and {@code lymphoma.} became bugs.</p>
 *
 * <p>🛑 <b>DOID is a space to translate out of, never one to store.</b> A DOID that reaches a stored
 * relation or an API response is a defect: MONDO is what DOID is being consolidated into, and Gemma
 * neither loads it nor annotates in it.</p>
 *
 * <p>Many-to-many in both directions, and left that way. One foreign identifier can be claimed by more
 * than one term and one term carries many identifiers; collapsing either side would invent an
 * equivalence nobody asserted.</p>
 */
public class OntologyXrefIndex {

    private static final OntologyXrefIndex EMPTY =
            new OntologyXrefIndex( Collections.emptyMap(), Collections.emptyMap() );

    public static OntologyXrefIndex empty() {
        return EMPTY;
    }

    /**
     * Invert a collection of cross-references.
     *
     * <p>Keyed on {@link OntologyXref#normalizeCurie(String)} so a lookup with {@code NCIt:C4001}, an
     * OBO PURL and {@code NCIT:C4001} all land on the same entry — prefix case genuinely varies inside
     * a single artifact.</p>
     */
    public static OntologyXrefIndex build( Collection<OntologyXref> xrefs ) {
        Map<String, Map<String, OntologyXref.Strength>> index = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        for ( OntologyXref xref : xrefs ) {
            String curie = OntologyXref.normalizeCurie( xref.getCurie() );
            if ( curie == null ) {
                continue;
            }
            index.computeIfAbsent( curie, k -> new java.util.LinkedHashMap<>() )
                    .merge( xref.getTermUri(), xref.getStrength(), OntologyXrefIndex::strongest );
            if ( xref.getTermLabel() != null ) {
                labels.putIfAbsent( xref.getTermUri(), xref.getTermLabel() );
            }
        }
        return index.isEmpty() ? EMPTY : new OntologyXrefIndex( index, labels );
    }

    private final Map<String, Map<String, OntologyXref.Strength>> index;
    private final Map<String, String> labels;

    private OntologyXrefIndex( Map<String, Map<String, OntologyXref.Strength>> index,
            Map<String, String> labels ) {
        this.index = index;
        this.labels = labels;
    }

    /**
     * The terms that may stand in for a foreign identifier.
     *
     * <p>Only {@link OntologyXref.Strength#isSubstitutable() substitutable} mappings, because that is
     * what a caller holding a DOID and wanting a MONDO term is asking for. Resolving across a narrow or
     * broad mapping returns a different disease with no signal that it is a different disease.</p>
     *
     * @return the matching term URIs, in the order the cross-references were read; empty when nothing
     * matches, which is an ordinary answer and not an error
     */
    public Set<String> resolve( @Nullable String foreignIdentifier ) {
        return resolve( foreignIdentifier, true );
    }

    /**
     * @param substitutableOnly false to also return terms reached through a narrow, broad or related
     *                          mapping — for a caller that widens a query rather than one that picks a
     *                          term to store
     */
    public Set<String> resolve( @Nullable String foreignIdentifier, boolean substitutableOnly ) {
        String curie = OntologyXref.normalizeCurie( foreignIdentifier );
        if ( curie == null ) {
            return Collections.emptySet();
        }
        Map<String, OntologyXref.Strength> hits = index.get( curie );
        if ( hits == null ) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for ( Map.Entry<String, OntologyXref.Strength> e : hits.entrySet() ) {
            if ( !substitutableOnly || e.getValue().isSubstitutable() ) {
                result.add( e.getKey() );
            }
        }
        return result;
    }

    /**
     * The qualifier a particular mapping carries, or {@code null} when there is no such mapping.
     */
    @Nullable
    public OntologyXref.Strength getStrength( @Nullable String foreignIdentifier, String termUri ) {
        String curie = OntologyXref.normalizeCurie( foreignIdentifier );
        if ( curie == null ) {
            return null;
        }
        Map<String, OntologyXref.Strength> hits = index.get( curie );
        return hits != null ? hits.get( termUri ) : null;
    }

    /**
     * What a term resolved through this index is called, according to the same artifact that resolved
     * it; null when that artifact carried no usable label for it.
     *
     * <p>🛑 <b>Resolving and naming have to come from the same read.</b> Splitting them is what made
     * the fix to this index only half a fix: with the cross-references inverted from the full MONDO
     * source, every DOID a CLO restriction names translated successfully, and 977 of those rows were
     * then dropped because the <i>label</i> was still being looked up in the corpus-seeded slim, which
     * omits the diseases Gemma does not yet annotate — the exact set a foreign identifier is being
     * translated to reach. The identifier resolved and the term stayed nameless.</p>
     *
     * <p>Obsolete terms are already excluded upstream, when the labels are read.</p>
     */
    @Nullable
    public String labelOf( @Nullable String termUri ) {
        return termUri != null ? labels.get( termUri ) : null;
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    /**
     * How many distinct foreign identifiers the index holds.
     */
    public int size() {
        return index.size();
    }

    /**
     * Distinct foreign identifiers per prefix — {@code DOID}, {@code NCIT}, {@code UMLS}, … — for
     * reporting what a load actually covers. Sorted, so two runs are comparable by eye.
     */
    public Map<String, Integer> countsByPrefix() {
        Map<String, Integer> counts = new TreeMap<>();
        for ( String curie : index.keySet() ) {
            int colon = curie.indexOf( ':' );
            counts.merge( colon > 0 ? curie.substring( 0, colon ) : curie, 1, Integer::sum );
        }
        return counts;
    }

    private static OntologyXref.Strength strongest( OntologyXref.Strength a, OntologyXref.Strength b ) {
        if ( a == OntologyXref.Strength.EXACT || b == OntologyXref.Strength.EXACT ) {
            return OntologyXref.Strength.EXACT;
        }
        return a == OntologyXref.Strength.UNSPECIFIED ? b : a;
    }
}

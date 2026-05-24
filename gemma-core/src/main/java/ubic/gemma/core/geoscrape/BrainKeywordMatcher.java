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
package ubic.gemma.core.geoscrape;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.providers.UberonOntologyService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Cheesy v1 brain-related keyword matcher. Cross-checks the curated keyword
 * set against title / summary / sample details / MeSH headings.
 *
 * <p>If the supplied {@link UberonOntologyService} is loaded at first
 * evaluation, descendant term labels of UBERON:0000955 (brain) are folded
 * into the keyword set. Otherwise the curated set alone is used — Uberon
 * isn't loaded in unit-test contexts.</p>
 *
 * <p>v2 will replace this with a curation-agent pass + gene-set-fetch skill
 * output; see {@link GeoRecordMatcher} javadoc.</p>
 */
@Component
public class BrainKeywordMatcher implements GeoRecordMatcher {

    private static final Log log = LogFactory.getLog( BrainKeywordMatcher.class );

    /** {@code UBERON:0000955} — brain. */
    static final String UBERON_BRAIN_URI = "http://purl.obolibrary.org/obo/UBERON_0000955";

    /**
     * Curated keyword list — lowercase. Hand-picked common terms that escape
     * a strict Uberon descendant walk (e.g. cell-type labels, common synonyms).
     */
    static final Set<String> CURATED_KEYWORDS = Collections.unmodifiableSet( new LinkedHashSet<>( Arrays.asList(
            "brain", "cortex", "cortical", "hippocampus", "hippocampal", "amygdala", "striatum", "striatal",
            "cerebellum", "cerebellar", "thalamus", "thalamic", "neuron", "neuronal", "neural", "glia", "glial",
            "astrocyte", "microglia", "oligodendrocyte", "brainstem", "midbrain", "forebrain", "hindbrain",
            "prefrontal", "hypothalamus", "substantia nigra", "nucleus accumbens"
    ) ) );

    @Nullable
    private final UberonOntologyService uberon;

    private volatile Set<String> keywords;
    private volatile boolean initialized = false;

    @Autowired
    public BrainKeywordMatcher( @Nullable UberonOntologyService uberon ) {
        this.uberon = uberon;
    }

    /** Test seam: no-arg constructor uses the curated set only. */
    public BrainKeywordMatcher() {
        this( null );
    }

    @Override
    public String name() {
        return "brain";
    }

    @Override
    public MatchResult evaluate( GeoRecord r ) {
        if ( r == null ) return MatchResult.miss();
        Set<String> kw = ensureKeywords();
        // Lower-case the haystacks once; search each keyword as a substring.
        StringBuilder haystack = new StringBuilder( 1024 );
        appendLowercase( haystack, r.getTitle() );
        haystack.append( ' ' );
        appendLowercase( haystack, r.getSummary() );
        haystack.append( ' ' );
        appendLowercase( haystack, r.getSampleDetails() );
        haystack.append( ' ' );
        appendLowercase( haystack, r.getOverallDesign() );
        haystack.append( ' ' );
        appendLowercase( haystack, joinCollection( r.getMeshHeadings() ) );
        String hay = haystack.toString();
        for ( String k : kw ) {
            if ( hay.contains( k ) ) {
                return MatchResult.hit( "brain keyword: " + k );
            }
        }
        return MatchResult.miss();
    }

    private Set<String> ensureKeywords() {
        if ( initialized ) return keywords;
        synchronized ( this ) {
            if ( initialized ) return keywords;
            Set<String> kw = new LinkedHashSet<>( CURATED_KEYWORDS );
            if ( uberon != null && uberon.isOntologyLoaded() ) {
                try {
                    OntologyTerm brain = uberon.getTerm( UBERON_BRAIN_URI );
                    if ( brain != null ) {
                        Set<OntologyTerm> kids = uberon.getChildren(
                                Collections.singleton( brain ), false, false );
                        for ( OntologyTerm t : kids ) {
                            String lbl = t.getLabel();
                            if ( lbl != null && !lbl.isEmpty() ) {
                                kw.add( lbl.toLowerCase( Locale.ROOT ) );
                            }
                        }
                    }
                } catch ( RuntimeException e ) {
                    // Ontology may not be enabled / loaded — fall back to curated set silently.
                    log.warn( "BrainKeywordMatcher: failed to fold in Uberon descendants: " + e.getMessage() );
                }
            }
            keywords = Collections.unmodifiableSet( kw );
            initialized = true;
            return keywords;
        }
    }

    /** Visible for testing: exposes the resolved keyword set. */
    Set<String> getKeywords() {
        return ensureKeywords();
    }

    private static void appendLowercase( StringBuilder sb, @Nullable String s ) {
        if ( s == null ) return;
        sb.append( s.toLowerCase( Locale.ROOT ) );
    }

    private static String joinCollection( @Nullable Collection<String> c ) {
        if ( c == null || c.isEmpty() ) return "";
        return String.join( " ", c );
    }
}

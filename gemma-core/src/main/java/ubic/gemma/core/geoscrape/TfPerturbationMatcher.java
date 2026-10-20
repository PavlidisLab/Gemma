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

import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cheesy v1 transcription-factor perturbation matcher. Flags when both:
 * <ul>
 *   <li>a perturbation verb (knockdown, knockout, overexpression, silencing,
 *       siRNA, shRNA, CRISPR, dCas9, TALEN, zinc-finger) appears in title/summary; AND</li>
 *   <li>a curated TF symbol from a short hand-picked list appears in title/summary.</li>
 * </ul>
 *
 * <p>v2 will delegate to the curation-agent + the gene-set-fetch skill output
 * (Reactome / curated TF gene set) instead of this curated list — see
 * {@link GeoRecordMatcher} javadoc.</p>
 */
@Component
public class TfPerturbationMatcher implements GeoRecordMatcher {

    /** Hand-picked common TFs — symbol uppercase. */
    static final Set<String> TF_SYMBOLS = Collections.unmodifiableSet( new LinkedHashSet<>( Arrays.asList(
            "TP53", "MYC", "KLF4", "OCT4", "POU5F1", "NF-KB", "NFKB1", "RELA",
            "SOX2", "NANOG", "PAX6", "CREB1", "REST", "BRN2", "POU3F2",
            "MEF2C", "NEUROD1", "FOXP2", "GATA3", "BCL11B", "ASCL1", "OLIG2",
            "RUNX1", "STAT3", "FOXA2", "FOXO1", "EOMES", "TBR1", "FOXP3",
            "HNF4A", "PPARG", "SOX9", "TBX21", "GATA1", "GATA4", "ETV1"
    ) ) );

    private static final Pattern PERTURB_VERB = Pattern.compile(
            "\\b(knock[- ]?down|knock[- ]?out|over[- ]?expression|silenc(?:e|ing)|sirna|shrna|crispr|dcas9|talen|zinc[- ]?finger|repressor|activation)\\b",
            Pattern.CASE_INSENSITIVE );

    @Override
    public String name() {
        return "tfperturb";
    }

    @Override
    public MatchResult evaluate( GeoRecord r ) {
        if ( r == null ) return MatchResult.miss();
        String title = r.getTitle() == null ? "" : r.getTitle();
        String summary = r.getSummary() == null ? "" : r.getSummary();
        String hay = title + " " + summary;
        if ( !PERTURB_VERB.matcher( hay ).find() ) {
            return MatchResult.miss();
        }
        // TF symbol match — token-boundary against the original-case haystack
        // so we don't match the symbol embedded inside an unrelated word.
        String hayUpper = hay.toUpperCase( Locale.ROOT );
        for ( String tf : TF_SYMBOLS ) {
            if ( containsWord( hayUpper, tf ) ) {
                return MatchResult.hit( "TF " + tf + " + perturbation verb" );
            }
        }
        return MatchResult.miss();
    }

    private static boolean containsWord( String hay, String needle ) {
        int idx = 0;
        while ( ( idx = hay.indexOf( needle, idx ) ) >= 0 ) {
            int before = idx - 1;
            int after = idx + needle.length();
            boolean leftOk = before < 0 || !isWordChar( hay.charAt( before ) );
            boolean rightOk = after >= hay.length() || !isWordChar( hay.charAt( after ) );
            if ( leftOk && rightOk ) return true;
            idx += needle.length();
        }
        return false;
    }

    private static boolean isWordChar( char c ) {
        return Character.isLetterOrDigit( c ) || c == '_';
    }
}

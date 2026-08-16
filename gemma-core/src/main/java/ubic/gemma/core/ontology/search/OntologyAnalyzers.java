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
package ubic.gemma.core.ontology.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;

import java.io.Reader;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The single place the ontology Lucene analysis recipe is defined.
 *
 * <p>Two indexes consume it — the Jena-model index behind the real ontologies and the flat
 * {@code LexicalOntologyIndex} behind Cellosaurus / MGI / the MeSH synonym table. Both had built
 * their own {@code new EnglishAnalyzer( getDefaultStopSet(), stemExclusion )}, identically, and
 * both hand the SAME analyzer instance to the {@code IndexWriter} and to the {@code QueryParser}.
 * That symmetry is what makes the code folding below correct, and it is why this lives in one
 * factory rather than being applied twice.</p>
 */
public final class OntologyAnalyzers {

    private OntologyAnalyzers() {
    }

    /**
     * Designation-shaped runs: a short alphabetic vendor prefix, an optional single space or
     * hyphen, then a run of at least three digits (with any further hyphenated digit groups
     * carried along, so {@code BAY 43-9006} folds whole).
     *
     * <p>The bounds are the narrow part. A prefix cap of five characters, and a number that is
     * either three digits or hyphenated into groups, keep ordinary prose out: {@code interleukin 6}
     * has too long a prefix and too short a number, {@code CD 34} too short a number,
     * {@code vitamin D3} both. Only strings that already look like coined identifiers are touched.</p>
     *
     * <p>The hyphenated alternative is not decoration — sorafenib's code is {@code BAY 43-9006},
     * whose first digit group is two characters. A flat three-digit floor silently excluded exactly
     * the compound that motivated this.</p>
     */
    private static final Pattern CODE_RUN = Pattern.compile(
            "(?i)\\b([a-z]{1,5})[\\s-]?([0-9]+(?:-[0-9]+)+|[0-9]{3,})\\b" );

    /**
     * An {@link EnglishAnalyzer} with the given stem-exclusion set, plus separator-insensitive
     * folding of designation-shaped runs at BOTH index and query time.
     *
     * <p>The problem it solves: CHEBI stores sunitinib's trial code as {@code SU-11248} and
     * tofacitinib's as {@code CP 690550}, while submitters write {@code SU11248} and
     * {@code CP690550}. {@link org.apache.lucene.analysis.standard.StandardTokenizer} splits on the
     * separator, so the stored form indexes as {@code su} + {@code 11248} and the written form as
     * the single token {@code su11248} — no overlap, no match. A search for {@code SU11248}
     * returned only EFO's {@code response to sunitinib}, never the compound, even though the
     * compound is loaded and used 20 times in the corpus.</p>
     *
     * <p>Folding happens in a {@link PatternReplaceCharFilter} ahead of tokenization, because by
     * the time a token filter runs the separator is already gone and the two halves are separate
     * tokens. Both sides of the search pass through the same filter, so the transform is symmetric
     * and nothing that matched before stops matching: {@code SU-11248} and {@code SU11248} both
     * become {@code su11248}, and each still finds the other's spelling.</p>
     *
     * <p>Wrapping rather than subclassing is deliberate — {@link EnglishAnalyzer} is final, and
     * {@link AnalyzerWrapper} adds the reader filter without restating the Porter chain, so the
     * analysis recipe cannot drift from Lucene's.</p>
     *
     * @param excludedFromStemming words to protect from the Porter stemmer; may be {@code null}
     */
    public static Analyzer english( Set<String> excludedFromStemming ) {
        CharArraySet stemExclusion = new CharArraySet(
                excludedFromStemming == null ? Collections.emptySet() : excludedFromStemming,
                false /* not case-sensitive */
        );
        EnglishAnalyzer delegate = new EnglishAnalyzer( EnglishAnalyzer.getDefaultStopSet(), stemExclusion );
        return new CodeFoldingAnalyzer( delegate );
    }

    /**
     * Visible for testing: applies the same folding the analyzer applies, so a test can assert on
     * the normalised form directly rather than round-tripping through an index.
     */
    public static String foldCodeRuns( String text ) {
        return text == null ? null : CODE_RUN.matcher( text ).replaceAll( "$1$2" );
    }

    private static final class CodeFoldingAnalyzer extends AnalyzerWrapper {

        private final Analyzer delegate;

        private CodeFoldingAnalyzer( Analyzer delegate ) {
            super( delegate.getReuseStrategy() );
            this.delegate = delegate;
        }

        @Override
        protected Analyzer getWrappedAnalyzer( String fieldName ) {
            return delegate;
        }

        @Override
        protected Reader wrapReader( String fieldName, Reader reader ) {
            return new PatternReplaceCharFilter( CODE_RUN, "$1$2", reader );
        }

        @Override
        public void close() {
            try {
                delegate.close();
            } finally {
                super.close();
            }
        }
    }
}

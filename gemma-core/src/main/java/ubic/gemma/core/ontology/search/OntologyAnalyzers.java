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
     * has too long a prefix and too short a number, {@code vitamin D3} both. Only strings that
     * already look like coined identifiers are touched.</p>
     *
     * <p>The hyphenated alternative is not decoration — sorafenib's code is {@code BAY 43-9006},
     * whose first digit group is two characters. A flat three-digit floor silently excluded exactly
     * the compound that motivated this.</p>
     *
     * @see #SHORT_CODE_RUN for the one- and two-digit designations this deliberately leaves alone
     */
    private static final Pattern CODE_RUN = Pattern.compile(
            "(?i)\\b([a-z]{1,5})[\\s-]?([0-9]+(?:-[0-9]+)+|[0-9]{3,})\\b" );

    /**
     * The same folding for designations whose number is only one or two digits — {@code MEC-1},
     * {@code IL-6}, {@code EMT-6}, {@code AR-12} — bounded so that it cannot reach into prose or
     * into a systematic chemical name.
     *
     * <p>Why it is separate from {@link #CODE_RUN}: a one-digit run is the shape CLO uses for cell
     * lines, and it is also the shape IUPAC uses for locants. {@code MEC-1 cell} and
     * {@code 2-(1H-indol-3-yl)ethanamine} differ only in where the run sits, so the bound has to be
     * positional rather than a looser digit floor. Two constraints do that:</p>
     *
     * <ul>
     * <li><b>Whitespace-delimited on both sides.</b> A locant is always welded into a longer
     * hyphenated chain — {@code indol-3-yl}, {@code pregn-4-ene}, {@code oxo-7-(piperazin} — so
     * requiring whitespace (or a string boundary) before the prefix and after the digits excludes
     * every one of them, while {@code MEC-1 cell} and a bare {@code IL-6} qualify. It also excludes
     * dotted numbering: the {@code 1} in {@code EC 1.5.1.3 (dihydrofolate reductase) inhibitor} is
     * followed by a period, not whitespace.</li>
     * <li><b>A three-character prefix cap</b>, against five above. The words that carry a small
     * number in ontology prose are longer than that — {@code type 2}, {@code grade 3},
     * {@code stage 4}, {@code group 1}, {@code class 2}, {@code digit 1}, {@code alpha-2} — and the
     * designations are shorter: {@code IL}, {@code CD}, {@code AE}, {@code MEC}, {@code EMT},
     * {@code PAX}, {@code SOX}. The cap is what keeps {@code type 2 diabetes mellitus} whole.</li>
     * </ul>
     *
     * <p>Measured over the 1,530,566 labels and synonyms of the twelve loaded ontologies, the two
     * bounds together touch 8,156 strings (0.53%): 728 in CLO, all of them cell lines, and 1,201 in
     * CHEBI, all of them trial codes of the {@code IPA-3} / {@code H-89} / {@code AR-12} shape that
     * {@link #CODE_RUN}'s digit floor had been missing. Dropping the digit floor without the
     * positional bound instead rewrites 27% of CHEBI, because that is how much of CHEBI is
     * systematic nomenclature.</p>
     *
     * <p>A short prefix is genuinely ambiguous in a handful of cases — {@code of 6} and
     * {@code and 49} fold too, 429 times across the whole corpus. They are left in rather than
     * excluded by a stop list, because the stop list would also have to exclude {@code a}, and
     * {@code A 72 cell} is a real CLO cell line. The welds are symmetric and cost nothing: a query
     * spelling them the same way folds the same way.</p>
     */
    private static final Pattern SHORT_CODE_RUN = Pattern.compile(
            "(?i)(?<!\\S)([a-z]{1,3})[\\s-]([0-9]{1,2})(?!\\S)" );

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
     * compound is loaded and used 20 times in the corpus. The same split is why {@code MEC1} could
     * not reach the CLO term labelled {@code MEC-1 cell}, and so could not reach the disease
     * restrictions only CLO carries.</p>
     *
     * <p>Folding happens in a {@link PatternReplaceCharFilter} ahead of tokenization, because by
     * the time a token filter runs the separator is already gone and the two halves are separate
     * tokens. Both sides of the search pass through the same filter, so the transform is symmetric
     * and nothing that matched before stops matching: {@code SU-11248} and {@code SU11248} both
     * become {@code su11248}, and each still finds the other's spelling.</p>
     *
     * <p>The two patterns are chained rather than merged because their bounds differ — see
     * {@link #SHORT_CODE_RUN}. Order does not matter for correctness; the long-run fold runs first
     * so the short-run fold sees text already free of {@code BAY 43-9006}-shaped runs.</p>
     *
     * <p><b>Changing either pattern requires a forced reindex.</b> An analyzer change does not
     * invalidate an existing Lucene index, so a restart alone leaves the old tokens in place and
     * the query side folding against them — visible as the model and the search disagreeing.
     * {@code /admin/ontologies/{name}/refresh?forceIndexing=true} is what rebuilds it.</p>
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
        if ( text == null ) {
            return null;
        }
        String folded = CODE_RUN.matcher( text ).replaceAll( "$1$2" );
        return SHORT_CODE_RUN.matcher( folded ).replaceAll( "$1$2" );
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
            return new PatternReplaceCharFilter( SHORT_CODE_RUN, "$1$2",
                    new PatternReplaceCharFilter( CODE_RUN, "$1$2", reader ) );
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

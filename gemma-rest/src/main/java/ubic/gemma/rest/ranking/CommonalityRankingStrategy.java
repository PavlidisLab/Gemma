/*
 * The gemma-rest project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest.ranking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Orders hits by how commonly prior curators wrote the query string to mean each candidate.
 *
 * <p>This asks a different question from {@link UsageWeightedRankingStrategy}. A usage count is
 * about the TERM — how much does Gemma use this URI at all — and answers "which compound is
 * meant". The per-string prior is about the STRING: of everyone who actually wrote the words being
 * searched for, how many meant each candidate. On the production corpus a search for {@code dmso}
 * finds that 508 experiments writing "DMSO" meant the compound and 16 meant
 * {@code reference substance role}, which separates them even though both are legitimate hits and
 * both terms are well used in their own right.</p>
 *
 * <p>Score: {@code rankWeight * (1 / (1 + originalRank)) + priorWeight * priorScore(prior, maxPrior)
 * - shapeWeight * designationPenalty(label)}. Higher sorts earlier; ties resolve by original
 * Lucene rank ascending, so the strategy is stable and never reorders hits it cannot separate.
 * The prior is scored relative to the strongest candidate in the same result set — see
 * {@link #priorScore} for why an absolute scale cannot work here.</p>
 *
 * <h2>Two constraints the measurement imposed</h2>
 * <ol>
 *   <li><strong>It is a frequency comparison, not a preference for synonyms.</strong> The tempting
 *   shortcut — prefer whichever string is a synonym rather than the preferred label — is wrong:
 *   {@code DMSO} (508) beats the label {@code dimethyl sulfoxide}, but {@code EtOH} (38) LOSES to
 *   the label {@code ethanol} (65). Only the counts can tell those apart, so only the counts are
 *   consulted.</li>
 *   <li><strong>A compound nobody has curated scores zero everywhere.</strong> Corpus frequency is
 *   silent on a drug the corpus has never seen — the case that matters most when annotating
 *   something for the first time. Rather than let a zero-information tie fall through to URI
 *   string order, {@link #designationPenalty} demotes labels shaped like systematic chemical
 *   names, which nobody writes when they mean the drug.</li>
 * </ol>
 *
 * <p>Purely numeric query strings never reach here with a populated prior: a count of {@code 24}
 * pools unrelated doses, timepoints and replicate numbers, so the tally is refused at the DAO.</p>
 *
 * <p>The prior is corpus curation history, so it reflects whatever is in the database, escrowed
 * experiments included. That is the same footing as {@code ?rank=usage} and fine for ordering a
 * live search; it is NOT a held-out signal and must not be treated as one when measuring a
 * resolver against an escrow.</p>
 */
@Component("commonality")
public class CommonalityRankingStrategy implements AnnotationSearchRankingStrategy {

    public static final String NAME = "commonality";

    private final double rankWeight;
    private final double priorWeight;
    private final double shapeWeight;

    @Autowired
    public CommonalityRankingStrategy(
            @Value("${gemma.rest.annotationSearch.commonality.rankWeight:0.35}") double rankWeight,
            @Value("${gemma.rest.annotationSearch.commonality.priorWeight:0.65}") double priorWeight,
            @Value("${gemma.rest.annotationSearch.commonality.shapeWeight:0.25}") double shapeWeight ) {
        this.rankWeight = rankWeight;
        this.priorWeight = priorWeight;
        this.shapeWeight = shapeWeight;
    }

    /**
     * Convenience constructor pinning the shape weight to the production default. For tests and
     * callers that only care about the rank/prior balance.
     */
    public CommonalityRankingStrategy( double rankWeight, double priorWeight ) {
        this( rankWeight, priorWeight, 0.25 );
    }

    @Override
    public List<CharacteristicValueObject> rank( String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri ) {
        // No prior supplied — every hit scores identically on the term-evidence component, so the
        // only honest thing left is the shape demotion over the incoming Lucene order.
        return rank( originalQuery, rawHits, usageCountsByUri, java.util.Collections.emptyMap() );
    }

    @Override
    public List<CharacteristicValueObject> rank( String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri,
            Map<String, Integer> stringPriorByUri ) {
        int n = rawHits.size();
        // The prior is scored against the strongest candidate in this result set rather than an
        // absolute scale. A fixed saturation point cannot serve both ends of the range seen in the
        // corpus: set it low enough that a term written 30 times counts, and 16 becomes
        // indistinguishable from 508 — which is the entire DMSO-versus-reference-substance-role
        // decision, thrown away. Relative scoring asks the question the prior actually answers:
        // of everyone who wrote this string, what share meant this candidate?
        int maxPrior = 0;
        for ( CharacteristicValueObject hit : rawHits ) {
            String uri = hit.getValueUri();
            Integer p = uri != null ? stringPriorByUri.get( uri ) : null;
            if ( p != null && p > maxPrior ) {
                maxPrior = p;
            }
        }
        List<Scored> scored = new ArrayList<>( n );
        for ( int i = 0; i < n; i++ ) {
            CharacteristicValueObject hit = rawHits.get( i );
            int prior = 0;
            String uri = hit.getValueUri();
            if ( uri != null ) {
                Integer p = stringPriorByUri.get( uri );
                if ( p != null ) {
                    prior = p;
                }
            }
            double score = rankWeight * ( 1.0 / ( 1.0 + i ) )
                    + priorWeight * priorScore( prior, maxPrior )
                    - shapeWeight * designationPenalty( hit.getValue() );
            scored.add( new Scored( hit, i, score ) );
        }
        scored.sort( Comparator
                .comparingDouble( ( Scored s ) -> s.score ).reversed()
                .thenComparingInt( s -> s.originalRank ) );
        List<CharacteristicValueObject> out = new ArrayList<>( n );
        for ( Scored s : scored ) {
            out.add( s.hit );
        }
        return out;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean requiresStringPrior() {
        return true;
    }

    /**
     * Score a candidate's per-string count in {@code [0, 1]} against the strongest count in the
     * same result set. Taken on a log scale, so an order-of-magnitude gap (508 against 16) is
     * decisive while a near-tie (65 against 38) leaves the incoming relevance order to settle it —
     * the counts are evidence about which candidate was meant, not a mandate to reorder on any
     * difference at all. Returns {@code 0} when the string is unattested for this candidate, which
     * is the ordinary case for a compound being annotated for the first time. Visible for testing.
     */
    static double priorScore( int prior, int maxPrior ) {
        if ( prior <= 0 || maxPrior <= 0 ) return 0.0;
        if ( prior >= maxPrior ) return 1.0;
        return Math.log1p( prior ) / Math.log1p( maxPrior );
    }

    /**
     * Score in {@code [0, 1]} for how much a label looks like a systematic chemical name rather
     * than something a curator would type — {@code (2S,3R)-2-amino-3-hydroxybutanoic acid} versus
     * {@code tobramycin}.
     *
     * <p>This only decides anything when the corpus prior is silent for every candidate, which is
     * exactly the first-time-annotation case. The signals are deliberately crude and additive: no
     * single one is trusted, and a plain drug name trips none of them. Stereo-descriptor and
     * locant punctuation is the strongest tell, since ordinary English names do not carry
     * parenthesised digits or comma-separated numbers. Visible for testing.</p>
     */
    static double designationPenalty( String label ) {
        if ( label == null ) return 0.0;
        String s = label.trim();
        if ( s.isEmpty() ) return 0.0;
        double signals = 0.0;
        // Locants and stereo descriptors: "(2S,3R)-", "1,3-dimethyl", "[a]".
        if ( s.matches( ".*[(\\[]\\d.*" ) || s.matches( ".*\\d,\\d.*" ) ) {
            signals += 2.0;
        }
        // Systematic names chain morphemes with hyphens far more than common names do.
        long hyphens = s.chars().filter( c -> c == '-' ).count();
        if ( hyphens >= 3 ) {
            signals += 1.0;
        }
        // Digits scattered through a long string, as opposed to a trailing series number.
        long digits = s.chars().filter( Character::isDigit ).count();
        if ( digits >= 3 && s.length() > 25 ) {
            signals += 1.0;
        }
        // Sheer length: IUPAC names run long, curator-typed names rarely do.
        if ( s.length() > 45 ) {
            signals += 1.0;
        }
        return Math.min( 1.0, signals / 3.0 );
    }

    private static final class Scored {
        final CharacteristicValueObject hit;
        final int originalRank;
        final double score;

        Scored( CharacteristicValueObject hit, int originalRank, double score ) {
            this.hit = hit;
            this.originalRank = originalRank;
            this.score = score;
        }
    }
}

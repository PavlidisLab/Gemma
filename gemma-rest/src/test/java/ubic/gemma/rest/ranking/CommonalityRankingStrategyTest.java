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

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-data tests for {@code ?rank=commonality}. No Spring, no database — the strategy is a scoring
 * function over a hit list plus a prior map, so every case here is stated as literal counts.
 * <p>
 * The counts used are the ones measured against the production corpus (see the DAO javadoc), so a
 * change that inverts one of these orderings is a real behavioural change, not a fixture artifact.
 */
public class CommonalityRankingStrategyTest {

    private static final String DMSO = "http://purl.obolibrary.org/obo/CHEBI_28262";
    private static final String REFERENCE_SUBSTANCE_ROLE = "http://purl.obolibrary.org/obo/OBI_0000025";
    private static final String ETHANOL = "http://purl.obolibrary.org/obo/CHEBI_16236";

    private final CommonalityRankingStrategy strategy = new CommonalityRankingStrategy( 0.35, 0.65 );

    private static CharacteristicValueObject hit( String value, String uri ) {
        CharacteristicValueObject vo = new CharacteristicValueObject();
        vo.setValue( value );
        vo.setValueUri( uri );
        return vo;
    }

    private static List<String> urisOf( List<CharacteristicValueObject> hits ) {
        List<String> out = new ArrayList<>( hits.size() );
        for ( CharacteristicValueObject h : hits ) {
            out.add( h.getValueUri() );
        }
        return out;
    }

    @Test
    public void prior_separates_two_legitimate_hits_that_usage_cannot() {
        // Both terms are well used in the corpus in their own right, so a usage count does not
        // choose between them. The string does: of everyone who wrote "DMSO", 508 experiments
        // meant the compound and 16 meant the role.
        List<CharacteristicValueObject> hits = Arrays.asList(
                hit( "reference substance role", REFERENCE_SUBSTANCE_ROLE ),
                hit( "dimethyl sulfoxide", DMSO ) );
        Map<String, Integer> prior = new HashMap<>();
        prior.put( DMSO, 508 );
        prior.put( REFERENCE_SUBSTANCE_ROLE, 16 );

        List<CharacteristicValueObject> ranked =
                strategy.rank( "dmso", hits, Collections.emptyMap(), prior );

        assertThat( urisOf( ranked ) ).containsExactly( DMSO, REFERENCE_SUBSTANCE_ROLE );
    }

    @Test
    public void it_is_a_frequency_comparison_not_a_preference_for_synonyms() {
        // The tempting shortcut — prefer whichever candidate matched on a synonym rather than its
        // preferred label — is wrong in both directions, and the corpus proves it: for `dmso` the
        // synonym wins (508 against the label's 85), but for `ethanol` the label wins (65 against
        // `EtOH`'s 38). The strategy is given no way to tell a label from a synonym, so the only
        // thing that can flip the winner is the counts. Same two hits, counts swapped, order
        // inverts.
        List<CharacteristicValueObject> hits = Arrays.asList(
                hit( "reference substance role", REFERENCE_SUBSTANCE_ROLE ),
                hit( "dimethyl sulfoxide", DMSO ) );

        Map<String, Integer> compoundWrittenMore = new HashMap<>();
        compoundWrittenMore.put( DMSO, 508 );
        compoundWrittenMore.put( REFERENCE_SUBSTANCE_ROLE, 16 );

        Map<String, Integer> roleWrittenMore = new HashMap<>();
        roleWrittenMore.put( DMSO, 16 );
        roleWrittenMore.put( REFERENCE_SUBSTANCE_ROLE, 508 );

        assertThat( urisOf( strategy.rank( "q", hits, Collections.emptyMap(), compoundWrittenMore ) ) )
                .containsExactly( DMSO, REFERENCE_SUBSTANCE_ROLE );
        assertThat( urisOf( strategy.rank( "q", hits, Collections.emptyMap(), roleWrittenMore ) ) )
                .as( "nothing but the counts may decide the winner" )
                .containsExactly( REFERENCE_SUBSTANCE_ROLE, DMSO );
    }

    @Test
    public void a_low_prior_hit_can_still_be_overtaken_on_lucene_rank() {
        // The prior saturates, so it informs the order without dictating it: a candidate attested
        // once should not leapfrog the whole result set on the strength of a single annotation.
        List<CharacteristicValueObject> hits = Arrays.asList(
                hit( "ethanol", ETHANOL ),
                hit( "dimethyl sulfoxide", DMSO ) );
        Map<String, Integer> prior = new HashMap<>();
        prior.put( ETHANOL, 65 );
        prior.put( DMSO, 1 );

        assertThat( urisOf( strategy.rank( "ethanol", hits, Collections.emptyMap(), prior ) ) )
                .containsExactly( ETHANOL, DMSO );
    }

    @Test
    public void prior_is_scored_against_the_best_candidate_in_the_same_result_set() {
        // An order-of-magnitude gap must be decisive...
        assertThat( CommonalityRankingStrategy.priorScore( 508, 508 ) ).isEqualTo( 1.0 );
        assertThat( CommonalityRankingStrategy.priorScore( 16, 508 ) ).isLessThan( 0.5 );
        // ...while a near-tie must not be, leaving the incoming relevance order to settle it.
        assertThat( CommonalityRankingStrategy.priorScore( 38, 65 ) ).isGreaterThan( 0.8 );
        // An unattested string is the first-time-annotation case, not a negative signal.
        assertThat( CommonalityRankingStrategy.priorScore( 0, 508 ) ).isEqualTo( 0.0 );
        // Nothing attested anywhere: no candidate may be credited over another.
        assertThat( CommonalityRankingStrategy.priorScore( 0, 0 ) ).isEqualTo( 0.0 );
    }

    @Test
    public void systematic_chemical_names_are_demoted_when_the_corpus_is_silent() {
        // The first-time-annotation case: nobody has written "tobramycin", so the prior is empty
        // for every candidate and the ordering would otherwise fall through to Lucene's. The
        // IUPAC-shaped label is the one nobody would ever type, so it goes last.
        String plain = "http://purl.obolibrary.org/obo/CHEBI_28864";
        String systematic = "http://purl.obolibrary.org/obo/CHEBI_999999";
        List<CharacteristicValueObject> hits = Arrays.asList(
                hit( "(2R,3R,4S)-2-amino-3,4-dihydroxy-6-(hydroxymethyl)oxan-3-yl carbamate", systematic ),
                hit( "tobramycin", plain ) );

        List<CharacteristicValueObject> ranked =
                strategy.rank( "tobramycin", hits, Collections.emptyMap(), Collections.emptyMap() );

        assertThat( urisOf( ranked ) ).containsExactly( plain, systematic );
    }

    @Test
    public void designation_penalty_does_not_fire_on_ordinary_names() {
        assertThat( CommonalityRankingStrategy.designationPenalty( "tobramycin" ) ).isEqualTo( 0.0 );
        assertThat( CommonalityRankingStrategy.designationPenalty( "dimethyl sulfoxide" ) ).isEqualTo( 0.0 );
        assertThat( CommonalityRankingStrategy.designationPenalty( "reference substance role" ) ).isEqualTo( 0.0 );
        // A trial-code designation is a name people really do write; it must not be demoted just
        // for containing digits.
        assertThat( CommonalityRankingStrategy.designationPenalty( "MK-2206" ) ).isEqualTo( 0.0 );
        assertThat( CommonalityRankingStrategy.designationPenalty( "C57BL/6J" ) ).isEqualTo( 0.0 );
        // Whereas locants and stereo descriptors are the real tell.
        assertThat( CommonalityRankingStrategy.designationPenalty(
                "(2S,3R)-2-amino-3-hydroxybutanoic acid" ) ).isGreaterThan( 0.0 );
    }

    @Test
    public void ordering_is_stable_when_nothing_separates_the_hits() {
        // With no prior and no shape signal the strategy must return the incoming Lucene order
        // untouched, rather than reshuffling on an accident of URI string order — the failure that
        // made `FTC` resolve to ferroptocide.
        List<CharacteristicValueObject> hits = Arrays.asList(
                hit( "alpha", "http://example.org/obo/X_2" ),
                hit( "beta", "http://example.org/obo/X_1" ),
                hit( "gamma", "http://example.org/obo/X_3" ) );

        List<CharacteristicValueObject> ranked =
                strategy.rank( "something", hits, Collections.emptyMap(), Collections.emptyMap() );

        assertThat( urisOf( ranked ) ).containsExactly(
                "http://example.org/obo/X_2", "http://example.org/obo/X_1", "http://example.org/obo/X_3" );
    }

    @Test
    public void three_arg_overload_still_works_for_callers_that_have_no_prior() {
        List<CharacteristicValueObject> hits = Arrays.asList(
                hit( "alpha", "http://example.org/obo/X_2" ),
                hit( "beta", "http://example.org/obo/X_1" ) );

        List<CharacteristicValueObject> ranked = strategy.rank( "alpha", hits, Collections.emptyMap() );

        assertThat( ranked ).hasSize( 2 );
    }

    @Test
    public void strategy_declares_it_needs_the_prior_and_not_the_usage_counts() {
        assertThat( strategy.requiresStringPrior() ).isTrue();
        assertThat( strategy.requiresUsageCounts() )
                .as( "must not make the caller pay for the usage scan it never reads" )
                .isFalse();
        assertThat( strategy.getName() ).isEqualTo( "commonality" );
    }
}

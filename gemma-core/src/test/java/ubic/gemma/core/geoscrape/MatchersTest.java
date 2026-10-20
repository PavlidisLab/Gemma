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

import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-Mockito-less tests for the v1 GEO scrape matchers. Synthesise
 * {@link GeoRecord} inputs and check matched / not for each matcher.
 *
 * @author phase 3 geo-scrape pipeline
 */
public class MatchersTest {

    /* ===== BrainKeywordMatcher ===== */

    @Test
    public void brainMatcher_hitsOnTitle() {
        BrainKeywordMatcher m = new BrainKeywordMatcher();
        GeoRecord r = new GeoRecord();
        r.setTitle( "Single-nucleus RNA-seq of human hippocampus" );

        GeoRecordMatcher.MatchResult res = m.evaluate( r );
        assertThat( res.isMatched() ).isTrue();
        assertThat( res.getReason() ).contains( "hippocampus" );
        assertThat( res.getConfidence() ).isEqualTo( 1.0 );
    }

    @Test
    public void brainMatcher_hitsOnMeshHeading() {
        BrainKeywordMatcher m = new BrainKeywordMatcher();
        GeoRecord r = new GeoRecord();
        r.setTitle( "Some unrelated title" );
        r.setMeshHeadings( Arrays.asList( "Astrocyte", "Mitochondrion" ) );

        assertThat( m.evaluate( r ).isMatched() ).isTrue();
    }

    @Test
    public void brainMatcher_missesOnUnrelatedRecord() {
        BrainKeywordMatcher m = new BrainKeywordMatcher();
        GeoRecord r = new GeoRecord();
        r.setTitle( "Comparative transcriptomics of pancreatic islet cells" );
        r.setSummary( "We profile islet beta cells under glucose challenge." );

        GeoRecordMatcher.MatchResult res = m.evaluate( r );
        assertThat( res.isMatched() ).isFalse();
        assertThat( res.getConfidence() ).isEqualTo( 0.0 );
    }

    @Test
    public void brainMatcher_missesOnNullRecord() {
        BrainKeywordMatcher m = new BrainKeywordMatcher();
        assertThat( m.evaluate( null ).isMatched() ).isFalse();
    }

    /* ===== SingleCellBrainMatcher ===== */

    @Test
    public void scbrainMatcher_hitsWhenSingleCellAndBrain() {
        SingleCellBrainMatcher m = new SingleCellBrainMatcher( new BrainKeywordMatcher() );
        GeoRecord r = new GeoRecord();
        r.setTitle( "Single-cell RNA-seq of mouse cortex" );
        r.setLibraryStrategy( "RNA-Seq" );

        assertThat( m.evaluate( r ).isMatched() ).isTrue();
    }

    @Test
    public void scbrainMatcher_hitsOn10xTechMention() {
        SingleCellBrainMatcher m = new SingleCellBrainMatcher( new BrainKeywordMatcher() );
        GeoRecord r = new GeoRecord();
        r.setTitle( "10x Genomics Chromium profiling of human prefrontal cortex" );

        assertThat( m.evaluate( r ).isMatched() ).isTrue();
    }

    @Test
    public void scbrainMatcher_missesWhenBrainButNotSingleCell() {
        SingleCellBrainMatcher m = new SingleCellBrainMatcher( new BrainKeywordMatcher() );
        GeoRecord r = new GeoRecord();
        r.setTitle( "Bulk RNA-seq of mouse hippocampus" );
        r.setLibraryStrategy( "RNA-Seq" );
        // No single-cell signal in title/summary/details

        assertThat( m.evaluate( r ).isMatched() ).isFalse();
    }

    @Test
    public void scbrainMatcher_missesWhenSingleCellButNotBrain() {
        SingleCellBrainMatcher m = new SingleCellBrainMatcher( new BrainKeywordMatcher() );
        GeoRecord r = new GeoRecord();
        r.setTitle( "scRNA-seq of human pancreatic islets" );
        r.setLibraryStrategy( "RNA-Seq" );

        assertThat( m.evaluate( r ).isMatched() ).isFalse();
    }

    /* ===== TfPerturbationMatcher ===== */

    @Test
    public void tfMatcher_hitsOnKnockdownPlusTfSymbol() {
        TfPerturbationMatcher m = new TfPerturbationMatcher();
        GeoRecord r = new GeoRecord();
        r.setTitle( "MYC knockdown in mouse embryonic stem cells" );
        r.setSummary( "We used shRNA to knockdown MYC." );

        GeoRecordMatcher.MatchResult res = m.evaluate( r );
        assertThat( res.isMatched() ).isTrue();
        assertThat( res.getReason() ).contains( "MYC" );
    }

    @Test
    public void tfMatcher_hitsOnCrisprKoOfTp53() {
        TfPerturbationMatcher m = new TfPerturbationMatcher();
        GeoRecord r = new GeoRecord();
        r.setTitle( "CRISPR knockout of TP53 in HCT116 cells" );

        assertThat( m.evaluate( r ).isMatched() ).isTrue();
    }

    @Test
    public void tfMatcher_missesWhenTfPresentButNoPerturbation() {
        TfPerturbationMatcher m = new TfPerturbationMatcher();
        GeoRecord r = new GeoRecord();
        r.setTitle( "Expression of MYC across human tissues" );
        r.setSummary( "We profile MYC expression in 30 tissues." );

        assertThat( m.evaluate( r ).isMatched() ).isFalse();
    }

    @Test
    public void tfMatcher_missesWhenPerturbationButNoTf() {
        TfPerturbationMatcher m = new TfPerturbationMatcher();
        GeoRecord r = new GeoRecord();
        r.setTitle( "Knockdown of an uncharacterised gene XYZ123 in HeLa" );

        assertThat( m.evaluate( r ).isMatched() ).isFalse();
    }

    @Test
    public void tfMatcher_doesNotMatchTfAsSubstring() {
        TfPerturbationMatcher m = new TfPerturbationMatcher();
        GeoRecord r = new GeoRecord();
        // "MYCO" / "REST" embedded inside other words must not match.
        r.setTitle( "Knockdown of MYCOLIC pathway in restful samples" );

        // 'restful' contains REST as substring but with word boundary required, must not match.
        // 'MYCOLIC' likewise contains MYC but with continuation 'OLIC', must not match.
        assertThat( m.evaluate( r ).isMatched() ).isFalse();
    }

    @Test
    public void tfMatcher_missesOnNullOrEmpty() {
        TfPerturbationMatcher m = new TfPerturbationMatcher();
        assertThat( m.evaluate( null ).isMatched() ).isFalse();
        GeoRecord r = new GeoRecord();
        assertThat( m.evaluate( r ).isMatched() ).isFalse();
    }

    /* ===== Matcher name() identifiers (REST API contract) ===== */

    @Test
    public void matchersHaveStableNames() {
        assertThat( new BrainKeywordMatcher().name() ).isEqualTo( "brain" );
        assertThat( new SingleCellBrainMatcher( new BrainKeywordMatcher() ).name() ).isEqualTo( "scbrain" );
        assertThat( new TfPerturbationMatcher().name() ).isEqualTo( "tfperturb" );
    }

    /* ===== BrainKeywordMatcher.getKeywords() — sanity ===== */

    @Test
    public void brainMatcher_keywordsIncludeCuratedSet() {
        BrainKeywordMatcher m = new BrainKeywordMatcher();
        assertThat( m.getKeywords() ).contains( "brain", "hippocampus", "astrocyte" );
    }
}

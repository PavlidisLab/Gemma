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
package ubic.gemma.core.loader.expression.geo;

import org.junit.jupiter.api.Test;

import ubic.gemma.core.loader.expression.geo.model.GeoLibraryStrategy;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The library-strategy vocabulary, and the one case where GEO's declared value is not believed.
 *
 * @author gembro
 */
public class GeoLibraryStrategyTest {

    /**
     * 🛑 Every constant must round-trip from GEO's own spelling. An unlisted value is FATAL in
     * {@code GeoFamilyParser} — deliberately, since folding it into {@code OTHER} would admit a chromatin or
     * genomic assay as expression — so a constant nothing can parse into is a constant that never fires.
     */
    @Test
    public void testEverySpellingResolves() {
        for ( GeoLibraryStrategy s : GeoLibraryStrategy.values() ) {
            assertThat( GeoLibraryStrategy.fromGeoString( s.getGeoString() ) )
                    .as( "%s spelled %s", s, s.getGeoString() )
                    .isEqualTo( s );
        }
    }

    @Test
    public void testSpellingIsCaseAndWhitespaceInsensitive() {
        assertThat( GeoLibraryStrategy.fromGeoString( "rna-seq" ) ).isEqualTo( GeoLibraryStrategy.RNA_SEQ );
        assertThat( GeoLibraryStrategy.fromGeoString( "  ChIP-Seq " ) ).isEqualTo( GeoLibraryStrategy.CHIP_SEQ );
        assertThat( GeoLibraryStrategy.fromGeoString( "RIBO-SEQ" ) ).isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
    }

    /** The spellings that were already in the corpus before the vocabulary was completed. */
    @Test
    public void testTheSpellingsAlreadySeenInProd() {
        assertThat( GeoLibraryStrategy.fromGeoString( "RNA-Seq" ) ).isEqualTo( GeoLibraryStrategy.RNA_SEQ );
        assertThat( GeoLibraryStrategy.fromGeoString( "ssRNA-seq" ) ).isEqualTo( GeoLibraryStrategy.SSRNA_SEQ );
        assertThat( GeoLibraryStrategy.fromGeoString( "miRNA-Seq" ) ).isEqualTo( GeoLibraryStrategy.MIRNA_SEQ );
        assertThat( GeoLibraryStrategy.fromGeoString( "ncRNA-Seq" ) ).isEqualTo( GeoLibraryStrategy.NCRNA_SEQ );
        assertThat( GeoLibraryStrategy.fromGeoString( "RIP-Seq" ) ).isEqualTo( GeoLibraryStrategy.RIP_SEQ );
        assertThat( GeoLibraryStrategy.fromGeoString( "OTHER" ) ).isEqualTo( GeoLibraryStrategy.OTHER );
    }

    @Test
    public void testAnUnknownSpellingIsNotGuessedAt() {
        assertThat( GeoLibraryStrategy.fromGeoString( "Ribo-Sequencing" ) ).isNull();
        assertThat( GeoLibraryStrategy.fromGeoString( "" ) ).isNull();
    }

    /**
     * 🛑 The GSE288755 case: titled {@code … Ribo-seq replicate #1}, declared {@code OTHER}.
     */
    @Test
    public void testRibosomeProfilingSubmittedAsOther() {
        assertThat( GeoConverterImpl.effectiveLibStrategy(
                other( "siControl HEK293T Ribo-seq replicate #1", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );

        assertThat( GeoConverterImpl.effectiveLibStrategy(
                other( "rep1", "Ribosome profiling of primary hepatocytes" ) ) )
                .as( "the description counts too, not only the title" )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
    }

    /** A DECLARED strategy is believed, whatever the sample happens to be called. */
    @Test
    public void testADeclaredStrategyIsNotSecondGuessed() {
        GeoSample s = other( "Ribo-seq input control", "" );
        s.setLibStrategy( GeoLibraryStrategy.RNA_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( s ) ).isEqualTo( GeoLibraryStrategy.RNA_SEQ );
    }

    /**
     * Ribosome-ASSOCIATED profiling counts, not only footprinting — Paul's ruling, 2026-09-16.
     * <p>
     * These titles are the ones measured in the corpus: eid 11789 {@code … FST TRAP}, 15632/15633
     * {@code …-EGFP-Rpl10a-… IP}, 33841 {@code AA Ins poly rep1}. 40 TRAP/IP and 32 polysome samples
     * already carried RIBO_SEQ from GEO's own declaration before this regex could match any of them.
     * An earlier version of this test asserted the OPPOSITE for polysome — see the class the ruling
     * reversed, and do not restore it without a newer ruling.
     */
    @Test
    public void testRibosomeAssociatedAssaysAreAlsoRiboSeq() {
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "Hipp FST TRAP rep1", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "Pcp2-EGFP-Rpl10a-IP-2", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "AA Ins poly rep1", "polysome profiling" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "polysome fraction 3", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "RiboTag, CA1, Control, IP, replicate 1", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "Pnoc_LepR_KO_Pulldown_5", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy(
                other( "eIF2D KO HEK293T #1 eIF2D-V5 OE TCP-seq replicate 1", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "disome profiling rep2", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "polysomal RNA fraction 2", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.RIBO_SEQ );
    }

    /**
     * 🛑 The karyotype words are NOT ribosome fractions. {@code polysomy}, {@code monosomy} and
     * {@code uniparental disomy} are chromosome counts and share a stem with {@code polysome} /
     * {@code monosome} / {@code disome}; the {@code (?!y)} lookahead is the only thing separating them.
     * Drop it and every trisomy-adjacent karyotype series declared OTHER becomes ribosome profiling.
     */
    @Test
    public void testKaryotypeWordsAreNotRibosomeFractions() {
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "monosomy 7 AML blasts", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.OTHER );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "polysomy of chromosome 17", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.OTHER );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "uniparental disomy 14 patient", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.OTHER );
    }

    /**
     * 🛑 The two shapes the regex CANNOT reach, whatever it says, because the gate is predicated on GEO's
     * declared strategy. Pinned so that a later regex change is not mistaken for covering them.
     * <p>
     * eid 57963 carries both in ONE curator-defined arm: 6 samples declared OTHER (the regex now catches
     * those) and 2 declared RNA_SEQ. eid 50185 declares nothing at all.
     */
    @Test
    public void testTheGateStillCannotReachADeclaredOrAbsentStrategy() {
        GeoSample declared = other( "RiboTag IP replicate 1", "" );
        declared.setLibStrategy( GeoLibraryStrategy.RNA_SEQ );
        assertThat( GeoConverterImpl.effectiveLibStrategy( declared ) ).isEqualTo( GeoLibraryStrategy.RNA_SEQ );

        GeoSample none = other( "RiboTag, CA1, Control, IP, replicate 1", "" );
        none.setLibStrategy( null );
        assertThat( GeoConverterImpl.effectiveLibStrategy( none ) ).isNull();
    }

    /** A non-transcriptomic source is never reinterpreted, however it is named. */
    @Test
    public void testANonTranscriptomicSampleIsNotPromoted() {
        GeoSample s = other( "Ribo-seq of something", "" );
        s.setLibSource( GeoLibrarySource.GENOMIC );
        assertThat( GeoConverterImpl.effectiveLibStrategy( s ) ).isEqualTo( GeoLibraryStrategy.OTHER );
    }

    /**
     * A microarray sample records how many channels it was hybridized in (Paul, 2026-09-13). The literal strings
     * are asserted rather than the constants, because rows already in production have to match them.
     */
    @Test
    public void testAMicroarraySampleRecordsItsChannelCount() {
        GeoSample s = new GeoSample();
        s.setType( GeoSampleType.RNA );
        assertThat( GeoConverterImpl.libraryStrategy( s ) ).isEqualTo( "MICROARRAY_ONE_COLOR" );
        s.addChannel();
        assertThat( GeoConverterImpl.libraryStrategy( s ) ).isEqualTo( "MICROARRAY_TWO_COLOR" );
    }

    /** A sequencing sample records the constant name, not GEO's spelling (Paul, 2026-09-13). */
    @Test
    public void testASequencingSampleRecordsTheConstantName() {
        GeoSample s = new GeoSample();
        s.setType( GeoSampleType.SRA );
        s.setLibStrategy( GeoLibraryStrategy.SCRNA_SEQ );
        assertThat( GeoConverterImpl.libraryStrategy( s ) ).isEqualTo( "SCRNA_SEQ" );
    }

    /** Neither a declared strategy nor a microarray: nothing is recorded. */
    @Test
    public void testASampleThatIsNeitherRecordsNothing() {
        GeoSample s = new GeoSample();
        s.setType( GeoSampleType.GENOMIC );
        assertThat( GeoConverterImpl.libraryStrategy( s ) ).isNull();
    }

    private static GeoSample other( String title, String description ) {
        GeoSample s = new GeoSample();
        s.setTitle( title );
        s.setDescription( description );
        s.setLibStrategy( GeoLibraryStrategy.OTHER );
        s.setLibSource( GeoLibrarySource.TRANSCRIPTOMIC );
        return s;
    }
}

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
     * ...and the detector stays narrow. These ride alongside ribosome profiling in the same series and are
     * their own methods; calling them RIBO_SEQ would trade one wrong label for another.
     */
    @Test
    public void testRelatedButDifferentAssaysAreLeftAsOther() {
        assertThat( GeoConverterImpl.effectiveLibStrategy(
                other( "eIF2D KO HEK293T #1 eIF2D-V5 OE TCP-seq replicate 1", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.OTHER );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "polysome fraction 3", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.OTHER );
        assertThat( GeoConverterImpl.effectiveLibStrategy( other( "disome profiling rep2", "" ) ) )
                .isEqualTo( GeoLibraryStrategy.OTHER );
    }

    /** A non-transcriptomic source is never reinterpreted, however it is named. */
    @Test
    public void testANonTranscriptomicSampleIsNotPromoted() {
        GeoSample s = other( "Ribo-seq of something", "" );
        s.setLibSource( GeoLibrarySource.GENOMIC );
        assertThat( GeoConverterImpl.effectiveLibStrategy( s ) ).isEqualTo( GeoLibraryStrategy.OTHER );
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

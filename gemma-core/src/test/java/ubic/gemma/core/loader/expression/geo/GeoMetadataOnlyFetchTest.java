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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The source-metadata backfill reads GEO's series and sample records directly rather than the
 * family SOFT file. Everything it needs has to survive that: the family file is what every other
 * caller parses, and a record set that parses into a series with no samples — or samples with no
 * submitter characteristics — would still build a document, just an empty one, and the backfill
 * would report success on 23,000 experiments.
 * <p>
 * Tagged {@code network} because it reads from GEO. Run it with
 * {@code mvn -pl gemma-core test -Dtest=GeoMetadataOnlyFetchTest -Dgroups=network -DexcludedGroups=slow}.
 *
 * @author gembro
 */
@Tag("network")
class GeoMetadataOnlyFetchTest {

    /**
     * GSE102415: 30 samples, each carrying {@code !Sample_characteristics_ch1 = tissue: Hypothalamus}.
     * The characteristics are the point — they are what the curation agent reads, and they live on
     * the sample records that {@code targ=gsm} returns.
     */
    @Test
    void testTheSampleRecordsCarryWhatTheDocumentNeeds( @TempDir Path cacheDir ) throws Exception {
        GeoSeries series = generator( cacheDir ).generateSeriesMetadataOnly( "GSE102415" );

        assertThat( series.getGeoAccession() ).isEqualTo( "GSE102415" );
        assertThat( series.getTitle() ).isNotBlank();
        assertThat( series.getSamples() ).hasSize( 30 );

        ObjectMapper objectMapper = new ObjectMapper();
        String json = new GeoSourceMetadataBuilder( objectMapper ).build( series,
                new GeoSourceMetadataBuilder.ExperimentIdentity( "GSE102415", 1L, false, null ),
                new Date() );
        assertThat( json ).isNotNull();

        JsonNode doc = objectMapper.readTree( json );
        assertThat( doc.get( "sampleCount" ).asInt() ).isEqualTo( 30 );
        JsonNode sample = doc.get( "samples" ).get( 0 );
        assertThat( sample.get( "accession" ).asText() ).startsWith( "GSM" );
        assertThat( sample.get( "title" ).asText() ).isNotBlank();
        assertThat( sample.get( "characteristics" ).get( "tissue" ).asText() ).isEqualTo( "Hypothalamus" );
    }

    /**
     * A two-colour series from 2004: two channels per sample, no characteristics at all. It is here
     * because the per-channel fields are prefixed only from the second channel onward, and because
     * "no characteristics" must parse rather than fail.
     */
    @Test
    void testATwoChannelSeriesWithoutCharacteristicsStillParses( @TempDir Path cacheDir ) {
        GeoSeries series = generator( cacheDir ).generateSeriesMetadataOnly( "GSE1024" );

        assertThat( series.getGeoAccession() ).isEqualTo( "GSE1024" );
        assertThat( series.getSamples() ).hasSize( 36 );
        assertThat( series.getSamples() )
                .allSatisfy( s -> assertThat( s.getTitle() ).isNotBlank() );
    }

    /**
     * Cached into a temporary directory rather than the configured one: this test must not deposit
     * records into the shared GEO download tree, which holds the real family files.
     */
    private GeoDomainObjectGenerator generator( Path cacheDir ) {
        GeoDomainObjectGenerator generator = new GeoDomainObjectGenerator();
        generator.setMetadataCacheDir( cacheDir.toFile() );
        return generator;
    }

    /**
     * 🛑 The lean fetch must produce the SAME document as the family file it replaces.
     * <p>
     * That is the whole risk of the change: the family SOFT carries the platform table and every
     * sample's data table, and the two records read instead carry neither. If some field the
     * document needs only appears in what was dropped, the backfill still succeeds — on 23,000
     * experiments — and stores a document quietly missing it.
     * <p>
     * The fixture is GSE102415's real family file, 4.6 KB because it is RNA-seq on a generic
     * platform, so the whole comparison fits in a classpath resource. That is also its limit: a
     * two-colour array's family file is tens of megabytes and cannot be a fixture, so equality is
     * proven here for a series whose family file carries no data tables. The channel fields such a
     * series does not exercise are read from the sample records either way, and
     * {@link #testATwoChannelSeriesWithoutCharacteristicsStillParses()} covers those.
     */
    @Test
    void testTheLeanFetchBuildsTheSameDocumentAsTheFamilyFile( @TempDir Path cacheDir ) throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        try ( InputStream is = new GZIPInputStream( Objects.requireNonNull( getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE102415_family.soft.gz" ) ) ) ) {
            parser.parse( is );
        }
        GeoSeries fromFamily = parser.getResults().iterator().next().getSeriesMap().get( "GSE102415" );
        GeoSeries fromRecords = generator( cacheDir ).generateSeriesMetadataOnly( "GSE102415" );

        assertThat( document( fromRecords ) )
                .as( "reading the series and sample records must lose nothing the document carries" )
                .isEqualTo( document( fromFamily ) );
    }

    /** Serialized with a fixed harvest date, since that is the one field that must differ. */
    private String document( GeoSeries series ) {
        return new GeoSourceMetadataBuilder( new ObjectMapper() ).build( series,
                new GeoSourceMetadataBuilder.ExperimentIdentity( "GSE102415", 1L, false, null ),
                new Date( 1_770_000_000_000L ) );
    }

    /**
     * 🛑 A series GEO has retired lists no samples, and `targ=gsm` then answers with nothing at all.
     * GSE1829 is titled "RETIRED", declares zero `!Series_sample_id`, and returns 0 bytes —
     * reproducibly, twice, not as a hiccup. Failing the experiment over that reports GEO's own state
     * as our error and throws away the series record we did get. It cost eid 861 its document in the
     * corpus sweep on 2026-08-29.
     */
    @Test
    void testARetiredSeriesWithNoSamplesStillYieldsItsSeriesRecord( @TempDir Path cacheDir ) {
        GeoSeries series = generator( cacheDir ).generateSeriesMetadataOnly( "GSE1829" );

        assertThat( series.getGeoAccession() ).isEqualTo( "GSE1829" );
        assertThat( series.getTitle() ).isEqualTo( "RETIRED" );
        assertThat( series.getSamples() ).isEmpty();
    }
}

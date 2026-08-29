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

import java.nio.file.Path;
import java.util.Date;

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
}

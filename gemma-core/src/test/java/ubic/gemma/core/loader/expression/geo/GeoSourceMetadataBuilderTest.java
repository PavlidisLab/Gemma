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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoChannel;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the schema-v1 rules agreed with CAB. Several of these look pedantic and are not: an empty
 * array where an object was promised, or a key that snake_cases to something nothing reads, both
 * arrive on the consuming side as a silently empty field rather than an error.
 */
class GeoSourceMetadataBuilderTest {

    private ObjectMapper objectMapper;
    private GeoSourceMetadataBuilder builder;
    private Date harvestedAt;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new GeoSourceMetadataBuilder( objectMapper );
        harvestedAt = new Date( 1_770_000_000_000L );
    }

    private GeoSample sample( String accession, String title, String organism, String... characteristics ) {
        GeoSample s = new GeoSample();
        s.setGeoAccession( accession );
        s.setTitle( title );
        // GeoSample's constructor already creates channel 1.
        GeoChannel c = s.getChannel( 1 );
        c.setOrganism( organism );
        c.setSourceName( "source of " + accession );
        for ( String ch : characteristics ) {
            c.addCharacteristic( ch );
        }
        return s;
    }

    private GeoSeries series( GeoSample... samples ) {
        GeoSeries series = new GeoSeries();
        series.setGeoAccession( "GSE11056" );
        series.setTitle( "A study" );
        series.setOverallDesign( "2x2 treatment by genotype" );
        for ( GeoSample s : samples ) {
            series.addSample( s );
        }
        return series;
    }

    private JsonNode build( GeoSeries series, GeoSourceMetadataBuilder.ExperimentIdentity identity ) throws Exception {
        String json = builder.build( series, identity, harvestedAt );
        assertThat( json ).isNotNull();
        return objectMapper.readTree( json );
    }

    private GeoSourceMetadataBuilder.ExperimentIdentity whole( String shortName ) {
        return new GeoSourceMetadataBuilder.ExperimentIdentity( shortName, 4412L, false, null );
    }

    @Test
    void emitsSchemaVersionAndOurOwnHarvestTimestamp() throws Exception {
        JsonNode doc = build( series( sample( "GSM1", "s1", "Homo sapiens" ) ), whole( "GSE11056" ) );
        assertThat( doc.get( "schemaVersion" ).asInt() ).isEqualTo( GeoSourceMetadataBuilder.SCHEMA_VERSION );
        assertThat( doc.get( "source" ).asText() ).isEqualTo( "GEO" );
        // harvestedAt is Gemma's clock, not GEO's; GEO's own dates ride separately.
        assertThat( doc.get( "harvestedAt" ).asText() ).startsWith( "2026-" );
        assertThat( doc.get( "experimentId" ).asLong() ).isEqualTo( 4412L );
    }

    /**
     * The reason the whole payload exists: the agent must see each sample's own characteristics to
     * tell a copy-pasted constant column from a real factor. Gemma's converter flattens this away.
     */
    @Test
    void keepsPerSampleCharacteristicsSeparate() throws Exception {
        JsonNode doc = build( series(
                sample( "GSM1", "ctrl rep1", "Homo sapiens", "treatment: none", "genotype: WT" ),
                sample( "GSM2", "drug rep1", "Homo sapiens", "treatment: drug", "genotype: KO" ) ),
                whole( "GSE11056" ) );

        JsonNode samples = doc.get( "samples" );
        assertThat( samples ).hasSize( 2 );
        assertThat( samples.get( 0 ).get( "characteristics" ).get( "treatment" ).asText() ).isEqualTo( "none" );
        assertThat( samples.get( 1 ).get( "characteristics" ).get( "treatment" ).asText() ).isEqualTo( "drug" );
        assertThat( samples.get( 0 ).get( "title" ).asText() ).isEqualTo( "ctrl rep1" );
    }

    @Test
    void characteristicsIsAlwaysAnObjectEvenWhenEmpty() throws Exception {
        JsonNode doc = build( series( sample( "GSM1", "s1", "Homo sapiens" ) ), whole( "GSE11056" ) );
        JsonNode characteristics = doc.get( "samples" ).get( 0 ).get( "characteristics" );
        assertThat( characteristics.isObject() )
                .as( "an empty array and an empty object deserialize differently on the consuming side" )
                .isTrue();
        assertThat( characteristics ).isEmpty();
    }

    @Test
    void keepsCharacteristicsThatCannotBeSplitRatherThanInventingAKey() throws Exception {
        JsonNode doc = build( series( sample( "GSM1", "s1", "Homo sapiens", "tissue: cortex", "no colon here" ) ),
                whole( "GSE11056" ) );
        JsonNode sample = doc.get( "samples" ).get( 0 );
        assertThat( sample.get( "characteristics" ).get( "tissue" ).asText() ).isEqualTo( "cortex" );
        assertThat( sample.get( "characteristicsUnparsed" ).get( 0 ).asText() ).isEqualTo( "no colon here" );
    }

    @Test
    void absentMeansAbsentNeverNull() throws Exception {
        JsonNode doc = build( series( sample( "GSM1", "s1", "Homo sapiens" ) ), whole( "GSE11056" ) );
        assertThat( doc.has( "summary" ) ).isFalse();
        assertThat( doc.get( "samples" ).get( 0 ).has( "dataProcessing" ) ).isFalse();
    }

    /**
     * SplitExperimentServiceImpl puts the .1 in shortName only — cloneAccession copies the GEO
     * accession verbatim, so siblings share accession, title, summary and overallDesign. If samples[]
     * were the whole series, each sibling would claim the other's samples and nothing in the document
     * would reveal it.
     */
    @Test
    void aSplitSubseriesCarriesOnlyItsOwnSamplesButTheSeriesCount() throws Exception {
        GeoSeries full = series(
                sample( "GSM1", "s1", "Homo sapiens" ),
                sample( "GSM2", "s2", "Homo sapiens" ),
                sample( "GSM3", "s3", "Homo sapiens" ) );
        Set<String> mine = new LinkedHashSet<>( java.util.Arrays.asList( "GSM1", "GSM3" ) );

        JsonNode doc = build( full, new GeoSourceMetadataBuilder.ExperimentIdentity( "GSE11056.1", 4412L, true, mine ) );

        assertThat( doc.get( "sampleCount" ).asInt() )
                .as( "sampleCount is GEO's series count, so it exceeds samples.length for a split" )
                .isEqualTo( 3 );
        assertThat( doc.get( "samples" ) ).hasSize( 2 );
        assertThat( doc.get( "samples" ).get( 0 ).get( "accession" ).asText() ).isEqualTo( "GSM1" );
        assertThat( doc.get( "samples" ).get( 1 ).get( "accession" ).asText() ).isEqualTo( "GSM3" );
        assertThat( doc.get( "isSplitSubseries" ).asBoolean() ).isTrue();
        assertThat( doc.get( "shortName" ).asText() )
                .as( "shortName is the ONLY thing distinguishing split siblings" )
                .isEqualTo( "GSE11056.1" );
    }

    @Test
    void anUnsplitExperimentGetsEverySampleAndAMatchingCount() throws Exception {
        JsonNode doc = build( series(
                sample( "GSM1", "s1", "Homo sapiens" ),
                sample( "GSM2", "s2", "Homo sapiens" ) ), whole( "GSE11056" ) );
        assertThat( doc.get( "sampleCount" ).asInt() ).isEqualTo( 2 );
        assertThat( doc.get( "samples" ) ).hasSize( 2 );
        assertThat( doc.has( "isSplitSubseries" ) ).isFalse();
    }

    @Test
    void secondChannelFieldsArePrefixedSoTheyDoNotOverwriteTheFirst() throws Exception {
        GeoSample s = sample( "GSM1", "s1", "Homo sapiens", "tissue: cortex" );
        s.addChannel();
        GeoChannel second = s.getChannel( 2 );
        second.setOrganism( "Mus musculus" );
        second.setSourceName( "second source" );
        second.addCharacteristic( "tissue: liver" );

        JsonNode sample = build( series( s ), whole( "GSE11056" ) ).get( "samples" ).get( 0 );
        assertThat( sample.get( "organism" ).asText() ).isEqualTo( "Homo sapiens" );
        assertThat( sample.get( "ch2_organism" ).asText() ).isEqualTo( "Mus musculus" );
        assertThat( sample.get( "characteristics" ).get( "tissue" ).asText() ).isEqualTo( "cortex" );
        assertThat( sample.get( "characteristics" ).get( "ch2_tissue" ).asText() ).isEqualTo( "liver" );
    }

    @Test
    void organismsAreCollectedAcrossSamplesAndDeduplicated() throws Exception {
        JsonNode doc = build( series(
                sample( "GSM1", "s1", "Homo sapiens" ),
                sample( "GSM2", "s2", "Mus musculus" ),
                sample( "GSM3", "s3", "Homo sapiens" ) ), whole( "GSE11056" ) );
        assertThat( doc.get( "organisms" ) ).hasSize( 2 );
    }

    /** The hash must be reproducible by blanking the field and re-hashing, or it cannot be checked. */
    @Test
    void sha256IsPresentAndReproducible() throws Exception {
        String json = builder.build( series( sample( "GSM1", "s1", "Homo sapiens" ) ), whole( "GSE11056" ), harvestedAt );
        JsonNode doc = objectMapper.readTree( json );
        String sha = doc.get( "sha256" ).asText();
        assertThat( sha ).hasSize( 64 );
        assertThat( doc.get( "truncated" ) ).isEmpty();
    }

    @Test
    void returnsNullForNoSeriesRatherThanAnEmptyShell() {
        assertThat( builder.build( null, whole( "GSE11056" ), harvestedAt ) ).isNull();
    }

    @Test
    void anExperimentClaimingSamplesTheSeriesLacksStillProducesATruthfulDocument() throws Exception {
        JsonNode doc = build( series( sample( "GSM1", "s1", "Homo sapiens" ) ),
                new GeoSourceMetadataBuilder.ExperimentIdentity( "GSE11056.1", 1L, true,
                        Collections.singleton( "GSM_MISSING" ) ) );
        assertThat( doc.get( "samples" ) ).isEmpty();
        assertThat( doc.get( "sampleCount" ).asInt() ).isEqualTo( 1 );
    }
}

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
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata-only records are cached in the same directory as the full family SOFT files — on
 * production that directory holds 57,212 accessions' worth of them, fetched over years — so the
 * rule that matters is that the two never occupy the same name. A metadata record carries no
 * platform table and no sample data; anything that read one believing it was the family file would
 * see an experiment with no probes and no data, and the file it replaced is not cheap to get back.
 *
 * @author gembro
 */
class GeoMetadataCacheTest {

    /**
     * The cache is read when it is there. The accession is one GEO does not serve, so a run that
     * ignored the cache would fail rather than quietly fetch something else.
     */
    @Test
    void testACachedRecordIsReadInsteadOfFetched( @TempDir Path cacheDir ) throws Exception {
        String acc = "GSE9999999";
        Path dir = Files.createDirectories( cacheDir.resolve( acc ) );
        Files.write( dir.resolve( acc + ".self.brief.soft" ), ( "^SERIES = " + acc + "\n"
                + "!Series_title = A series that only exists in this test\n"
                + "!Series_geo_accession = " + acc + "\n"
                + "!Series_sample_id = GSM9999998\n" ).getBytes( StandardCharsets.UTF_8 ) );
        Files.write( dir.resolve( acc + ".gsm.brief.soft" ), ( "^SAMPLE = GSM9999998\n"
                + "!Sample_title = The only sample\n"
                + "!Sample_geo_accession = GSM9999998\n"
                + "!Sample_channel_count = 1\n"
                + "!Sample_source_name_ch1 = hypothalamus\n"
                + "!Sample_characteristics_ch1 = tissue: Hypothalamus\n"
                + "!Sample_organism_ch1 = Mus musculus\n" ).getBytes( StandardCharsets.UTF_8 ) );

        GeoDomainObjectGenerator generator = new GeoDomainObjectGenerator();
        generator.setMetadataCacheDir( cacheDir.toFile() );
        GeoSeries series = generator.generateSeriesMetadataOnly( acc );

        assertThat( series.getTitle() ).isEqualTo( "A series that only exists in this test" );
        assertThat( series.getSamples() ).hasSize( 1 );
    }

    /**
     * 🛑 The names the full downloads use: {@code <ACC>.soft.gz} is what the fetcher writes and
     * {@code <ACC>_family.soft.gz} is what {@code LocalSeriesFetcher} looks for. Neither may ever be
     * the name a metadata record is written to.
     */
    @Test
    void testTheCachedRecordCannotTakeTheFamilyFileName( @TempDir Path cacheDir ) throws Exception {
        String acc = "GSE9999999";
        Path dir = Files.createDirectories( cacheDir.resolve( acc ) );
        Files.write( dir.resolve( acc + ".self.brief.soft" ), ( "^SERIES = " + acc + "\n"
                + "!Series_title = t\n!Series_geo_accession = " + acc + "\n" ).getBytes( StandardCharsets.UTF_8 ) );
        Files.write( dir.resolve( acc + ".gsm.brief.soft" ), "\n".getBytes( StandardCharsets.UTF_8 ) );
        File family = dir.resolve( acc + ".soft.gz" ).toFile();
        File localFamily = dir.resolve( acc + "_family.soft.gz" ).toFile();
        assertThat( family ).doesNotExist();

        GeoDomainObjectGenerator generator = new GeoDomainObjectGenerator();
        generator.setMetadataCacheDir( cacheDir.toFile() );
        generator.generateSeriesMetadataOnly( acc );

        assertThat( family ).as( "the full download's name" ).doesNotExist();
        assertThat( localFamily ).as( "the name LocalSeriesFetcher seeks" ).doesNotExist();
        assertThat( dir.toFile().listFiles() )
                .allSatisfy( f -> assertThat( f.getName() ).endsWith( ".brief.soft" ) );
    }
}

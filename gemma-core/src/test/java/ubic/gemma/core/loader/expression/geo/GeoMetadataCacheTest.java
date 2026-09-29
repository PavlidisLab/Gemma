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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    /**
     * 🛑 GEO answers a withdrawn, private or unknown accession with an HTML page and a 200, not an
     * error. The SOFT parser reads it happily, finds no series, and the run reports "No series was
     * parsed for GSE6959" — which reads as a parser bug rather than as GEO declining to serve the
     * record. Two of the first 2,160 experiments in the corpus sweep failed this way.
     */
    @Test
    void testAnHtmlErrorPageIsNotParsedAsSoft( @TempDir Path cacheDir ) throws Exception {
        String acc = "GSE9999999";
        writeCachedPage( cacheDir, acc, accessionViewerPage( acc,
                "Accession \"" + acc + "\" was deleted by the GEO staff on Jun 18, 2007." ) );

        GeoDomainObjectGenerator generator = new GeoDomainObjectGenerator();
        generator.setMetadataCacheDir( cacheDir.toFile() );

        assertThatThrownBy( () -> generator.generateSeriesMetadataOnly( acc ) )
                .hasMessageContaining( "HTML page" )
                .hasMessageContaining( "withdrawn, private or unknown" );
    }

    /**
     * 🛑 The verdict has to come from GEO, not from the mere fact that HTML arrived. acc.cgi
     * intermittently answers a perfectly good accession with an NCBI error page — GSE42727,
     * 133 KB of it, 2026-09-17, with the SOFT record on the very next attempt — and the old wording
     * called every one of those a withdrawn, private or unknown accession. Seventeen experiments in
     * the 2026-09-15 backfill summary carry that sentence with nothing having asked twice.
     */
    @Test
    void testAnErrorPageIsNotCalledAWithdrawnAccession( @TempDir Path cacheDir ) throws Exception {
        String acc = "GSE9999999";
        writeCachedPage( cacheDir, acc, "<!DOCTYPE html>\n<html><head><title>Error</title></head>\n"
                + "<body>The NCBI web site is temporarily unavailable.</body></html>\n" );

        GeoDomainObjectGenerator generator = new GeoDomainObjectGenerator();
        generator.setMetadataCacheDir( cacheDir.toFile() );

        assertThatThrownBy( () -> generator.generateSeriesMetadataOnly( acc ) )
                .hasMessageContaining( "HTML page" )
                .hasMessageContaining( "error page" )
                .hasMessageNotContaining( "withdrawn, private or unknown" );
    }

    /**
     * Which of withdrawn, private and unknown it was is in GEO's own sentence, so a summary row says
     * so without anyone re-fetching the page to find out. The three wordings were measured against
     * acc.cgi on 2026-09-17.
     */
    @Test
    void testTheVerdictIsQuotedBackFromThePage( @TempDir Path cacheDir ) throws Exception {
        String acc = "GSE9999999";
        writeCachedPage( cacheDir, acc, accessionViewerPage( acc,
                "Accession &quot;" + acc + "&quot; is currently private and is scheduled to be released on Dec 31, 2027." ) );

        GeoDomainObjectGenerator generator = new GeoDomainObjectGenerator();
        generator.setMetadataCacheDir( cacheDir.toFile() );

        assertThatThrownBy( () -> generator.generateSeriesMetadataOnly( acc ) )
                .hasMessageContaining( "is currently private and is scheduled to be released on Dec 31, 2027." );
    }

    /**
     * The shape acc.cgi serves: the verdict sits in a {@code <font color="red">} of its own, on one
     * very long line of markup, which is why the enclosing tags and not the line bound it.
     */
    private static String accessionViewerPage( String acc, String verdict ) {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n<HTML>\n"
                + "<HEAD><TITLE>GEO Accession viewer</TITLE></HEAD>\n<BODY>"
                + "<tr><td align=\"left\" bgcolor=\"white\"><table><tr><td colspan=\"2\">"
                + "<font color=\"red\">" + verdict + "</font><br><br>"
                + "GEO can be contacted at geo@ncbi.nlm.nih.gov if additional assistance is required."
                + "</td></tr></table></td></tr></BODY></HTML>\n";
    }

    private static void writeCachedPage( Path cacheDir, String acc, String page ) throws Exception {
        Path dir = Files.createDirectories( cacheDir.resolve( acc ) );
        Files.write( dir.resolve( acc + ".self.brief.soft" ), page.getBytes( StandardCharsets.UTF_8 ) );
        Files.write( dir.resolve( acc + ".gsm.brief.soft" ), page.getBytes( StandardCharsets.UTF_8 ) );
    }
}

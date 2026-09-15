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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysis;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * One-shot capture utility that records every
 * {@link GoldenPathSequenceAnalysis#findAssociations} response for the
 * {@code gpl96.blatresults.psl.gz} fixture against a live hg19 GoldenPath
 * database, and writes a JSON fixture used by the fast mock variant of
 * {@link CompositeSequenceGeneMapperServiceTest}.
 * <p>
 * Tagged {@code goldenPath} + {@code slow} so it is excluded from default
 * {@code mvn verify}. Run only when refreshing the fixture, e.g.:
 * <pre>
 *   mvn -pl gemma-core test \
 *       -Dtest='GoldenPathFixtureRecorder' \
 *       -DexcludedGroups= \
 *       -Dgemma.goldenpath.db.host=localhost \
 *       -Dgemma.goldenpath.db.user=root \
 *       -Dgemma.goldenpath.db.password=... \
 *       -Dgemma.goldenpath.db.human=hg19
 * </pre>
 * <p>
 * Coordinate-system note: {@code gpl96.blatresults.psl.gz} was generated against
 * hg19 (chromosome sizes match hg19). The fixture is only meaningful when
 * captured from hg19. Running against hg38 would produce empty results for
 * HSPA6 since the gene shifted assemblies. The {@code gemma.goldenpath.db.human}
 * property must therefore be {@code hg19} (overriding the {@code hg38} default
 * in {@code default.properties}).
 */
@Tag("goldenPath")
@Tag("slow")
public class GoldenPathFixtureRecorder {

    private static final String PSL_RESOURCE = "/data/loader/genome/gpl96.blatresults.psl.gz";

    /**
     * Output location relative to the project root when run from
     * {@code gemma-core}. Lands next to other classpath test fixtures so the
     * fast variant can load it via {@link ClassPathResource}.
     */
    private static final String FIXTURE_OUTPUT_PATH =
            "src/test/resources/data/loader/genome/goldenpath/gpl96-hg19-fixture.json";

    @Test
    public void recordFixture() throws IOException {
        Taxon human = Taxon.Factory.newInstance();
        human.setCommonName( "human" );
        human.setScientificName( "Homo sapiens" );
        human.setIsGenesUsable( true );

        List<PslEntry> pslEntries = parsePsl();
        System.out.println( "[recorder] parsed " + pslEntries.size() + " PSL entries from " + PSL_RESOURCE );

        ProbeMapperConfig config = new ProbeMapperConfig();
        Map<String, Object> fixture = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put( "source", "hg19 GoldenPath via Settings (gemma.goldenpath.db.human=hg19)" );
        meta.put( "captured_at", LocalDate.now().toString() );
        meta.put( "psl_resource", PSL_RESOURCE );
        meta.put( "taxon_common_name", "human" );
        meta.put( "three_prime_distance_method", "RIGHT" );
        fixture.put( "_meta", meta );

        List<Map<String, Object>> calls = new ArrayList<>();
        int totalAssociations = 0;
        int hits = 0;
        try ( GoldenPathSequenceAnalysis gp = new GoldenPathSequenceAnalysis( human ) ) {
            for ( PslEntry e : pslEntries ) {
                Collection<BlatAssociation> result = gp.findAssociations(
                        e.chrom, e.queryStart, e.queryEnd, e.starts, e.sizes,
                        e.strand, ThreePrimeDistanceMethod.RIGHT, config );

                Map<String, Object> call = new LinkedHashMap<>();
                call.put( "chrom", e.chrom );
                call.put( "queryStart", e.queryStart );
                call.put( "queryEnd", e.queryEnd );
                call.put( "starts", e.starts );
                call.put( "sizes", e.sizes );
                call.put( "strand", e.strand );

                List<Map<String, Object>> associations = new ArrayList<>();
                if ( result != null ) {
                    for ( BlatAssociation ba : result ) {
                        associations.add( serialise( ba ) );
                    }
                }
                call.put( "associations", associations );
                calls.add( call );

                if ( !associations.isEmpty() ) {
                    hits++;
                    totalAssociations += associations.size();
                }
            }
        }
        fixture.put( "calls", calls );

        ObjectMapper mapper = new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT );
        File out = new File( FIXTURE_OUTPUT_PATH );
        File parent = out.getParentFile();
        if ( parent != null && !parent.exists() && !parent.mkdirs() ) {
            throw new IOException( "Could not create directory " + parent );
        }
        mapper.writeValue( out, fixture );

        System.out.println( "[recorder] wrote " + out.getAbsolutePath()
                + " with " + calls.size() + " calls, " + hits + " non-empty, "
                + totalAssociations + " total associations." );
    }

    private static Map<String, Object> serialise( BlatAssociation ba ) {
        Map<String, Object> m = new LinkedHashMap<>();
        if ( ba.getOverlap() != null ) m.put( "overlap", ba.getOverlap() );
        if ( ba.getThreePrimeDistance() != null ) m.put( "threePrimeDistance", ba.getThreePrimeDistance() );
        if ( ba.getThreePrimeDistanceMeasurementMethod() != null )
            m.put( "threePrimeDistanceMethod", ba.getThreePrimeDistanceMeasurementMethod().name() );

        GeneProduct gp = ba.getGeneProduct();
        if ( gp != null ) {
            Map<String, Object> gpm = new LinkedHashMap<>();
            gpm.put( "name", gp.getName() );
            gpm.put( "ncbiGi", gp.getNcbiGi() );
            gpm.put( "description", gp.getDescription() );
            PhysicalLocation pl = gp.getPhysicalLocation();
            if ( pl != null ) {
                Map<String, Object> plm = new LinkedHashMap<>();
                plm.put( "nucleotide", pl.getNucleotide() );
                plm.put( "nucleotideLength", pl.getNucleotideLength() );
                plm.put( "strand", pl.getStrand() );
                if ( pl.getChromosome() != null ) {
                    plm.put( "chromosome", pl.getChromosome().getName() );
                }
                gpm.put( "physicalLocation", plm );
            }
            if ( gp.getGene() != null ) {
                Map<String, Object> gm = new LinkedHashMap<>();
                gm.put( "officialSymbol", gp.getGene().getOfficialSymbol() );
                gm.put( "officialName", gp.getGene().getOfficialName() );
                gm.put( "ncbiGeneId", gp.getGene().getNcbiGeneId() );
                if ( gp.getGene().getTaxon() != null ) {
                    gm.put( "taxonCommonName", gp.getGene().getTaxon().getCommonName() );
                }
                gpm.put( "gene", gm );
            }
            m.put( "geneProduct", gpm );
        }
        return m;
    }

    private static List<PslEntry> parsePsl() throws IOException {
        List<PslEntry> out = new ArrayList<>();
        try ( InputStream in = new GZIPInputStream(
                new ClassPathResource( PSL_RESOURCE ).getInputStream() );
              BufferedReader br = new BufferedReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) ) ) {
            String line;
            while ( ( line = br.readLine() ) != null ) {
                if ( line.isEmpty() || line.startsWith( "psLayout" ) || line.startsWith( "match" )
                        || line.startsWith( "---" ) || Character.isLetter( line.charAt( 0 ) ) ) {
                    continue;
                }
                String[] f = line.split( "\t" );
                if ( f.length < 21 ) continue;
                PslEntry e = new PslEntry();
                e.strand = f[8];
                e.chrom = f[13];
                e.queryStart = Long.parseLong( f[15] );
                e.queryEnd = Long.parseLong( f[16] );
                e.sizes = f[18];
                e.starts = f[20];
                out.add( e );
            }
        }
        return out;
    }

    private static class PslEntry {
        String chrom;
        String strand;
        Long queryStart;
        Long queryEnd;
        String starts;
        String sizes;
    }
}

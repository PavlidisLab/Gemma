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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.sequence.ShellDelegatingBlat;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.goldenpath.GoldenPathQuery;
import ubic.gemma.core.goldenpath.GoldenPathQueryFactory;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysis;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysisFactory;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest5;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.core.util.FileTools;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fast mock variant of {@link CompositeSequenceGeneMapperServiceTest}.
 * <p>
 * Mirrors the assertions of the {@code @Tag("goldenPath")} truth-source test,
 * but replaces the live UCSC GoldenPath lookup with a Mockito stub backed by
 * a JSON fixture captured from hg19 (see
 * {@code data/loader/genome/goldenpath/gpl96-hg19-fixture.json} and
 * {@link GoldenPathFixtureRecorder}).
 * <p>
 * The seam is the {@link GoldenPathSequenceAnalysisFactory} bean
 * (commit {@code 317ea9c785}). The inner {@link FastGoldenPathConfig} registers
 * a {@code @Primary} factory that returns a Mockito-mocked
 * {@link GoldenPathSequenceAnalysis} whose {@code findAssociations} responses
 * come from the fixture.
 */
@ContextConfiguration(classes = CompositeSequenceGeneMapperServiceFastTest.FastGoldenPathConfig.class, inheritLocations = false)
@Tag("slow")
public class CompositeSequenceGeneMapperServiceFastTest extends AbstractGeoServiceTest5 {

    static final String FIXTURE_RESOURCE = "/data/loader/genome/goldenpath/gpl96-hg19-fixture.json";

    private final String arrayAccession = "GPL96";
    private final ShellDelegatingBlat blat = new ShellDelegatingBlat();
    private final String csName = "117_at";
    private final String geneOfficialSymbol = "HSPA6";
    private ArrayDesign ad = null;

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private GeoService geoService;
    @Autowired
    private ArrayDesignSequenceProcessingService sequenceProcessingService;
    @Autowired
    private ArrayDesignSequenceAlignmentService aligner;
    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService;
    @Autowired
    private GeneWriteService geneWriteService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    @Configuration
    @TestComponent
    @ImportResource("classpath*:ubic/gemma/applicationContext-*.xml")
    static class FastGoldenPathConfig {

        /**
         * Sibling factory used by {@link
         * ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl}.
         * For this test we never have cached alignments on GoldenPath, so the
         * mock query returns no hits — the test feeds in pre-canned BLAT
         * results via the {@code processArrayDesign(ad, taxon, results)}
         * overload, which adds GoldenPath-cached alignments on top.
         */
        @Bean
        @Primary
        public GoldenPathQueryFactory mockGoldenPathQueryFactory() {
            return taxon -> {
                GoldenPathQuery stub = mock( GoldenPathQuery.class );
                lenient().when( stub.getTaxon() ).thenReturn( taxon );
                lenient().when( stub.findAlignments( anyString() ) )
                        .thenReturn( Collections.emptyList() );
                return stub;
            };
        }

        @Bean
        @Primary
        public GoldenPathSequenceAnalysisFactory mockGoldenPathFactory() throws IOException {
            Map<FindAssociationsKey, List<BlatAssociation>> table = FixtureLoader.load();
            return taxon -> {
                GoldenPathSequenceAnalysis stub = mock( GoldenPathSequenceAnalysis.class );
                lenient().when( stub.getTaxon() ).thenReturn( taxon );
                lenient().when( stub.findAssociations(
                        anyString(), anyLong(), anyLong(),
                        any(), any(), any(), any(), any() ) )
                        .thenAnswer( inv -> {
                            String chrom = inv.getArgument( 0 );
                            Long qs = inv.getArgument( 1 );
                            Long qe = inv.getArgument( 2 );
                            FindAssociationsKey key = new FindAssociationsKey( chrom, qs, qe );
                            List<BlatAssociation> hits = table.get( key );
                            if ( hits == null || hits.isEmpty() ) {
                                return null;
                            }
                            // Return a fresh list of fresh transient instances per call so the
                            // ProbeMapperImpl can populate blatResult/bioSequence on its own without
                            // sharing references across probes. The managed Taxon passed in by
                            // ArrayDesignProbeMapperServiceImpl is reused so persistence does not
                            // cascade-save a transient duplicate Taxon.
                            List<BlatAssociation> fresh = new ArrayList<>( hits.size() );
                            for ( BlatAssociation src : hits ) {
                                fresh.add( cloneTransient( src, taxon ) );
                            }
                            return fresh;
                        } );
                return stub;
            };
        }
    }

    @AfterEach
    public void cleanup() {
        ad = arrayDesignService.findByShortName( arrayAccession );
        if ( ad != null ) {
            for ( ExpressionExperiment ee : arrayDesignService.getExpressionExperiments( ad ) ) {
                eeService.remove( ee );
            }
            arrayDesignService.remove( ad );
        }

        Collection<Gene> genes = geneService.loadAll();
        for ( Gene gene : genes ) {
            try {
                geneService.remove( gene );
            } catch ( Exception ignored ) {
                // genes from other tests, fine
            }
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        cleanup();
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "platform" ) ) );
        @SuppressWarnings("unchecked")
        Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService
                .fetchAndLoad( arrayAccession, true, true, false );
        ad = ads.iterator().next();
        ad = arrayDesignService.thaw( ad );

        loadData();
    }

    @Test
    public void testGetCompositeSequencesByGeneId() {
        Collection<Gene> genes = geneService.findByOfficialSymbol( geneOfficialSymbol );
        if ( genes == null || genes.isEmpty() )
            return;
        Gene g = genes.iterator().next();
        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequences( g, true );
        assertNotNull( compositeSequences );
        assertEquals( compositeSequences.size(), 1 );
        assertEquals( ( compositeSequences.iterator().next() ).getName(), csName );
    }

    @Test
    public void testGetGenesForCompositeSequence() {
        CompositeSequence cs = compositeSequenceService.findByName( ad, csName );
        if ( cs == null )
            return;
        Collection<Gene> genes = compositeSequenceService.getGenes( cs, false );
        assertNotNull( genes );
        assertEquals( 1, genes.size() );
        assertEquals( geneOfficialSymbol, genes.iterator().next().getName() );

        Map<CompositeSequence, Collection<BlatResult>> alignments = arrayDesignService.getAlignments( ad );
        assertTrue( !alignments.isEmpty() );
        for ( CompositeSequence c : alignments.keySet() ) {
            assertTrue( !alignments.get( c ).isEmpty() );
        }
    }

    private void blatCollapsedSequences() throws IOException {
        Taxon taxon = taxonService.findByScientificName( "Homo sapiens" );
        InputStream blatResultInputStream = new GZIPInputStream(
                new ClassPathResource( "/data/loader/genome/gpl96.blatresults.psl.gz" ).getInputStream() );
        Collection<BlatResult> results = blat.processPsl( blatResultInputStream, taxon );
        aligner.processArrayDesign( ad, taxon, results );
        // routed through the mocked factory
        arrayDesignProbeMapperService.processArrayDesign( ad );
    }

    private void loadData() throws Exception {
        loadGeneData();
        loadSequenceData();
        blatCollapsedSequences();
    }

    private void loadGeneData() throws Exception {
        NcbiGeneLoader loader = new NcbiGeneLoader();
        loader.setTaxonService( taxonService );
        loader.setGeneWriteService( geneWriteService );

        String filePath = FileTools.resourceToPath( "/data/loader/genome/gene" );
        String geneInfoFile = filePath + File.separatorChar + "selected_gene_info.gz";
        String gene2AccFile = filePath + File.separatorChar + "selected_gene2accession.gz";
        String geneHistoryFile = filePath + File.separatorChar + "selected_gene_history.gz";
        loader.load( geneInfoFile, gene2AccFile, geneHistoryFile, null, true );
    }

    private void loadSequenceData() throws IOException {
        try ( InputStream sequenceFile = getClass()
                .getResourceAsStream( "/data/loader/genome/gpl96_short.sequences2.fasta" ) ) {
            sequenceProcessingService.processArrayDesign( ad, sequenceFile, SequenceType.EST,
                    taxonService.findByCommonName( "human" ) );
        }
    }

    /**
     * Returns a fresh transient {@link BlatAssociation} cloned from a fixture
     * row, with the gene's taxon resolved to the supplied managed Taxon
     * so persistence does not attempt to cascade-save a transient duplicate.
     * ProbeMapperImpl will set blatResult + bioSequence after we return.
     */
    private static BlatAssociation cloneTransient( BlatAssociation src, Taxon managedTaxon ) {
        BlatAssociation copy = BlatAssociation.Factory.newInstance();
        copy.setOverlap( src.getOverlap() );
        copy.setThreePrimeDistance( src.getThreePrimeDistance() );
        copy.setThreePrimeDistanceMeasurementMethod( src.getThreePrimeDistanceMeasurementMethod() );

        GeneProduct gp = src.getGeneProduct();
        if ( gp != null ) {
            GeneProduct gpCopy = GeneProduct.Factory.newInstance();
            gpCopy.setName( gp.getName() );
            gpCopy.setNcbiGi( gp.getNcbiGi() );
            PhysicalLocation pl = gp.getPhysicalLocation();
            if ( pl != null ) {
                PhysicalLocation plCopy = PhysicalLocation.Factory.newInstance();
                plCopy.setNucleotide( pl.getNucleotide() );
                plCopy.setNucleotideLength( pl.getNucleotideLength() );
                plCopy.setStrand( pl.getStrand() );
                gpCopy.setPhysicalLocation( plCopy );
            }
            if ( gp.getGene() != null ) {
                Gene geneSrc = gp.getGene();
                Gene geneCopy = Gene.Factory.newInstance();
                geneCopy.setOfficialSymbol( geneSrc.getOfficialSymbol() );
                geneCopy.setOfficialName( geneSrc.getOfficialName() );
                geneCopy.setNcbiGeneId( geneSrc.getNcbiGeneId() );
                geneCopy.setTaxon( managedTaxon );
                gpCopy.setGene( geneCopy );
            }
            copy.setGeneProduct( gpCopy );
        }
        return copy;
    }

    /**
     * Composite key used to index the fixture by the GoldenPath call signature.
     * We key on (chrom, queryStart, queryEnd) and normalise the chromosome to
     * its bare form (no {@code chr} prefix), matching how the persisted
     * {@code BlatResult.targetChromosome.name} is shaped in Gemma's DB. The
     * recorder writes UCSC-style {@code chr1}, the runtime calls with
     * {@code 1}; the normaliser bridges the two.
     * <p>
     * Strand is excluded: the runtime path under this test ({@code SequenceType.EST})
     * always passes {@code strand=null} via
     * {@link ubic.gemma.core.analysis.sequence.ProbeMapperImpl#processBlatResult},
     * and the captured GoldenPath responses already incorporate strand-agnostic
     * results. The {@code starts} / {@code sizes} arguments influence overlap
     * math inside the real implementation but the captured response already
     * carries that work.
     */
    static final class FindAssociationsKey {
        final String chrom;
        final Long queryStart;
        final Long queryEnd;

        FindAssociationsKey( String chrom, Long queryStart, Long queryEnd ) {
            this.chrom = normaliseChrom( chrom );
            this.queryStart = queryStart;
            this.queryEnd = queryEnd;
        }

        private static String normaliseChrom( String chrom ) {
            if ( chrom == null ) return null;
            return chrom.startsWith( "chr" ) ? chrom.substring( 3 ) : chrom;
        }

        @Override
        public boolean equals( Object o ) {
            if ( !( o instanceof FindAssociationsKey ) ) return false;
            FindAssociationsKey k = ( FindAssociationsKey ) o;
            return java.util.Objects.equals( chrom, k.chrom )
                    && java.util.Objects.equals( queryStart, k.queryStart )
                    && java.util.Objects.equals( queryEnd, k.queryEnd );
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash( chrom, queryStart, queryEnd );
        }

        @Override
        public String toString() {
            return "(" + chrom + ":" + queryStart + "-" + queryEnd + ")";
        }
    }

    /**
     * Loads the captured fixture once and converts each entry into a list of
     * transient {@link BlatAssociation} prototypes (cloned per-call at
     * lookup time so ProbeMapperImpl can mutate them safely).
     */
    static final class FixtureLoader {
        static Map<FindAssociationsKey, List<BlatAssociation>> load() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root;
            try ( InputStream in = new ClassPathResource( FIXTURE_RESOURCE ).getInputStream() ) {
                root = mapper.readTree( in );
            }
            Map<FindAssociationsKey, List<BlatAssociation>> table = new HashMap<>();
            JsonNode calls = root.path( "calls" );
            for ( JsonNode call : calls ) {
                FindAssociationsKey key = new FindAssociationsKey(
                        call.path( "chrom" ).asText(),
                        call.path( "queryStart" ).asLong(),
                        call.path( "queryEnd" ).asLong() );
                JsonNode associations = call.path( "associations" );
                if ( associations.size() == 0 ) {
                    table.put( key, Collections.emptyList() );
                    continue;
                }
                List<BlatAssociation> list = new ArrayList<>( associations.size() );
                for ( JsonNode a : associations ) {
                    list.add( deserialise( a ) );
                }
                table.put( key, list );
            }
            return table;
        }

        private static BlatAssociation deserialise( JsonNode a ) {
            BlatAssociation ba = BlatAssociation.Factory.newInstance();
            if ( !a.path( "overlap" ).isMissingNode() && !a.path( "overlap" ).isNull() ) {
                ba.setOverlap( a.path( "overlap" ).asInt() );
            }
            if ( !a.path( "threePrimeDistance" ).isMissingNode() && !a.path( "threePrimeDistance" ).isNull() ) {
                ba.setThreePrimeDistance( a.path( "threePrimeDistance" ).asLong() );
            }
            if ( !a.path( "threePrimeDistanceMethod" ).isMissingNode() && !a.path( "threePrimeDistanceMethod" ).isNull() ) {
                ba.setThreePrimeDistanceMeasurementMethod(
                        ThreePrimeDistanceMethod.valueOf( a.path( "threePrimeDistanceMethod" ).asText() ) );
            }
            JsonNode gpNode = a.path( "geneProduct" );
            if ( !gpNode.isMissingNode() && !gpNode.isNull() ) {
                GeneProduct gp = GeneProduct.Factory.newInstance();
                if ( !gpNode.path( "name" ).isNull() ) gp.setName( gpNode.path( "name" ).asText() );
                if ( !gpNode.path( "ncbiGi" ).isNull() ) gp.setNcbiGi( gpNode.path( "ncbiGi" ).asText() );
                if ( !gpNode.path( "description" ).isNull() ) gp.setDescription( gpNode.path( "description" ).asText() );
                JsonNode plNode = gpNode.path( "physicalLocation" );
                if ( !plNode.isMissingNode() && !plNode.isNull() ) {
                    PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
                    if ( !plNode.path( "nucleotide" ).isNull() ) pl.setNucleotide( plNode.path( "nucleotide" ).asLong() );
                    if ( !plNode.path( "nucleotideLength" ).isNull() ) pl.setNucleotideLength( plNode.path( "nucleotideLength" ).asInt() );
                    if ( !plNode.path( "strand" ).isNull() ) pl.setStrand( plNode.path( "strand" ).asText() );
                    // Chromosome left transient with name only; the real ProbeMapperImpl does
                    // not require a fully-populated chromosome for downstream persistence
                    // because GenomePersister recreates Chromosome via cache by name+taxon.
                    gp.setPhysicalLocation( pl );
                }
                JsonNode geneNode = gpNode.path( "gene" );
                if ( !geneNode.isMissingNode() && !geneNode.isNull() ) {
                    Gene g = Gene.Factory.newInstance();
                    if ( !geneNode.path( "officialSymbol" ).isNull() ) g.setOfficialSymbol( geneNode.path( "officialSymbol" ).asText() );
                    if ( !geneNode.path( "officialName" ).isNull() ) g.setOfficialName( geneNode.path( "officialName" ).asText() );
                    if ( !geneNode.path( "ncbiGeneId" ).isNull() ) g.setNcbiGeneId( geneNode.path( "ncbiGeneId" ).asInt() );
                    // Taxon left transient — name-only; persister resolves via cache.
                    if ( !geneNode.path( "taxonCommonName" ).isNull() ) {
                        Taxon t = Taxon.Factory.newInstance();
                        t.setCommonName( geneNode.path( "taxonCommonName" ).asText() );
                        g.setTaxon( t );
                    }
                    gp.setGene( g );
                }
                ba.setGeneProduct( gp );
            }
            return ba;
        }
    }
}

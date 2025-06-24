/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.sequence.ShellDelegatingBlat;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperServiceImpl;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.core.util.test.category.GoldenPathTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

/**
 * This test makes use of the {@link ArrayDesignProbeMapperServiceImpl}. These tests add array data and gene data to the
 * database to be used for testing. GPL96.
 *
 * @author keshav
 */
@Category({ GoldenPathTest.class, SlowTest.class })
public class CompositeSequenceGeneMapperServiceTest extends AbstractGeoServiceTest {

    private final String arrayAccession = "GPL96";
    private final ShellDelegatingBlat blat = new ShellDelegatingBlat();
    private final String csName = "117_at";// "218120_s_at";
    private final String geneOfficialSymbol = "HSPA6";// "HMOX2";
    private ArrayDesign ad = null;
    @Autowired
    private ArrayDesignService arrayDesignService = null;
    @Autowired
    private CompositeSequenceService compositeSequenceService = null;
    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private GeneService geneService = null;

    // private static boolean alreadyPersistedData = false;

    @Autowired
    private GeoService geoService = null;

    @Autowired
    private ArrayDesignSequenceProcessingService sequenceProcessingService;

    @Autowired
    private ArrayDesignSequenceAlignmentService aligner;

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    @After
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
                // not a problem, we have genes from other tests, etc.
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        this.cleanup();
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "platform" ) ) );
        @SuppressWarnings("unchecked") final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService
                .fetchAndLoad( arrayAccession, true, true, false );
        ad = ads.iterator().next();

        ad = arrayDesignService.thaw( ad );

        this.loadData();

    }

    /**
     * Tests finding the composite sequences for a given gene id.
     */
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

    /**
     * Tests finding all genes for a given composite sequence.
     */
    @Test
    public void testGetGenesForCompositeSequence() {

        CompositeSequence cs = compositeSequenceService.findByName( ad, csName );

        if ( cs == null )
            return;

        Collection<Gene> genes = compositeSequenceService.getGenes( cs );

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

        InputStream blatResultInputStream = new GZIPInputStream( new ClassPathResource( "/data/loader/genome/gpl96.blatresults.psl.gz" ).getInputStream() );

        Collection<BlatResult> results = blat.processPsl( blatResultInputStream, taxon );

        aligner.processArrayDesign( ad, taxon, results );

        // real stuff.
        arrayDesignProbeMapperService.processArrayDesign( ad );
    }

    /**
     * Adds gene, sequence, and blat results to the database.
     */
    private void loadData() throws Exception {

        // insert the needed genes and geneproducts into the system.
        this.loadGeneData();

        // needed to fill in the sequence information for blat scoring.
        this.loadSequenceData();

        // fill in the blat results.
        this.blatCollapsedSequences();

    }

    private void loadGeneData() throws Exception {
        NcbiGeneLoader loader = new NcbiGeneLoader();
        loader.setTaxonService( taxonService );
        loader.setPersisterHelper( this.persisterHelper );

        String filePath = FileTools.resourceToPath( "/data/loader/genome/gene" );

        String geneInfoFile = filePath + File.separatorChar + "selected_gene_info.gz";
        String gene2AccFile = filePath + File.separatorChar + "selected_gene2accession.gz";
        String geneHistoryFile = filePath + File.separatorChar + "selected_gene_history.gz";
        loader.load( geneInfoFile, gene2AccFile, geneHistoryFile, null, true );
    }

    private void loadSequenceData() throws IOException {
        try ( InputStream sequenceFile = this.getClass()
                .getResourceAsStream( "/data/loader/genome/gpl96_short.sequences2.fasta" ) ) {

            sequenceProcessingService
                    .processArrayDesign( ad, sequenceFile, SequenceType.EST, taxonService.findByCommonName( "human" ) );
        }

    }

}

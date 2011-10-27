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
package ubic.gemma.analysis.service;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.apps.Blat;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.ConfigUtils;

/**
 * This integration test makes use of the {@link ArrayDesignProbeMapperService}. These tests add array data and gene
 * data to the database to be used for testing. GPL96.
 * 
 * @author keshav
 * @version $Id$
 */
public class CompositeSequenceGeneMapperServiceIntegrationTest extends AbstractGeoServiceTest {

    @Autowired
    CompositeSequenceService compositeSequenceService = null;

    @Autowired
    GeneService geneService = null;

    @Autowired
    ArrayDesignService arrayDesignService = null;

    @Autowired
    GeoDatasetService geoService = null;

    ArrayDesign ad = null;

    String arrayAccession = "GPL96";

    String csName = "117_at";// "218120_s_at";

    String geneOfficialSymbol = "HSPA6";// "HMOX2";

    Blat blat = new Blat();

    static boolean alreadyPersistedData = false;

    /**
     *
     */
    @Before
    public void setup() throws Exception {

        String path = getTestFileBasePath();

        ad = arrayDesignService.findByShortName( arrayAccession );

        if ( !alreadyPersistedData ) {
            // first load small two-color
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "platform" ) );
            final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( arrayAccession,
                    true, true, false, false );
            ad = ads.iterator().next();

            ad = arrayDesignService.thaw( ad );

            loadData();

            alreadyPersistedData = true;
        }
    }

    /**
     * Tests finding the composite sequences for a given gene id.
     * 
     * @throws Exception
     */
    @Test
    public void testGetCompositeSequencesByGeneId() throws Exception {

        Collection<Gene> genes = geneService.findByOfficialSymbol( geneOfficialSymbol );

        if ( genes == null || genes.isEmpty() ) return;

        Gene g = genes.iterator().next();

        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( g.getId() );

        log.info( "Found " + compositeSequences.size() + " composite sequence(s) for gene " + g.getOfficialSymbol()
                + " ... " );

        assertNotNull( compositeSequences );
        // assertEquals( compositeSequences.size(), 1 );
        // assertEquals( ( compositeSequences.iterator().next() ).getName(), csName );
    }

    /**
     * Tests finding all genes for a given composite sequence.
     * 
     * @throws Exception
     */
    @Test
    public void testGetGenesForCompositeSequence() throws Exception {

        CompositeSequence cs = compositeSequenceService.findByName( ad, csName );

        if ( cs == null ) return;

        Collection<Gene> genes = compositeSequenceService.getGenes( cs );

        log.info( "Found " + genes.size() + " gene(s) for " + cs.getName() );

        assertNotNull( genes );
        // assertEquals( genes.size(), 1 );
        // assertEquals( genes.iterator().next().getName(), geneOfficialSymbol );
    }

    /**
     * @throws IOException
     */
    private void blatCollapsedSequences() throws IOException {

        Taxon taxon = taxonService.findByScientificName( "Homo sapiens" );

        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );

        InputStream blatResultInputStream = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/genome/gpl140.blatresults.psl.gz" ) ); // TODO create the correct blat results file

        Collection<BlatResult> results = blat.processPsl( blatResultInputStream, taxon );

        aligner.processArrayDesign( ad, taxon, results );

        // real stuff.
        ArrayDesignProbeMapperService arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );
        arrayDesignProbeMapperService.processArrayDesign( ad );
    }

    /**
     * Adds gene, sequence, and blat results to the database.
     * 
     * @throws Exception
     */
    private void loadData() throws Exception {

        // insert the needed genes and geneproducts into the system.
        loadGeneData();

        // needed to fill in the sequence information for blat scoring.
        loadSequenceData();

        // fill in the blat results.
        blatCollapsedSequences();

    }

    /**
     * 
     *
     */
    private void loadGeneData() {
        NcbiGeneLoader loader = new NcbiGeneLoader();
        loader.setTaxonService( taxonService );
        loader.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
        String filePath = ConfigUtils.getString( "gemma.home" ) + File.separatorChar;
        filePath = filePath + "gemma-core/src/test/resources/data/loader/genome/gene";
        String geneInfoFile = filePath + File.separatorChar + "selected_gene_info.gz";
        String gene2AccFile = filePath + File.separatorChar + "selected_gene2accession.gz";
        String geneHistoryFile = filePath + File.separatorChar + "selected_gene_history.gz";
        loader.load( geneInfoFile, gene2AccFile, geneHistoryFile, null, true );
    }

    /**
     * @throws IOException
     */
    private void loadSequenceData() throws IOException {
        InputStream sequenceFile = this.getClass().getResourceAsStream(
                "/data/loader/genome/gpl96_short.sequences.fasta" );
        ArrayDesignSequenceProcessingService sequenceProcessingService = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );

        sequenceProcessingService.processArrayDesign( ad, sequenceFile, SequenceType.EST );

    }

}

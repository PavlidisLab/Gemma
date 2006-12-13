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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import ubic.gemma.apps.Blat;
import ubic.gemma.apps.LoadExpressionDataCli;
import ubic.gemma.genome.CompositeSequenceGeneMapperService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.util.ConfigUtils;

/**
 * This integration test makes use of both the {@link ArrayDesignProbeMapperService} and the
 * {@link LoadExpressionDataCli}. These tests add array data, gene data, and expression data to the database to be used
 * for testing. Specifically, the tests use GSE994 and the corresponding platform GPL96.
 * 
 * @author keshav
 * @version $Id$
 */
public class CompositeSequenceGeneMapperServiceIntegrationTest extends AbstractGeoServiceTest {

    CompositeSequenceGeneMapperService compositeSequenceGeneMapperService = null;

    CompositeSequenceService compositeSequenceService = null;

    GeneService geneService = null;

    ArrayDesignService arrayDesignService = null;

    ExpressionExperimentService expressionExperimentService = null;

    AbstractGeoService geoService = null;

    ArrayDesign ad = null;

    String arrayAccession = "GPL96";

    String eeShortName = "GSE994";

    String csName = "218120_s_at";

    String geneOfficialSymbol = "HMOX2";

    Blat blat = new Blat();

    /**
     *
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();

        compositeSequenceGeneMapperService = ( CompositeSequenceGeneMapperService ) this
                .getBean( "compositeSequenceGeneMapperService" );

        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        compositeSequenceService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );

        geneService = ( GeneService ) this.getBean( "geneService" );

        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );

        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );

        String path = getTestFileBasePath();

        ad = arrayDesignService.findByShortName( arrayAccession );

        if ( ad == null ) {

            // first load small two-color
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "platform" ) );
            geoService.setLoadPlatformOnly( true );
            final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( arrayAccession );
            ad = ads.iterator().next();
        }
        arrayDesignService.thaw( ad );

        Collection<Gene> foundGenes = geneService.findByOfficialSymbol( geneOfficialSymbol );
        if ( foundGenes == null || foundGenes.isEmpty() ) {
            processArrayDesignWithData();
        }

        if ( expressionExperimentService.findByShortName( eeShortName ) == null ) {
            // createExpressionExperiment(); //TODO add this in after creating a stripped down version of GSE994.
        }
    }

    /**
     *
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
    }

    /**
     * Runs Blat on composite sequences in array design.
     * 
     * @throws Exception
     */
    private void processArrayDesignWithData() throws Exception {

        Taxon taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Homo sapiens" );

        // insert the needed genes and geneproducts into the system.
        NcbiGeneLoader loader = new NcbiGeneLoader();
        loader.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
        String filePath = ConfigUtils.getString( "gemma.home" ) + File.separatorChar;
        assert filePath != null;
        filePath = filePath + "gemma-core/src/test/resources/data/loader/genome/gene";
        String geneInfoFile = filePath + File.separatorChar + "selected_gene_info.gz";
        String gene2AccFile = filePath + File.separatorChar + "selected_gene2accession.gz";
        loader.load( geneInfoFile, gene2AccFile, true );

        // TODO need hg18 for this - check to see if connection exists. If not, don't add sequence information.
        // needed to fill in the sequence information for blat scoring.
        // InputStream sequenceFile = this.getClass().getResourceAsStream( "/data/loader/genome/gpl140.sequences.fasta"
        // );
        // ArrayDesignSequenceProcessingService app = ( ArrayDesignSequenceProcessingService ) getBean(
        // "arrayDesignSequenceProcessingService" );
        //
        // app.processArrayDesign( ad, sequenceFile, SequenceType.EST );
        //
        // // fill in the blat results. Note that each time you run this test you
        // // get the results loaded again (so they
        // // pile up)
        // ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean(
        // "arrayDesignSequenceAlignmentService" );
        //
        // InputStream blatResultInputStream = new GZIPInputStream( this.getClass().getResourceAsStream(
        // "/data/loader/genome/gpl140.blatresults.psl.gz" ) );
        //
        // Collection<BlatResult> results = blat.processPsl( blatResultInputStream, taxon );
        //
        // aligner.processArrayDesign( ad, results );
        //
        // // real stuff.
        // ArrayDesignProbeMapperService arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
        // .getBean( "arrayDesignProbeMapperService" );
        // arrayDesignProbeMapperService.processArrayDesign( ad );
        //
        // // possibly assert no unexpected new genes or gene products were added.
        //
        // // expect to see added (not in NCBI) as of 10/28
        // /*
        // * CR749610 (gene and gene product) (HYOU1) NM_001008411 (product of TDG) This product is in NCBI gene Another
        // * anonymous product of TDG. CR541839 (HMOX2) CR456760 (HMOX2)
        // */

    }

    /**
     * Loads GSE994. The corresponding ArrayDesign information is loaded in {@link createArrayDesign()}
     */
    private void createExpressionExperiment() {
        // FIXME don't use this. GSE994 will be too large. Instead make a stripped down version like
        // you have for GPL96.
        String[] args = { "-u", "administrator", "-p", "testing", "-a", eeShortName };

        LoadExpressionDataCli.main( args );
    }

    /**
     * Tests finding genes given official symbols.
     * 
     * @throws Exception
     */
    public void testFindGenesByOfficialSymbols() throws Exception {

        Collection<String> geneSymbols = new HashSet<String>();
        geneSymbols.add( geneOfficialSymbol );

        LinkedHashMap<String, Collection<Gene>> genesMap = compositeSequenceGeneMapperService
                .findGenesByOfficialSymbols( geneSymbols );

        Collection<String> keyset = genesMap.keySet();
        for ( String key : keyset ) {
            log.info( "key: " + key + " , gene: " + genesMap.get( key ) );
        }
        assertNotNull( genesMap );
        assertEquals( keyset.size(), 1 );

    }

    /**
     * Tests finding all genes for a given composite sequence.
     * 
     * @throws Exception
     */
    public void testGetGenesForCompositeSequence() throws Exception {

        CompositeSequence cs = compositeSequenceService.findByName( ad, csName );
        if ( cs == null ) return;

        Collection<Gene> genes = compositeSequenceGeneMapperService.getGenesForCompositeSequence( cs );

        assertNotNull( genes );

    }

    /**
     * Tests finding composite sequences for a given collection of composite sequence names in a specific array design.
     * 
     * @throws Exception
     */
    public void testfindByNamesInArrayDesigns() {
        /* test getting the matching composite sequences */
        List<String> compositeSequenceNames = new ArrayList<String>();
        compositeSequenceNames.add( csName );

        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        arrayDesignService.thaw( ad );
        Collection<CompositeSequence> compositeSequences = compositeSequenceService.findByNamesInArrayDesigns(
                compositeSequenceNames, ads );

        assertNotNull( compositeSequences );

    }

    /**
     * Tests finding the composite sequences for a given gene id.
     * 
     * @throws Exception
     */
    public void testGetCompositeSequencesByGeneId() throws Exception {

        Collection<Gene> genes = geneService.findByOfficialSymbol( geneOfficialSymbol );

        assertNotNull( genes );
        assertEquals( genes.size(), 1 );

        Iterator iter = genes.iterator();
        Gene g = ( Gene ) iter.next();

        Collection<CompositeSequence> compositeSequences = compositeSequenceGeneMapperService
                .getCompositeSequencesByGeneId( g.getId() );

        log.info( compositeSequences.size() + " composite sequences for gene " + g.getOfficialSymbol() + " ... " );
        for ( CompositeSequence cs : compositeSequences ) {
            log.info( "CompositeSequence: " + cs.getName() );
        }
    }

    @Override
    protected void init() {
        log.info( "nothing to do in init." );
    }
}

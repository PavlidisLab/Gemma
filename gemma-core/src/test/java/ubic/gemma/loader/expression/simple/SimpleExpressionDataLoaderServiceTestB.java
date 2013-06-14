package ubic.gemma.loader.expression.simple;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.ontology.OntologyService; 

public class SimpleExpressionDataLoaderServiceTestB extends AbstractGeoServiceTest {

    @Autowired
    protected GeoService geoService;

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    OntologyService ontologyService;

    ExperimentalFactorOntologyService mos;

    @Autowired
    SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    ExpressionExperiment ee;

    @Before
    public void setUp() throws Exception {
        super.executeSqlScript( "/script/sql/add-fish-taxa.sql", false );
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            ee = eeService.load( ee.getId() );
            eeService.delete( ee );
        }
    }

    /**
     * @throws Exception
     */
    @Test
    public final void testLoadWithDuplicateBioMaterials() throws Exception {

        /*
         * Have to add ssal for this platform.
         */

        // Taxon salmon = taxonService.findByScientificName( "atlantic salmon" );

        // if ( salmon == null ) {

        Taxon salmon = taxonService.findByCommonName( "atlantic salmon" );
        // }

        assertNotNull( salmon );

        /*
         * Load the array design (platform).
         */
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath() ) );
        geoService.fetchAndLoad( "GPL2716", true, true, false, false );
        ArrayDesign ad = arrayDesignService.findByShortName( "GPL2716" );

        assertNotNull( ad );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/flatfileload/gill2006hormone.head.txt" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        mos = ontologyService.getExperimentalFactorOntologyService();
        if ( !mos.isOntologyLoaded() ) mos.startInitializationThread( true );
        while ( !mos.isOntologyLoaded() ) {
            Thread.sleep( 1000 );
            log.info( "Waiting for mgedontology to load" );
        }

        makeMetaData( salmon, ad, metaData );

        ee = simpleExpressionDataLoaderService.create( metaData, data );

        /*
         * Do second one that has overlapping bioassay names.
         */
        data = this.getClass().getResourceAsStream( "/data/loader/expression/flatfileload/gill2006oceanfate.head.txt" );

        metaData = new SimpleExpressionExperimentMetaData();

        assertNotNull( salmon );

        makeMetaData( salmon, ad, metaData );

        ExpressionExperiment a = simpleExpressionDataLoaderService.create( metaData, data );

        // ugly, but try to clean up .
        eeService.delete( a );

    }

    /**
     * @param salmon
     * @param ad
     * @param metaData
     */
    private void makeMetaData( Taxon salmon, ArrayDesign ad, SimpleExpressionExperimentMetaData metaData ) {
        metaData.setShortName( RandomStringUtils.randomAlphabetic( 10 ) );
        metaData.setDescription( "bar" );
        metaData.setIsRatio( false );
        metaData.setTaxon( salmon );
        metaData.setQuantitationTypeName( "value" );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.getArrayDesigns().add( ad );
    }

}

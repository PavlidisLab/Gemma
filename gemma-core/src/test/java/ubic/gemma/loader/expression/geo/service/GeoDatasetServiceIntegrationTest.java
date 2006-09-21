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
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.ExpressionDataMatrixService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.AbstractGeoServiceTest;

/**
 * This is an integration test
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetServiceIntegrationTest extends AbstractGeoServiceTest {
    ExpressionExperimentService eeService;
    ExpressionExperiment ee;
    protected AbstractGeoService geoService;

    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        geoService = ( GeoDatasetService ) this.getBean( "geoDatasetService" );
        geoService.setLoadPlatformOnly( false );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        init();
    }

    @Override
    protected void onTearDown() throws Exception {
        // this.delete( ee );
    }

    /**
     * Clean up expression experiments inserted by this test.
     * 
     * @param expExp
     */
    private void delete( ExpressionExperiment expExp ) {
        BioAssayDimensionDao daddao = ( BioAssayDimensionDao ) this.getBean( "bioAssayDimensionDao" );
        DesignElementDataVectorDao dedvdao = ( DesignElementDataVectorDao ) this.getBean( "designElementDataVectorDao" );
        Set<BioAssayDimension> dims = new HashSet<BioAssayDimension>();

        // avoid lazy-load errors.
        SessionFactory sessf = ( SessionFactory ) this.getBean( "sessionFactory" );
        Session sess = sessf.openSession();
        sess.beginTransaction();
        sess.lock( expExp, LockMode.NONE );
        Collection<DesignElementDataVector> designElementDataVectors = expExp.getDesignElementDataVectors();
        designElementDataVectors.size(); // lazy-load...
        sess.getTransaction().commit();
        sess.evict( expExp );
        sess.close();

        for ( DesignElementDataVector dv : designElementDataVectors ) {
            BioAssayDimension dim = dv.getBioAssayDimension();
            dims.add( dim );
        }

        dedvdao.remove( designElementDataVectors );

        for ( BioAssayDimension dim : dims ) {
            daddao.remove( dim );
        }
        eeService.update( expExp );
        eeService.delete( expExp );

    }

    // ////////////////////////////////////////////////////////////
    // Full tests - only run if you have lots of time to kill
    // ////////////////////////////////////////////////////////////

    // /**
    // * This test uses just one dataset, one series (only about 900 probes), just the platform.
    // */
    // public void testFetchAndLoadOneSeriesPlatform() throws Exception {
    // geoService.setLoadPlatformOnly( true );
    // geoService.fetchAndLoad( "GSE2" );
    // }
    //
    // /**
    // * This test uses just one dataset, one series (only about 900 probes)
    // */
    // public void testFetchAndLoadOneSeries() throws Exception {
    // geoService.fetchAndLoad( "GSE2" );
    // }

    // /**
    // * One platform but many quantitation types.
    // */
    // public void testFetchAndLoadB() throws Exception {
    // geoService.fetchAndLoad( "GDS942" );
    // }
    //
    // /**
    // * two platforms.
    // */
    // public void testFetchAndLoadC() throws Exception {
    // geoService.fetchAndLoad( "GDS100" );
    // }
    //
    // public void testFetchAndLoadD() throws Exception {
    // geoService.fetchAndLoad( "GDS1033" );
    // }
    //
    // /**
    // * Three patforms
    // * @throws Exception
    // */
    // public void testFetchAndLoadE() throws Exception {
    // geoService.fetchAndLoad( "GDS835" );
    // }

    // Two samples don't have the same data columns etc.
    // public void testGDS637() throws Exception {
    // geoService.fetchAndLoad( "GDS637" );
    // }

    // // too big for memory <1gb - parkinson's model with 80 arrays.
    // public void testGSE30() throws Exception {
    // geoService.fetchAndLoad( "GSE30" );
    // }

    /**
     * This test uses all three MG-U74 arrays.
     */
    // public void testFetchAndLoadThreePlatforms() throws Exception {
    // gds.fetchAndLoad( "GDS243" );
    // }
    /**
     * HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see {@link 
     * http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search)
     */
    // public void testFetchAndLoadMultiChipPerSeries() throws Exception {
    // geoService.fetchAndLoad( "GDS472" );
    // }
    /**
     * This test uses just one dataset, one series
     */
    // public void testFetchAndLoadOneDataset() throws Exception {
    // geoService.fetchAndLoad( "GDS599" );
    // log.info( "**** done ****" );
    // }
    /**
     * Another basic one, but we also use cut-down versions for testing below.
     */
    // public void testFetchAndLoadG() throws Exception {
    // geoService.fetchAndLoad( "GDS994" );
    // }
    // /////////////////////////////////////////////////////////////
    // Medium-sized tests, quite a bit faster than the above but more realistic (in size) then the ones below
    // ////////////////////////////////////////////////////////////
    // public void testFetchAndLoadCacheExercise() throws Exception {
    // assert config != null;
    // String path = getTestFileBasePath();
    // geoService.setGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT + "gds994medium" ) );
    // this.setFlushModeCommit();
    // geoService.fetchAndLoad( "GDS994" );
    // }
    // ////////////////////////////////////////////////////////////
    // Unit tests, should run reasonably quickly.
    // ////////////////////////////////////////////////////////////
    /**
     * Original reason for test: yields audit trail errors.
     */
    public void testFetchAndLoadGDS775() throws Exception {
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gds775short" ) );
        ee = ( ExpressionExperiment ) geoService.fetchAndLoad( "GDS775" );
    }

    /**
     * problem case GDS22 - has lots of missing values and number format issues.
     */
    public void testFetchAndLoadGDS22() throws Exception {
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gds22Short" ) );

        ee = ( ExpressionExperiment ) geoService.fetchAndLoad( "GDS22" );
    }

    /*
     * Data Access Failure ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl; nested exception is
     * org.hibernate.TransientObjectException: edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl
     */
    public void testFetchAndLoadGDS994() throws Exception {
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gds994Short" ) );
        ee = ( ExpressionExperiment ) geoService.fetchAndLoad( "GDS994" );
    }

    //

    @SuppressWarnings("unchecked")
    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        // this.setFlushModeCommit();
        // this.setComplete();
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "shortTest" ) );

        /*
         * HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
         * http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
         */
        ExpressionExperiment newee = ( ExpressionExperiment ) geoService.fetchAndLoad( "GDS472" );

        // get the data back out.
        ExpressionExperimentService ees = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        QuantitationTypeService qts = ( QuantitationTypeService ) getBean( "quantitationTypeService" );

        ExpressionDataMatrixService edms = new ExpressionDataMatrixService();

        edms
                .setDesignElementDataVectorService( ( DesignElementDataVectorService ) getBean( "designElementDataVectorService" ) );

        ee = ees.findByName( "Normal Muscle - Female , Effect of Age" );

        // Recovering a quantitation type.
        QuantitationType qtf = QuantitationType.Factory.newInstance();
        qtf.setIsBackground( false );
        qtf.setName( "VALUE" );
        qtf.setRepresentation( PrimitiveType.DOUBLE );
        qtf.setGeneralType( GeneralType.QUANTITATIVE );
        qtf.setType( StandardQuantitationType.DERIVEDSIGNAL );
        qtf.setScale( ScaleType.UNSCALED );
        QuantitationType qt = qts.find( qtf );

        assertTrue( qt != null );
        assertTrue( ee != null );
        assertTrue( newee.equals( ee ) );

        DoubleMatrixNamed matrix = edms.getMatrix( newee, qt );

        if ( log.isDebugEnabled() ) {
            printMatrix( matrix );
        }

        assertTrue( matrix != null );

        assertEquals( 31, matrix.rows() );

        assertEquals( 15, matrix.columns() );

        double k = matrix.getRowByName( "200000_s_at" )[matrix.getColIndexByName( "GSE674_bioMaterial_14" )];
        assertEquals( 6357.0, k, 0.00001 );

        k = matrix.getRowByName( "1007_s_at" )[matrix.getColIndexByName( "GSE674_bioMaterial_14" )];
        assertEquals( 1558.0, k, 0.00001 );

    }

    /**
     * This test uses 4 data sets, 4 platforms, and samples that aren't run on all platforms. Insane! And has messed up
     * values in double and string conversion.
     * 
     * @throws Exception
     */
    public void testConversionGDS825Family() throws Exception {
        String path = getTestFileBasePath();

        // clean up possibly corrupt state from previous runs.
        // ee = eeService.findByName( "Breast Cancer Cell Line Experiment" );
        // if ( ee != null ) {
        // this.delete( ee );
        // }

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "complexShortTest" ) );
        ExpressionExperiment newee = ( ExpressionExperiment ) geoService.fetchAndLoad( "GDS825" );

        // get the data back out.
        QuantitationTypeService qts = ( QuantitationTypeService ) getBean( "quantitationTypeService" );

        ExpressionDataMatrixService edms = new ExpressionDataMatrixService();

        edms
                .setDesignElementDataVectorService( ( DesignElementDataVectorService ) getBean( "designElementDataVectorService" ) );

        ee = eeService.findByName( "Breast Cancer Cell Line Experiment" );
        QuantitationType qtf = QuantitationType.Factory.newInstance();

        // Affymetrix platform.
        qtf.setName( "VALUE" );
        qtf.setScale( ScaleType.UNSCALED );
        qtf.setRepresentation( PrimitiveType.DOUBLE );
        qtf.setGeneralType( GeneralType.QUANTITATIVE );
        qtf.setType( StandardQuantitationType.DERIVEDSIGNAL );
        QuantitationType qt = qts.find( qtf );

        assertTrue( qt != null );
        assertTrue( ee != null );
        assertTrue( newee.equals( ee ) );

        DoubleMatrixNamed matrix = edms.getMatrix( newee, qt );
        assertTrue( matrix != null );
        if ( log.isDebugEnabled() ) {
            printMatrix( matrix );
        }

        assertEquals( 116, matrix.rows() );

        assertEquals( 6, matrix.columns() );

        // GSM21252 on GDS823 (GPL97)
        double k = matrix.getRowByName( "224444_s_at" )[matrix.getColIndexByName( "GSE1299_bioMaterial_1" )];
        assertEquals( 7.62, k, 0.0001 );

        k = matrix.getRowByName( "224501_at" )[matrix.getColIndexByName( "GSE1299_bioMaterial_5" )];
        assertEquals( 7.88, k, 0.00001 );

        // ///////////////////////////////////
        // / now for the other platform // For the agilent array
        qtf.setName( "VALUE" );
        qtf.setScale( ScaleType.LOG2 );
        qtf.setRepresentation( PrimitiveType.DOUBLE );
        qtf.setGeneralType( GeneralType.QUANTITATIVE );
        qtf.setType( StandardQuantitationType.RATIO );
        qt = qts.find( qtf );

        assertTrue( qt != null );
        assertTrue( ee != null );
        assertTrue( newee.equals( ee ) );

        matrix = edms.getMatrix( newee, qt );

        if ( log.isDebugEnabled() ) {
            printMatrix( matrix );
        }

        assertTrue( matrix != null );

        assertEquals( 17, matrix.rows() );

        assertEquals( 4, matrix.columns() );

        k = matrix.getRowByName( "885" )[matrix.getColIndexByName( "GSE1299_bioMaterial_4" )];
        assertEquals( -0.1202943, k, 0.00001 );

        k = matrix.getRowByName( "878" )[matrix.getColIndexByName( "GSE1299_bioMaterial_2" )];
        assertEquals( 0.6135323, k, 0.00001 );

    }

    /**
     * @param matrix
     */
    @SuppressWarnings( { "unchecked", "unused" })
    private void printMatrix( DoubleMatrixNamed matrix ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "probe" );
        for ( String columnName : ( Collection<String> ) matrix.getColNames() ) {
            buf.append( "\t" + columnName );
        }
        buf.append( "\n" );
        for ( String rowName : ( Collection<String> ) matrix.getRowNames() ) {
            buf.append( rowName );
            double[] array = matrix.getRowByName( rowName );
            for ( int i = 0; i < array.length; i++ ) {
                double array_element = array[i];
                buf.append( "\t" + array_element );
            }
            buf.append( "\n" );
        }
        log.debug( buf.toString() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.AbstractGeoServiceTest#init()
     */
    @Override
    protected void init() {
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
    }

}

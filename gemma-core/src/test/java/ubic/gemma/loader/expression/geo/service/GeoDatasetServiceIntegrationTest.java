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

import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.ExpressionDataMatrixService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import baseCode.dataStructure.matrix.DoubleMatrixNamed;

/**
 * This is an integration test
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetServiceIntegrationTest extends AbstractGeoServiceTest {

    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        geoService = new GeoDatasetService();
        geoService.setLoadPlatformOnly( false );
        super.init();
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
        assert config != null;
        String path = getTestFileBasePath();
        geoService.setGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT + "gds775short" ) );
        geoService.fetchAndLoad( "GDS775" );
        log.info( "**** done ****" );
    }

    /**
     * problem case GDS22 - has lots of missing values and number format issues.
     */
    public void testFetchAndLoadGDS22() throws Exception {
        assert config != null;
        String path = getTestFileBasePath();
        geoService.setGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT + "gds22Short" ) );
        geoService.fetchAndLoad( "GDS22" );
        log.info( "**** done ****" );
    }

    /*
     * Data Access Failure edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl; nested exception is
     * org.hibernate.TransientObjectException: edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl
     */
    public void testFetchAndLoadGDS994() throws Exception {
        assert config != null;
        String path = getTestFileBasePath();
        geoService.setGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT + "gds994Short" ) );
        geoService.fetchAndLoad( "GDS994" );
        log.info( "**** done ****" );
    }

    //

    @SuppressWarnings("unchecked")
    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        // this.setFlushModeCommit();
        // this.setComplete();
        assert config != null;
        String path = getTestFileBasePath();
        geoService.setGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT + "shortTest" ) );

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

        ExpressionExperiment ee = ees.findByName( "Normal Muscle - Female , Effect of Age" );
        QuantitationType qtf = QuantitationType.Factory.newInstance();
        qtf.setName( "VALUE" );
        qtf.setRepresentation( PrimitiveType.DOUBLE );
        qtf.setGeneralType( GeneralType.QUANTITATIVE );
        qtf.setType( StandardQuantitationType.MEASUREDSIGNAL );
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
        assert config != null;
        String path = getTestFileBasePath();
        geoService.setGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT + "complexShortTest" ) );
        ExpressionExperiment newee = ( ExpressionExperiment ) geoService.fetchAndLoad( "GDS825" );

        // get the data back out.
        ExpressionExperimentService ees = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        QuantitationTypeService qts = ( QuantitationTypeService ) getBean( "quantitationTypeService" );

        ExpressionDataMatrixService edms = new ExpressionDataMatrixService();

        edms
                .setDesignElementDataVectorService( ( DesignElementDataVectorService ) getBean( "designElementDataVectorService" ) );

        ExpressionExperiment ee = ees.findByName( "Breast Cancer Cell Line Experiment" );
        QuantitationType qtf = QuantitationType.Factory.newInstance();

        // For the agilent array
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

        if ( log.isDebugEnabled() ) {
            printMatrix( matrix );
        }

        assertTrue( matrix != null );

        assertEquals( 116, matrix.rows() );

        assertEquals( 6, matrix.columns() );

        // GSM21252 on GDS823 (GPL97)
        double k = matrix.getRowByName( "224444_s_at" )[matrix.getColIndexByName( "GSE1299_bioMaterial_1" )];
        assertEquals( 7.62, k, 0.0001 );

        k = matrix.getRowByName( "224501_at" )[matrix.getColIndexByName( "GSE1299_bioMaterial_5" )];
        assertEquals( 7.88, k, 0.00001 );

        // ///////////////////////////////////
        // / now for the other platform
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

}

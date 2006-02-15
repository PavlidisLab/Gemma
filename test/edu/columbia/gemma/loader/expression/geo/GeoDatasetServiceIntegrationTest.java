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
package edu.columbia.gemma.loader.expression.geo;

import java.io.IOException;
import java.util.Collection;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.quantitationtype.GeneralType;
import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeService;
import edu.columbia.gemma.common.quantitationtype.StandardQuantitationType;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorService;
import edu.columbia.gemma.expression.bioAssayData.ExpressionDataMatrixService;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * This is an integration test
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetServiceIntegrationTest extends BaseServiceTestCase {
    GeoDatasetService gds;

    /**
     * 
     */
    public GeoDatasetServiceIntegrationTest() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gds = new GeoDatasetService();

        GeoConverter geoConv = new GeoConverter();

        gds.setPersister( getPersisterHelper() );
        gds.setConverter( geoConv );
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // public void testFetchAndLoadMultiChipPerSeries() throws Exception {
    // gds.fetchAndLoad( "GDS472" ); // HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
    // // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
    // }

    // /**
    // * This test uses just one dataset, one series
    // */
    // public void testFetchAndLoadOneDataset() throws Exception {
    // gds.fetchAndLoad( "GDS599" );
    // }
    //
    // /**
    // * This test uses all three MG-U74 arrays.
    // */
    // public void testFetchAndLoadThreePlatforms() throws Exception {
    // gds.fetchAndLoad( "GDS243" );
    // }

    @SuppressWarnings("unchecked")
    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        assert config != null;
        String path = config.getString( "gemma.home" );
        if ( path == null ) {
            throw new IOException( "You must define the 'gemma.home' variable in your build.properties file" );
        }
        gds.setGenerator( new GeoDomainObjectGeneratorLocal( path + "/test/data/geo/shortTest" ) );
      //  gds.fetchAndLoad( "GDS472" ); // HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search

        // get the data back out.
        ExpressionExperimentService ees = ( ExpressionExperimentService ) BaseDAOTestCase.ctx
                .getBean( "expressionExperimentService" );
        QuantitationTypeService qts = ( QuantitationTypeService ) BaseDAOTestCase.ctx
                .getBean( "quantitationTypeService" );

        ExpressionDataMatrixService edms = new ExpressionDataMatrixService();

        edms.setDesignElementDataVectorService( ( DesignElementDataVectorService ) BaseDAOTestCase.ctx
                .getBean( "designElementDataVectorService" ) );

        ExpressionExperiment ee = ees.findByName( "Normal Muscle - Female , Effect of Age" );
        QuantitationType qtf = QuantitationType.Factory.newInstance();
        qtf.setName( "VALUE" );
        qtf.setRepresentation( PrimitiveType.DOUBLE );
        qtf.setGeneralType( GeneralType.QUANTITATIVE );
        qtf.setType( StandardQuantitationType.MEASUREDSIGNAL );
        QuantitationType qt = qts.find( qtf );

        DoubleMatrixNamed matrix = edms.getMatrix( ee, qt );

        printMatrix( matrix );

        assertEquals( 31, matrix.rows() );

        assertEquals( 15, matrix.columns() );

        double k = matrix.getRowByName( "200000_s_at" )[matrix.getColIndexByName( "GSE674_bioMaterial_14" )];
        assertEquals( 6357.0, k, 0.00001 );

        k = matrix.getRowByName( "1007_s_at" )[matrix.getColIndexByName( "GSE674_bioMaterial_14" )];
        assertEquals( 1558.0, k, 0.00001 );

    }

    // public void testFetchAndLoadWithRawData() throws Exception {
    // gds.fetchAndLoad( "GDS562" );
    // }
    //
    // public void testFetchAndLoadB() throws Exception {
    // gds.fetchAndLoad( "GDS942" );
    // }
    //
    // public void testFetchAndLoadC() throws Exception {
    // gds.fetchAndLoad( "GDS100" );
    // }
    //
    // public void testFetchAndLoadD() throws Exception {
    // gds.fetchAndLoad( "GDS1033" );
    // }
    //
    // public void testFetchAndLoadE() throws Exception {
    // gds.fetchAndLoad( "GDS835" );
    // }
    //
    // public void testFetchAndLoadF() throws Exception {
    // gds.fetchAndLoad( "GDS58" );
    // }

    /**
     * @param matrix
     */
    @SuppressWarnings( { "unchecked", "unused" })
    private void printMatrix( DoubleMatrixNamed matrix ) {
        System.err.print( "probe" );
        for ( String columnName : ( Collection<String> ) matrix.getColNames() ) {
            System.err.print( "\t" + columnName );
        }
        System.err.print( "\n" );
        for ( String rowName : ( Collection<String> ) matrix.getRowNames() ) {
            System.err.print( rowName );
            double[] array = matrix.getRowByName( rowName );
            for ( int i = 0; i < array.length; i++ ) {
                double array_element = array[i];
                System.err.print( "\t" + array_element );
            }
            System.err.print( "\n" );
        }
    }

}

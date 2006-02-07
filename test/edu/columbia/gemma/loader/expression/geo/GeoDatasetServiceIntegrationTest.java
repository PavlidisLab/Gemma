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

import baseCode.io.ByteArrayConverter;
import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.quantitationtype.GeneralType;
import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.common.quantitationtype.QuantitationTypeService;
import edu.columbia.gemma.common.quantitationtype.StandardQuantitationType;
import edu.columbia.gemma.expression.bioAssayData.BioAssayDimension;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVectorService;
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

    @SuppressWarnings("unchecked")
    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        assert config != null;
        String path = config.getString( "gemma.home" );
        if ( path == null ) {
            throw new IOException( "You must define the 'gemma.home' variable in your build.properties file" );
        }
        gds.setGenerator( new GeoDomainObjectGeneratorLocal( path + "/test/data/geo/shortTest" ) );
        gds.fetchAndLoad( "GDS472" ); // HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search

        // get the data back out.

        ExpressionExperimentService ees = ( ExpressionExperimentService ) BaseDAOTestCase.ctx
                .getBean( "expressionExperimentService" );
        QuantitationTypeService qts = ( QuantitationTypeService ) BaseDAOTestCase.ctx
                .getBean( "quantitationTypeService" );

        ExpressionExperiment ee = ees.findByName( "Normal Muscle - Female , Effect of Age" );
        QuantitationType qtf = QuantitationType.Factory.newInstance();
        qtf.setName( "VALUE" );
        qtf.setRepresentation( PrimitiveType.DOUBLE );
        qtf.setGeneralType( GeneralType.QUANTITATIVE );
        qtf.setType( StandardQuantitationType.MEASUREDSIGNAL );
        QuantitationType qt = qts.find( qtf );

        DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) BaseDAOTestCase.ctx
                .getBean( "designElementDataVectorService" );

        ByteArrayConverter bac = new ByteArrayConverter();

        Collection<DesignElementDataVector> co = dedvs.findAllForMatrix( ee, qt );
        // assertEquals( 20, co.size() );
        for ( DesignElementDataVector dedv : co ) {
            BioAssayDimension bad = dedv.getBioAssayDimension();
            byte[] bytes = dedv.getData();
            log.info( "Read " + bytes.length + " bytes" );
            double[] vals = bac.byteArrayToDoubles( bytes );
            log.info( bad.getName().substring( 0, 20 ) + "... Elements: " + vals.length );
  //          assertEquals( bad.getDimensionBioAssays().size(), vals.length );
        }
    }

    /*
     * public void testFetchAndLoadWithRawData() throws Exception { gds.fetchAndLoad( "GDS562" ); } public void
     * testFetchAndLoadB() throws Exception { gds.fetchAndLoad( "GDS942" ); } public void testFetchAndLoadC() throws
     * Exception { gds.fetchAndLoad( "GDS100" ); } public void testFetchAndLoadD() throws Exception { gds.fetchAndLoad(
     * "GDS1033" ); } public void testFetchAndLoadE() throws Exception { gds.fetchAndLoad( "GDS835" ); } public void
     * testFetchAndLoadF() throws Exception { gds.fetchAndLoad( "GDS58" ); }
     */
}

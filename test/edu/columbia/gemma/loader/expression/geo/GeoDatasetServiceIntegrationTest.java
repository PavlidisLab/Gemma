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

import edu.columbia.gemma.BaseServiceTestCase;

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

    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        assert config != null;
        String path = config.getString( "gemma.home" );
        if ( path == null ) {
            throw new IOException( "You must define the 'gemma.home' variable in your build.properties file" );
        }
        gds.setGenerator( new GeoDomainObjectGeneratorLocal( path + "/test/data/geo/shortTest" ) );
        gds.fetchAndLoad( "GDS472" ); // HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
    } /*
         * public void testFetchAndLoadWithRawData() throws Exception { gds.fetchAndLoad( "GDS562" ); } public void
         * testFetchAndLoadB() throws Exception { gds.fetchAndLoad( "GDS942" ); } public void testFetchAndLoadC() throws
         * Exception { gds.fetchAndLoad( "GDS100" ); } public void testFetchAndLoadD() throws Exception {
         * gds.fetchAndLoad( "GDS1033" ); } public void testFetchAndLoadE() throws Exception { gds.fetchAndLoad(
         * "GDS835" ); } public void testFetchAndLoadF() throws Exception { gds.fetchAndLoad( "GDS58" ); }
         */
}

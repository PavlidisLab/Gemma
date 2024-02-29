/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.loader.expression.geo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;


/**
 * @author pavlidis
 */
@Category(GeoTest.class)
public class GeoBrowserTest {

    private static final Log log = LogFactory.getLog( GeoBrowserTest.class );

    @BeforeClass
    public static void checkThatGeoIsAvailable() throws Exception {
        assumeThatResourceIsAvailable( "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi" );
    }

    @Test
    public void testGetRecentGeoRecords() throws Exception {
        GeoBrowser b = new GeoBrowser();
        Collection<GeoRecord> res = b.getRecentGeoRecords( 10, 10 );
        assertTrue( res.size() > 0 );
    }

    @Test
    public void testGetGeoRecordsBySearchTerm() {
        GeoBrowser b = new GeoBrowser();

        Collection<GeoRecord> res;
        try {
            res = b.getGeoRecordsBySearchTerm( "Homo+sapiens[orgn]", 10, 10, false, null, null );
        } catch ( IOException e ) {
            assumeNoException( e );
            return;
        }
        // Check that the search has returned at least one record
        assertTrue( res.size() > 0 );

        // Print out accession numbers etc.; check that the records returned match the search term
        for ( GeoRecord record : res ) {
            log.info( "Accession: " + record.getGeoAccession() );
            log.info( "Title : " + record.getTitle() );
            log.info( "Number of samples: " + record.getNumSamples() );
            log.info( "Date: " + record.getReleaseDate() );
            log.info( "Platform: " + record.getPlatform() );
            assertTrue( record.getOrganisms().contains( "Homo sapiens" ) );
        }
    }


    /**
     * Exercises getting details
     *
     */
    @Test
    @Category(SlowTest.class)
    public void testGetGeoRecords() {
        GeoBrowser b = new GeoBrowser();
        Collection<GeoRecord> res;
        try {
            res = b.getGeoRecordsBySearchTerm( null, 10, 10, true, null, null );
        } catch ( IOException e ) {
            assumeNoException( e );
            return;
        }
        // Check that the search has returned at least one record
        assertTrue( res.size() > 0 );

        // Print out accession numbers etc.; check that the records returned match the search term
        for ( GeoRecord record : res ) {
            log.info( "Accession: " + record.getGeoAccession() );
            log.info( "Title : " + record.getTitle() );
            log.info( "Number of samples: " + record.getNumSamples() );
            log.info( "Date: " + record.getReleaseDate() );
            log.info( "Platforms: " + record.getPlatform() );
        }
    }


    @Test
    public void testGetGeoRecordsB() {
        GeoBrowser b = new GeoBrowser();
        Collection<GeoRecord> geoRecords;
        try {
            geoRecords = b.getGeoRecords( Arrays.asList( "GSE1", "GSE2", "GSE3" ) );
        } catch ( IOException e ) {
            assumeNoException( e );
            return;
        }
        assertEquals( 3, geoRecords.size() );
    }

    @Test
    public void testGetGeoRecordGSE93825() {
        GeoBrowser b = new GeoBrowser();

        Collection<GeoRecord> res;
        try {
            res = b.getGeoRecordsBySearchTerm( "GSE93825[acc]", 0, 10, false, null, null );
        } catch ( IOException e ) {
            assumeNoException( e );
            return;
        }
        // Check that the search has returned at least one record
        assertTrue( res.size() > 0 );

        // Print out accession numbers etc.; check that the records returned match the search term
        for ( GeoRecord record : res ) {
            log.info( "Accession: " + record.getGeoAccession() );
            log.info( "Title : " + record.getTitle() );
            log.info( "Number of samples: " + record.getNumSamples() );
            log.info( "Date: " + record.getReleaseDate() );
            log.info( "Platform: " + record.getPlatform() );
            log.info( "Pubmed: " + record.getPubMedIds() );
            assertTrue( record.getOrganisms().contains( "Homo sapiens" ) );
        }
    }

    /**
     * GEO returns an empty document when retrieving the samples for this document.
     */
    @Test
    @Category(SlowTest.class)
    public void testGeoEmptyMINiML() throws IOException {
        GeoBrowser b = new GeoBrowser();
        b.getGeoRecordsBySearchTerm( "GSE127242", 0, 10, true, null, null );
    }


    /* Make the method public to run this test */
    //    @Test
    //    public void testGetTaxonCollection() throws Exception {
    //    	GeoBrowser b = new GeoBrowser();
    //    	Collection<String> oneTaxon = b.getTaxonCollection( "Homo sapiens" );
    //    	assertTrue(oneTaxon.size() == 1);
    //    	Collection<String> twoTaxa = b.getTaxonCollection( "Homo sapiens; Mus musculus" );
    //    	assertTrue(twoTaxa.size() == 2);
    //    	assertTrue(twoTaxa.contains( "Homo sapiens" ));
    //    	assertTrue(twoTaxa.contains( "Mus musculus" ));
    //    }

}

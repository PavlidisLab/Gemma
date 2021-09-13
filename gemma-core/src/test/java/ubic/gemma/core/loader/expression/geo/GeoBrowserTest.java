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
import org.junit.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * @author pavlidis
 */
public class GeoBrowserTest {

    private static final Log log = LogFactory.getLog( GeoBrowserTest.class );

    @Test
    public void testGetRecentGeoRecords() throws Exception {
        GeoBrowser b = new GeoBrowser();

        try {
            Thread.sleep( 200 );

            Collection<GeoRecord> res = b.getRecentGeoRecords( 10, 10 );
            assertTrue( res.size() > 0 );
        } catch ( IOException e ) {
            if ( e.getMessage().contains( "GEO returned an error" ) ) {
                GeoBrowserTest.log.warn( "GEO returned an error, skipping test." );
                return;
            }
            throw e;
        }
    }

    @Test
    public void testGetGeoRecordsBySearchTerm() throws Exception {
        GeoBrowser b = new GeoBrowser();

        try {

            Thread.sleep( 200 );

            Collection<GeoRecord> res = b.getGeoRecordsBySearchTerm( "Homo+sapiens[orgn]", 10, 10, false, null, null );
            // Check that the search has returned at least one record
            assertTrue( res.size() > 0 );

            // Print out accession numbers etc.; check that the records returned match the search term
            for ( GeoRecord record : res ) {
                System.out.println( "Accession: " + record.getGeoAccession() );
                System.out.println( "Title : " + record.getTitle() );
                System.out.println( "Number of samples: " + record.getNumSamples() );
                System.out.println( "Date: " + record.getReleaseDate() );
                System.out.println( "Platform: " + record.getPlatform() );
                assertTrue( record.getOrganisms().contains( "Homo sapiens" ) );
            }

        } catch ( IOException e ) {
            if ( e.getMessage().contains( "GEO returned an error" ) ) {
                GeoBrowserTest.log.warn( "GEO returned an error, skipping test." );
                return;
            }
            throw e;
        }
    }

    /**
     * Exercises getting details
     *
     * @throws Exception
     */
    @Test
    public void testGetGeoRecords() throws Exception {
        GeoBrowser b = new GeoBrowser();

        try {
            Thread.sleep( 200 );

            Collection<GeoRecord> res = b.getGeoRecordsBySearchTerm( null, 10, 10, true, null, null );
            // Check that the search has returned at least one record
            assertTrue( res.size() > 0 );

            // Print out accession numbers etc.; check that the records returned match the search term
            for ( GeoRecord record : res ) {
                System.out.println( "Accession: " + record.getGeoAccession() );
                System.out.println( "Title : " + record.getTitle() );
                System.out.println( "Number of samples: " + record.getNumSamples() );
                System.out.println( "Date: " + record.getReleaseDate() );
                System.out.println( "Platforms: " + record.getPlatform() );
            }

        } catch ( IOException e ) {
            if ( e.getMessage().contains( "GEO returned an error" ) ) {
                GeoBrowserTest.log.warn( "GEO returned an error, skipping test." );
                return;
            }
            throw e;
        }
    }


    @Test
    public void testGetGeoRecordsB() throws Exception {
        GeoBrowser b = new GeoBrowser();
        try {
            Collection<GeoRecord> geoRecords = b.getGeoRecords( Arrays.asList( new String[] { "GSE1", "GSE2", "GSE3" } ) );
            assertEquals( 3, geoRecords.size() );
        } catch ( IOException e ) {
            if ( e.getMessage().contains( "GEO returned an error" ) ) {
                GeoBrowserTest.log.warn( "GEO returned an error, skipping test." );
                return;
            }
            throw e;
        }
    }

    @Test
    public void testGetGeoRecordGSE93825() throws Exception {
        GeoBrowser b = new GeoBrowser();

        try {

            Thread.sleep( 200 );

            Collection<GeoRecord> res = b.getGeoRecordsBySearchTerm( "GSE93825[acc]", 0, 10, false, null, null );
            // Check that the search has returned at least one record
            assertTrue( res.size() > 0 );

            // Print out accession numbers etc.; check that the records returned match the search term
            for ( GeoRecord record : res ) {
                System.out.println( "Accession: " + record.getGeoAccession() );
                System.out.println( "Title : " + record.getTitle() );
                System.out.println( "Number of samples: " + record.getNumSamples() );
                System.out.println( "Date: " + record.getReleaseDate() );
                System.out.println( "Platform: " + record.getPlatform() );
                System.out.println( "Pubmed: " + record.getPubMedIds() );
                assertTrue( record.getOrganisms().contains( "Homo sapiens" ) );
            }

        } catch ( IOException e ) {
            if ( e.getMessage().contains( "GEO returned an error" ) ) {
                GeoBrowserTest.log.warn( "GEO returned an error, skipping test." );
                return;
            }
            throw e;
        }
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

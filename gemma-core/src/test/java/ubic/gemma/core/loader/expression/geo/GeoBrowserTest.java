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
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoSearchField;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;


/**
 * @author pavlidis
 */
@Category(GeoTest.class)
public class GeoBrowserTest {

    private static final Log log = LogFactory.getLog( GeoBrowserTest.class );

    private static final String ncbiApiKey = Settings.getString( "entrez.efetch.apikey" );

    @BeforeClass
    public static void checkThatGeoIsAvailable() throws Exception {
        assumeThatResourceIsAvailable( "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi" );
    }

    @Test
    public void testGetRecentGeoRecords() throws Exception {
        assumeThatResourceIsAvailable( "https://www.ncbi.nlm.nih.gov/geo/browse/" );
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
        Collection<GeoRecord> res = b.getRecentGeoRecords( 10, 10 );
        assertFalse( res.isEmpty() );
    }

    @Test
    public void testSearchGeoRecords() {
        GeoBrowser b = new GeoBrowser( ncbiApiKey );

        Collection<GeoRecord> res;
        try {
            res = b.searchGeoRecords( "Homo sapiens", GeoSearchField.ORGANISM, null, null, null, 10, 10, false );
        } catch ( IOException e ) {
            assumeNoException( e );
            return;
        }
        // Check that the search has returned at least one record
        assertFalse( res.isEmpty() );

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
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
        Collection<GeoRecord> res;
        try {
            res = b.searchGeoRecords( null, null, null, null, null, 10, 10, true );
        } catch ( IOException e ) {
            assumeNoException( e );
            return;
        }
        // Check that the search has returned at least one record
        assertFalse( res.isEmpty() );

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
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
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
        GeoBrowser b = new GeoBrowser( ncbiApiKey );

        Collection<GeoRecord> res;
        try {
            res = b.searchGeoRecords( "GSE93825", GeoSearchField.ACCESSION, null, null, null, 0, 10, false );
        } catch ( IOException e ) {
            assumeNoException( e );
            return;
        }
        // Check that the search has returned at least one record
        assertFalse( res.isEmpty() );

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
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
        b.searchGeoRecords( "GSE127242", null, null, null, null, 0, 10, true );
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

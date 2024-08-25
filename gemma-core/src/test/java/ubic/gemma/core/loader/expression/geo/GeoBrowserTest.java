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

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;


/**
 * @author pavlidis
 */
@Category(GeoTest.class)
public class GeoBrowserTest {

    private static final Log log = LogFactory.getLog( GeoBrowserTest.class );

    private static final String ncbiApiKey = Settings.getString( "entrez.efetch.apikey" );

    GeoBrowser b = new GeoBrowser( ncbiApiKey );

    @BeforeClass
    public static void checkThatGeoIsAvailable() throws Exception {
        assumeThatResourceIsAvailable( "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi" );
    }

    @Test
    public void testGetRecentGeoRecords() throws Exception {
        assumeThatResourceIsAvailable( "https://www.ncbi.nlm.nih.gov/geo/browse/" );
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
        Collection<GeoRecord> res = b.getRecentGeoRecords( 10, 10 );
        assertThat( res )
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo( 10 );
    }

    @Test
    public void testSearchGeoRecords() throws IOException {
        Collection<GeoRecord> res = b.searchGeoRecords( "Homo sapiens", GeoSearchField.ORGANISM, null, null, null, 10, 10, false );
        // Check that the search has returned at least one record
        assertThat( res )
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo( 10 )
                .allSatisfy( record -> {
                    // Print out accession numbers etc.; check that the records returned match the search term
                    log.info( "Accession: " + record.getGeoAccession() );
                    log.info( "Title : " + record.getTitle() );
                    log.info( "Number of samples: " + record.getNumSamples() );
                    log.info( "Date: " + record.getReleaseDate() );
                    log.info( "Platform: " + record.getPlatform() );
                    assertThat( record.getOrganisms() ).contains( "Homo sapiens" );
                } );
    }


    /**
     * Exercises getting details
     *
     */
    @Test
    @Category(SlowTest.class)
    public void testGetGeoRecords() throws IOException {
        Collection<GeoRecord> res = b.searchGeoRecords( null, null, null, null, null, 10, 10, true );
        // Check that the search has returned at least one record
        assertThat( res )
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo( 10 )
                .allSatisfy( record -> {
                    // Print out accession numbers etc.; check that the records returned match the search term
                    log.info( "Accession: " + record.getGeoAccession() );
                    log.info( "Title : " + record.getTitle() );
                    log.info( "Number of samples: " + record.getNumSamples() );
                    log.info( "Date: " + record.getReleaseDate() );
                    log.info( "Platforms: " + record.getPlatform() );
                } );
    }


    @Test
    public void testGetGeoRecordsB() throws IOException {
        assertThat( b.getGeoRecords( Arrays.asList( "GSE1", "GSE2", "GSE3" ), false ) ).hasSize( 3 );
    }

    @Test
    public void testGetGeoRecordGSE93825() throws IOException {
        Collection<GeoRecord> res = b.searchGeoRecords( "GSE93825", GeoSearchField.ACCESSION, null, null, null, 0, 10, false );
        // Check that the search has returned at least one record
        assertThat( res ).isNotEmpty()
                .allSatisfy( record -> {
                    // Print out accession numbers etc.; check that the records returned match the search term
                    log.info( "Accession: " + record.getGeoAccession() );
                    log.info( "Title : " + record.getTitle() );
                    log.info( "Number of samples: " + record.getNumSamples() );
                    log.info( "Date: " + record.getReleaseDate() );
                    log.info( "Platform: " + record.getPlatform() );
                    log.info( "Pubmed: " + record.getPubMedIds() );
                    assertThat( record.getOrganisms() ).contains( "Homo sapiens" );
                } );
    }

    /**
     * GEO returns an empty document when retrieving the samples for this document.
     */
    @Test
    public void testGeoEmptyMINiML() throws IOException {
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
        b.searchGeoRecords( "GSE127242", null, null, null, null, 0, 10, true );
    }

    /**
     * This dataset has MESH headings.
     */
    @Test
    @Category(SlowTest.class)
    public void testGetGeoRecordWithMeshHeadings() throws IOException {
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
        assertThat( b.getGeoRecord( "GSE171541", true ) )
                .satisfies( record -> {
                    assertThat( record.getPubMedIds() ).containsExactly( "36539833" );
                    assertThat( record.getNumSamples() ).isEqualTo( 9 );
                    assertThat( record.getMeshHeadings() ).hasSize( 7 );
                } );
    }

    @Test
    public void testSearchGeoRecordWithMeshHeadings() throws IOException {
        GeoBrowser b = new GeoBrowser( ncbiApiKey );
        assertThat( b.searchGeoRecords( "GSE171541", GeoSearchField.ACCESSION, null, null, null, 0, 10, true ) )
                .singleElement()
                .satisfies( record -> {
                    assertThat( record.getPubMedIds() ).containsExactly( "36539833" );
                    assertThat( record.getNumSamples() ).isEqualTo( 9 );
                    assertThat( record.getMeshHeadings() ).hasSize( 7 );
                } );
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

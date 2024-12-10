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
import org.xml.sax.SAXParseException;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;


/**
 * @author pavlidis
 */
@Category(GeoTest.class)
public class GeoBrowserTest {

    private static final Log log = LogFactory.getLog( GeoBrowserTest.class );

    private static final String ncbiApiKey = Settings.getString( "entrez.efetch.apikey" );

    private final GeoBrowser b = new GeoBrowserImpl( ncbiApiKey );

    @BeforeClass
    public static void checkThatGeoIsAvailable() throws Exception {
        assumeThatResourceIsAvailable( "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi" );
    }

    @Test
    public void testGetRecentGeoRecords() throws Exception {
        assumeThatResourceIsAvailable( "https://www.ncbi.nlm.nih.gov/geo/browse/" );
        GeoBrowser b = new GeoBrowserImpl( ncbiApiKey );
        Collection<GeoRecord> res = b.getRecentGeoRecords( GeoRecordType.SERIES, 10, 10 );
        assertThat( res )
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo( 10 );
    }

    @Test
    @Category(SlowTest.class)
    public void testGetDetailedGeoRecord() throws IOException {
        GeoBrowser b = new GeoBrowserImpl( ncbiApiKey );
        b.getGeoRecord( GeoRecordType.SERIES, "GSE1", GeoRetrieveConfig.DETAILED );
        b.getGeoRecord( GeoRecordType.SERIES, "GSE999", GeoRetrieveConfig.DETAILED );
        b.getGeoRecord( GeoRecordType.SERIES, "GSE1000", GeoRetrieveConfig.DETAILED );
    }

    @Test
    public void testSearchGeoRecords() throws IOException {
        GeoQuery query = b.searchGeoRecords( GeoRecordType.SERIES, "Homo sapiens", GeoSearchField.ORGANISM, null, null, null );
        assertThat( query.getQueryId() ).isNotNull();
        assertThat( query.getCookie() ).isNotNull();
        assertThat( query.getTotalRecords() ).isGreaterThan( 100000 );
        Collection<GeoRecord> res = b.retrieveGeoRecords( query, 10, 10 );
        // Check that the search has returned at least one record
        assertThat( res )
                .hasSize( 10 )
                .allSatisfy( record -> {
                    // Print out accession numbers etc.; check that the records returned match the search term
                    log.info( "Accession: " + record.getGeoAccession() );
                    log.info( "Title : " + record.getTitle() );
                    log.info( "Number of samples: " + record.getNumSamples() );
                    log.info( "Date: " + record.getReleaseDate() );
                    log.info( "Platform: " + record.getPlatform() );
                    assertThat( record.getOrganisms() ).contains( "Homo sapiens" );
                } );
        assertThat( b.retrieveGeoRecords( query, 0, 10 ) )
                .hasSize( 10 );
        assertThat( b.retrieveGeoRecords( query, 1000000000, 10 ) )
                .isEmpty();
    }


    /**
     * Exercises getting details
     *
     */
    @Test
    @Category(SlowTest.class)
    public void testRetrieveDetailedGeoRecords() throws IOException {
        GeoQuery query = b.searchGeoRecords( GeoRecordType.SERIES, null, null, null, null, null );
        // Check that the search has returned at least one record
        assertThat( b.retrieveGeoRecords( query, 0, 10, GeoRetrieveConfig.DETAILED ) )
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
    public void testGetGeoRecordB() throws IOException {
        assertThat( b.getGeoRecords( GeoRecordType.SERIES, Arrays.asList( "GSE1", "GSE2", "GSE3" ) ) ).hasSize( 3 );
    }

    @Test
    public void testGetGeoRecordGSE93825() throws IOException {
        // Check that the search has returned at least one record
        assertThat( b.getGeoRecord( GeoRecordType.SERIES, "GSE93825" ) ).isNotNull()
                .satisfies( record -> {
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
     * This GEO record has ~1000 samples with hundreds of characteristics each.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE97948() throws IOException {
        assertThat( b.getGeoRecord( GeoRecordType.SERIES, "GSE97948", GeoRetrieveConfig.builder()
                .subSeriesStatus( true )
                .libraryStrategy( true )
                .build() ) )
                .isNotNull();
    }

    /**
     * GEO returns an empty document when retrieving the samples for this document.
     */
    @Test
    public void testGeoEmptyMINiML() throws IOException {
        GeoBrowser b = new GeoBrowserImpl( ncbiApiKey );
        b.searchAndRetrieveGeoRecords( GeoRecordType.SERIES, "GSE127242", null, null, null, null, 0, 10, true );
    }

    /**
     * This dataset has MESH headings.
     */
    @Test
    @Category(SlowTest.class)
    public void testGetGeoRecordWithMeshHeadings() throws IOException {
        GeoBrowser b = new GeoBrowserImpl( ncbiApiKey );
        assertThat( b.getGeoRecord( GeoRecordType.SERIES, "GSE171541", GeoRetrieveConfig.DETAILED ) )
                .satisfies( record -> {
                    assertThat( record.getPubMedIds() ).containsExactly( "36539833" );
                    assertThat( record.getNumSamples() ).isEqualTo( 9 );
                    assertThat( record.getMeshHeadings() ).hasSize( 7 );
                } );
    }

    @Test
    public void testSearchGeoRecordWithMeshHeadings() throws IOException {
        GeoBrowser b = new GeoBrowserImpl( ncbiApiKey );
        assertThat( b.searchAndRetrieveGeoRecords( GeoRecordType.SERIES, "GSE171541", GeoSearchField.GEO_ACCESSION, null, null, null, 0, 10, true ) )
                .singleElement()
                .satisfies( record -> {
                    assertThat( record.getPubMedIds() ).containsExactly( "36539833" );
                    assertThat( record.getNumSamples() ).isEqualTo( 9 );
                    assertThat( record.getMeshHeadings() ).hasSize( 7 );
                } );
    }

    /**
     * This dataset has incorrect UTF-8 characters in its MINiML file.
     */
    @Test
    public void testGSE2569() {
        GeoBrowser b = new GeoBrowserImpl( ncbiApiKey );
        assertThatThrownBy( () -> b.getGeoRecord( GeoRecordType.SERIES, "GSE2569", GeoRetrieveConfig.DETAILED ) )
                .cause()
                .isInstanceOf( IOException.class )
                .cause()
                .isInstanceOf( SAXParseException.class )
                .hasMessage( "Invalid byte 1 of 1-byte UTF-8 sequence." );
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

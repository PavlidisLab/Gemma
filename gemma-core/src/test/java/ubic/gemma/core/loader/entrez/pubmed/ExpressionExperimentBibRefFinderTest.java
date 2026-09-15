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
package ubic.gemma.core.loader.entrez.pubmed;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Resolution is exercised against a stubbed {@link GeoBrowser} rather than live Entrez.
 * <p>
 * These assertions used to be driven by feeding SOFT text to a package-private parser, because the
 * finder scraped {@code acc.cgi} and had no seam. It now goes through {@code esearch}/{@code esummary}
 * via {@link GeoBrowser}, which is injectable, so the same behaviours — order, de-duplication, the
 * empty case — are pinned at the boundary the class actually has.
 *
 * @author pavlidis
 */
public class ExpressionExperimentBibRefFinderTest {

    private static final String ACCESSION = "GSE99114";

    private static ExpressionExperimentBibRefFinder finderReturning( String... pubMedIds ) throws IOException {
        GeoRecord record = new GeoRecord();
        record.setGeoAccession( ACCESSION );
        record.setPubMedIds( Arrays.asList( pubMedIds ) );
        GeoBrowser browser = mock( GeoBrowser.class );
        when( browser.getGeoRecord( eq( GeoRecordType.SERIES ), eq( ACCESSION ) ) ).thenReturn( record );
        return new ExpressionExperimentBibRefFinder( browser, null );
    }

    @Test
    public void testTheSeriesPubMedIdIsTheAuthoritativeLink() throws IOException {
        assertEquals( 38064339, finderReturning( "38064339" ).locatePubMedId( ACCESSION ) );
    }

    @Test
    public void testReturnsFirstWhenMultiple() throws IOException {
        // multi-paper series: the first is taken as primary (curator confirms), the rest logged
        assertEquals( 38064339, finderReturning( "38064339", "30255127" ).locatePubMedId( ACCESSION ) );
    }

    /**
     * A series listing two papers has a primary and a follow-up, and the second one has somewhere to
     * go: the other-relevant slot. The single-id form counted them and warned "a curator should
     * confirm which is primary", which described work no caller could act on because the id was
     * already gone by the time the warning was written.
     */
    @Test
    public void testEveryPubMedIdIsReadableNotJustTheFirst() throws IOException {
        ExpressionExperimentBibRefFinder finder = finderReturning( "38064339", "30255127" );
        assertEquals( Arrays.asList( 38064339, 30255127 ), finder.locatePubMedIds( ACCESSION ) );
        // the single-id form still answers exactly as it did
        assertEquals( 38064339, finder.locatePubMedId( ACCESSION ) );
    }

    @Test
    public void testARepeatedIdIsOnePaper() throws IOException {
        // GEO does list the same id more than once in a record; that is one paper, not two
        assertEquals( Collections.singletonList( 38064339 ),
                finderReturning( "38064339", "38064339" ).locatePubMedIds( ACCESSION ) );
    }

    @Test
    public void testANonNumericIdIsSkippedRatherThanFatal() throws IOException {
        assertEquals( Collections.singletonList( 38064339 ),
                finderReturning( "not-a-pmid", "38064339" ).locatePubMedIds( ACCESSION ) );
    }

    @Test
    public void testNoPublicationIsAnEmptyListNotAZero() throws IOException {
        assertTrue( finderReturning().locatePubMedIds( ACCESSION ).isEmpty() );
    }

    @Test
    public void testNoPublicationIsMinusOneForTheSingleIdForm() throws IOException {
        // a series with no linked paper yields -1
        assertEquals( -1, finderReturning().locatePubMedId( ACCESSION ) );
    }

    @Test
    public void testANonSeriesAccessionIsEmpty() throws IOException {
        GeoBrowser browser = mock( GeoBrowser.class );
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder( browser, null );
        assertTrue( finder.locatePubMedIds( "GPL1234" ).isEmpty() );
        assertEquals( -1, finder.locatePubMedId( "GDS1234" ) );
    }

    /**
     * The distinction the reCAPTCHA incident turned on. GEO being unreadable must not arrive as an
     * empty list, because callers write "GEO states no publication" findings off an empty list —
     * {@code VerifyPublicationEvidenceCli} records {@code geo_states_none} with a dated evidence
     * string. An unknown answer throws instead, so no finding can be built from it.
     */
    @Test
    public void testAnUnreadableGeoThrowsRatherThanLookingLikeNoPublication() throws IOException {
        GeoBrowser browser = mock( GeoBrowser.class );
        when( browser.getGeoRecord( eq( GeoRecordType.SERIES ), eq( ACCESSION ) ) )
                .thenThrow( new IOException( "GEO served an HTML challenge page" ) );
        assertThatThrownBy( () -> new ExpressionExperimentBibRefFinder( browser, null ).locatePubMedIds( ACCESSION ) )
                .isInstanceOf( IOException.class );
    }

    /**
     * Likewise for an accession GEO has no record of: we have learned nothing about its publications,
     * which is not the same as learning that it has none.
     */
    @Test
    public void testAnAbsentRecordThrowsRatherThanLookingLikeNoPublication() throws IOException {
        GeoBrowser browser = mock( GeoBrowser.class );
        when( browser.getGeoRecord( eq( GeoRecordType.SERIES ), any( String.class ) ) ).thenReturn( null );
        assertThatThrownBy( () -> new ExpressionExperimentBibRefFinder( browser, null ).locatePubMedIds( ACCESSION ) )
                .isInstanceOf( IOException.class )
                .hasMessageContaining( ACCESSION );
    }

    /**
     * A record that carries no PubMed list at all is GEO stating none, not a failure.
     */
    @Test
    public void testARecordWithNoPubMedListIsEmpty() throws IOException {
        GeoRecord record = new GeoRecord();
        record.setGeoAccession( ACCESSION );
        GeoBrowser browser = mock( GeoBrowser.class );
        when( browser.getGeoRecord( eq( GeoRecordType.SERIES ), eq( ACCESSION ) ) ).thenReturn( record );
        List<Integer> ids = new ExpressionExperimentBibRefFinder( browser, null ).locatePubMedIds( ACCESSION );
        assertThat( ids ).isEmpty();
    }
}

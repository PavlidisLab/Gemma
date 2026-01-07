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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;
import ubic.gemma.core.util.test.category.PubMedTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.BibliographicReference;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
@Category(PubMedTest.class)
@NetworkAvailable(url = EntrezUtils.ESEARCH)
public class PubMedSearchTest {

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    private final PubMedSearch pms = new PubMedSearch( Settings.getString( "entrez.efetch.apikey" ) );

    @Test
    public void testSearchAndRetrieveByHTTP() throws Exception {
        Collection<String> searchTerms = new HashSet<>();
        searchTerms.add( "brain" );
        searchTerms.add( "hippocampus" );
        searchTerms.add( "habenula" );
        searchTerms.add( "glucose" );
        Collection<BibliographicReference> actualResult = pms.searchAndRetrieve( StringUtils.join( " ", searchTerms ), 100 );
        assertTrue( "Expected at least 5 results, got " + actualResult.size(), actualResult.size() >= 5 );
        /*
         * at least, this was the result on 4/2008.
         */
        BibliographicReference r = actualResult.iterator().next();
        assertNotNull( r.getAuthorList() );
        assertNotNull( r.getPublicationDate() );
    }

    @Test
    @Category(SlowTest.class)
    public void testSearchAndRetrieveByHTTPInChunks() throws Exception {
        Collection<String> searchTerms = new HashSet<>();
        searchTerms.add( "brain" );
        searchTerms.add( "hippocampus" );
        searchTerms.add( "habenula" );
        Collection<BibliographicReference> actualResult = pms.searchAndRetrieve( StringUtils.join( " ", searchTerms ), 100 );
        /*
         * at least, this was the result on 4/2008.
         */
        assertTrue( "Expected at least 10, got " + actualResult.size(), actualResult.size() >= 10 );
    }

    @Test
    public void testSearchAndRetrieveIdByHTTPBookshelf() throws Exception {
        Collection<String> searchTerms = new HashSet<>();
        searchTerms.add( "23865096" );
        Collection<BibliographicReference> actualResult = pms.retrieve( searchTerms );
        assertEquals( 1, actualResult.size() );
    }

    @Test
    public void testSearchAndRetrieveIdsByHTTP() throws Exception {
        Collection<String> searchTerms = new HashSet<>();
        searchTerms.add( "brain" );
        searchTerms.add( "hippocampus" );
        searchTerms.add( "habenula" );
        searchTerms.add( "glucose" );
        Collection<String> actualResult = pms.search( searchTerms, 100 );
        assertTrue( "Expect at least 5 results, got " + actualResult.size(), actualResult.size() >= 5 );
    }
}

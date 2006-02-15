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
package edu.columbia.gemma.loader.entrez.pubmed;

import java.util.Collection;
import java.util.HashSet;

import edu.columbia.gemma.common.description.BibliographicReference;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PubMedSearchTest extends TestCase {

    /*
     * Test method for 'edu.columbia.gemma.loader.entrez.pubmed.PubMedSearch.searchAndRetriveByHTTP(Collection<String>)'
     */
    public void testSearchAndRetriveByHTTP() throws Exception {
        PubMedSearch pms = new PubMedSearch();
        Collection<String> searchTerms = new HashSet<String>();
        searchTerms.add( "brain" );
        searchTerms.add( "hippocampus" );
        searchTerms.add( "habenula" );
        searchTerms.add( "glucose" );
        Collection<BibliographicReference> actualResult = pms.searchAndRetriveByHTTP( searchTerms );
        assertEquals( 4, actualResult.size() ); // at least, this was the result on 2/15/2006.
    }
    
    
    /*
     * Test method for 'edu.columbia.gemma.loader.entrez.pubmed.PubMedSearch.searchAndRetriveByHTTP(Collection<String>)'
     */
    public void testSearchAndRetriveByHTTPInChunks() throws Exception {
        PubMedSearch pms = new PubMedSearch();
        Collection<String> searchTerms = new HashSet<String>();
        searchTerms.add( "brain" );
        searchTerms.add( "hippocampus" );
        searchTerms.add( "habenula" );
        Collection<BibliographicReference> actualResult = pms.searchAndRetriveByHTTP( searchTerms );
        assertEquals( 20, actualResult.size() ); // at least, this was the result on 2/15/2006.
    }
}

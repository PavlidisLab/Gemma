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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.entrez.EntrezXmlUtils;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.model.common.description.BibliographicReference;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Search PubMed for terms, retrieve document records.
 *
 * @author pavlidis
 */
@CommonsLog
public class PubMedSearch {

    private static final int MAX_TRIES = 3;
    private static final int BATCH_SIZE = 30;

    private final String apiKey;
    private final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( new SimpleRetryPolicy( MAX_TRIES, 1000, 1.5 ), IOException.class, PubMedSearch.class.getName() );

    public PubMedSearch( String apiKey ) {
        this.apiKey = apiKey;
    }

    /**
     * Gets all the pubmed ID's that would be returned given a list of input terms, using two eUtil calls.
     *
     * @param  searchTerms                  search terms
     * @return The PubMed ids (as strings) for the search results.
     * @throws IOException                  IO problems
     */
    public Collection<String> search( Collection<String> searchTerms ) throws IOException {
        return search( String.join( " ", searchTerms ) );
    }

    /**
     * Gets all the pubmed ID's that would be returned from a pubmed search string, using two eUtil calls.
     *
     * @param  searchQuery                  - what would normally be typed into pubmed search box for example "Neural
     *                                      Pathways"[MeSH]
     * @return The PubMed ids (as strings) for the search results.
     * @throws IOException                  IO problems
     */
    public Collection<String> search( String searchQuery ) throws IOException {
        URL searchUrl = EntrezUtils.search( "pubmed", searchQuery, "xml", apiKey );
        log.debug( "Fetching " + searchUrl );
        return retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = searchUrl.openStream() ) {
                return EntrezXmlUtils.extractIds( EntrezXmlUtils.parse( is ) );
            }
        }, apiKey ), "retrieving the number of results of " + searchQuery );
    }

    /**
     * Search based on terms
     *
     * @param  terms                  search terms
     * @return BibliographicReference representing the publication
     * @throws IOException                  IO problems
     */
    public Collection<BibliographicReference> searchAndRetrieve( Collection<String> terms ) throws IOException {
        URL searchUrl = EntrezUtils.search( "pubmed", StringUtils.join( " ", terms ), "xml", apiKey );
        log.debug( "Fetching " + searchUrl );
        Collection<String> ids = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = searchUrl.openStream() ) {
                Document document = EntrezXmlUtils.parse( is );
                return EntrezXmlUtils.extractIds( document );
            }
        }, apiKey ), "fetching " + searchUrl );
        return fetchById( ids );
    }

    /**
     * @see EntrezUtils#fetchById(String, String, String, String, String)
     */
    @Nullable
    public BibliographicReference fetchById( int pubMedId ) throws IOException {
        URL fetchUrl = EntrezUtils.fetchById( "pubmed", String.valueOf( pubMedId ), "xml", "full", apiKey );
        log.debug( "Fetching " + fetchUrl );
        Collection<BibliographicReference> results = retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = fetchUrl.openStream() ) {
                return PubMedXMLParser.parse( is );
            }
        }, apiKey ), "fetching " + fetchUrl );

        if ( results == null || results.isEmpty() ) {
            return null;
        }
        assert results.size() == 1;
        return results.iterator().next();
    }

    /**
     * @see EntrezUtils#fetchById(String, String, String, String, String)
     */
    public Collection<BibliographicReference> fetchById( Collection<String> pubmedIds ) throws IOException {
        Collection<BibliographicReference> results = new HashSet<>();
        if ( pubmedIds == null || pubmedIds.isEmpty() ) return results;
        List<Integer> uniqueSortedIds = pubmedIds.stream().map( Integer::valueOf ).collect( Collectors.toList() );
        if ( uniqueSortedIds.isEmpty() ) {
            return Collections.emptyList();
        }
        List<BibliographicReference> result = new ArrayList<>( uniqueSortedIds.size() );
        for ( List<Integer> batch : ListUtils.partition( uniqueSortedIds.stream().distinct().collect( Collectors.toList() ), BATCH_SIZE ) ) {
            URL fetchUrl = EntrezUtils.fetchById( "pubmed",
                    batch.stream().map( String::valueOf ).collect( Collectors.joining( "," ) ),
                    "xml", "full", apiKey );
            log.debug( "Fetching " + fetchUrl );
            result.addAll( retryTemplate.execute( ( ctx ) ->
                    EntrezUtils.doNicely( () -> {
                        try ( InputStream is = fetchUrl.openStream() ) {
                            return PubMedXMLParser.parse( is );
                        }
                    }, apiKey ), "fetching " + fetchUrl ) );
        }
        return result;
    }
}

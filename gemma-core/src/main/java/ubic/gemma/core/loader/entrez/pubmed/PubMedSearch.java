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
import org.w3c.dom.Document;
import ubic.gemma.core.loader.entrez.EntrezQuery;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.entrez.EntrezXmlUtils;
import ubic.gemma.core.loader.entrez.EntrezRetmode;
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
    public Collection<String> search( Collection<String> searchTerms, int maxResults ) throws IOException {
        return search( String.join( " ", searchTerms ), maxResults );
    }

    /**
     * Gets all the pubmed ID's that would be returned from a pubmed search string, using two eUtil calls.
     *
     * @param  searchQuery                  - what would normally be typed into pubmed search box for example "Neural
     *                                      Pathways"[MeSH]
     * @return The PubMed ids (as strings) for the search results.
     */
    public Collection<String> search( String searchQuery, int maxResults ) throws IOException {
        EntrezQuery query = searchInternal( searchQuery );
        int max = maxResults > 0 ? Math.min( maxResults, query.getTotalRecords() ) : query.getTotalRecords();
        List<String> ids = new ArrayList<>( max );
        for ( int i = 0; i < max; i += BATCH_SIZE ) {
            URL fetchUrl = EntrezUtils.search( "pubmed", query, EntrezRetmode.XML, i, BATCH_SIZE, apiKey );
            ids.addAll( retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
                try ( InputStream is = fetchUrl.openStream() ) {
                    Document doc = EntrezXmlUtils.parse( is );
                    return EntrezXmlUtils.extractIds( doc );
                }
            }, apiKey ), "retrieving " + ( i + BATCH_SIZE ) + "/" + query.getTotalRecords() + " PubMed IDs from " + searchQuery ) );
        }
        return ids;
    }

    /**
     * Search based on terms and retrieve the PubMed records.
     * <p>
     * This is more efficient than calling {@link #search(String, int)} and then {@link #retrieve(Collection)} as it bypasses
     * the intermediary query to get PubMed IDs.
     *
     * @param  searchQuery            search terms
     * @return BibliographicReference representing the publication
     */
    public Collection<BibliographicReference> searchAndRetrieve( String searchQuery, int maxResults ) throws IOException {
        EntrezQuery query = searchInternal( searchQuery );
        int max = maxResults > 0 ? Math.min( maxResults, query.getTotalRecords() ) : query.getTotalRecords();
        List<BibliographicReference> ids = new ArrayList<>( max );
        for ( int i = 0; i < max; i += BATCH_SIZE ) {
            URL fetchUrl = EntrezUtils.fetch( "pubmed", query, EntrezRetmode.XML, i, BATCH_SIZE, apiKey );
            ids.addAll( fetch( fetchUrl ) );
        }
        return ids;
    }

    /**
     * Retrieve a single PubMed record by ID.
     * @see EntrezUtils#fetchById(String, String, EntrezRetmode, String, String)
     */
    @Nullable
    public BibliographicReference retrieve( String pubMedId ) throws IOException {
        URL fetchUrl = EntrezUtils.fetchById( "pubmed", pubMedId, EntrezRetmode.XML, "full", apiKey );
        Collection<BibliographicReference> results = fetch( fetchUrl );
        if ( results == null || results.isEmpty() ) {
            return null;
        }
        assert results.size() == 1;
        return results.iterator().next();
    }

    /**
     * Retrieve multiple PubMed records by ID.
     * @see EntrezUtils#fetchById(String, String, EntrezRetmode, String, String)
     */
    public Collection<BibliographicReference> retrieve( Collection<String> pubmedIds ) throws IOException {
        Collection<BibliographicReference> results = new HashSet<>();
        if ( pubmedIds == null || pubmedIds.isEmpty() ) return results;
        List<String> uniqueIds = pubmedIds.stream().distinct().collect( Collectors.toList() );
        if ( uniqueIds.isEmpty() ) {
            return Collections.emptyList();
        }
        List<BibliographicReference> result = new ArrayList<>( uniqueIds.size() );
        for ( List<String> batch : ListUtils.partition( uniqueIds.stream().distinct().collect( Collectors.toList() ), BATCH_SIZE ) ) {
            URL fetchUrl = EntrezUtils.fetchById( "pubmed",
                    batch.stream().map( String::valueOf ).collect( Collectors.joining( "," ) ),
                    EntrezRetmode.XML, "full", apiKey );
            result.addAll( fetch( fetchUrl ) );
        }
        return result;
    }

    private EntrezQuery searchInternal( String searchQuery ) throws IOException {
        URL searchUrl = EntrezUtils.search( "pubmed", searchQuery, EntrezRetmode.XML, apiKey );
        log.debug( "Fetching " + searchUrl );
        return retryTemplate.execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = searchUrl.openStream() ) {
                return EntrezXmlUtils.getQuery( EntrezXmlUtils.parse( is ) );
            }
        }, apiKey ), "searching " + searchQuery );
    }

    private Collection<BibliographicReference> fetch( URL fetchUrl ) throws IOException {
        log.debug( "Fetching " + fetchUrl );
        return retryTemplate.execute( ( ctx ) ->
                EntrezUtils.doNicely( () -> {
                    try ( InputStream is = fetchUrl.openStream() ) {
                        return PubMedXMLParser.parse( is );
                    }
                }, apiKey ), "fetching " + fetchUrl );
    }
}

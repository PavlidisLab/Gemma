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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.model.common.description.BibliographicReference;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Search PubMed for terms, retrieve document records.
 *
 * @author pavlidis
 */
public class PubMedSearch {

    private static final Log log = LogFactory.getLog( PubMedSearch.class );
    private static final int CHUNK_SIZE = 10; // don't retrieve too many at once, it isn't nice.
    private static final int MAX_TRIES = 3;

    private final String uri;
    private final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( new SimpleRetryPolicy( MAX_TRIES, 1000, 1.5 ), IOException.class, PubMedSearch.class.getName() );
    private final ESearchXMLParser parser = new ESearchXMLParser();
    private final PubMedXMLFetcher fetcher;

    public PubMedSearch( String apiKey ) {
        this.uri = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmode=xml&rettype=full" + ( StringUtils.isNotBlank( apiKey ) ? "&api_key=" + urlEncode( apiKey ) : "" );
        this.fetcher = new PubMedXMLFetcher( apiKey );
    }

    /**
     * Search based on terms
     *
     * @param  searchTerms                  search terms
     * @return BibliographicReference representing the publication
     * @throws IOException                  IO problems
     */
    public Collection<BibliographicReference> searchAndRetrieveByHTTP( Collection<String> searchTerms ) throws IOException {
        URL toBeGotten = new URL( uri + "&term=" + urlEncode( StringUtils.join( " ", searchTerms ) ) );
        log.info( "Fetching " + toBeGotten );
        Collection<String> ids = retryTemplate.execute( ( ctx ) -> {
            try ( InputStream is = toBeGotten.openStream() ) {
                return parser.parse( is );
            } catch ( ParserConfigurationException | ESearchException | SAXException e ) {
                throw new RuntimeException( e );
            }
        }, "fetching " + toBeGotten );

        try {
            Thread.sleep( 100 );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }

        Collection<BibliographicReference> results = fetchById( ids );

        log.info( "Fetched " + results.size() + " references" );

        return results;
    }

    public Collection<BibliographicReference> searchAndRetrieveIdByHTTP( Collection<String> searchTerms ) throws IOException {
        Collection<BibliographicReference> results = fetchById( searchTerms );
        log.info( "Fetched " + results.size() + " references" );
        return results;
    }

    /**
     * Gets all the pubmed ID's that would be returned given a list of input terms, using two eUtil calls.
     *
     * @param  searchTerms                  search terms
     * @return The PubMed ids (as strings) for the search results.
     * @throws IOException                  IO problems
     */
    public Collection<String> searchAndRetrieveIdsByHTTP( Collection<String> searchTerms ) throws IOException {
        // space them out, then let the overloaded method urlencode them
        return searchAndRetrieveIdsByHTTP( String.join( " ", searchTerms ) );
    }

    /**
     * Gets all the pubmed ID's that would be returned from a pubmed search string, using two eUtil calls.
     *
     * @param  searchQuery                  - what would normally be typed into pubmed search box for example "Neural
     *                                      Pathways"[MeSH]
     * @return The PubMed ids (as strings) for the search results.
     * @throws IOException                  IO problems
     */
    public Collection<String> searchAndRetrieveIdsByHTTP( String searchQuery ) throws IOException {
        // build URL
        String URLString = uri + "&term=" + urlEncode( searchQuery );

        int count = retryTemplate.execute( ( ctx ) -> {
            // log.info( "Fetching " + toBeGotten );
            // builder.append("&retmax=" + 70000);
            URL toBeGotten = new URL( URLString );
            // parse how many
            try ( InputStream is = toBeGotten.openStream() ) {
                return parser.getCount( is );
            } catch ( ParserConfigurationException | SAXException | ESearchException e ) {
                throw new RuntimeException( e );
            }
        }, "retrieving the number of results of " + searchQuery );

        // be nice
        try {
            Thread.sleep( 100 );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }

        return retryTemplate.execute( ( ctx ) -> {
            // now get them all
            URL toBeGotten = new URL( URLString + "&retmax=" + count );
            log.info( "Fetching " + count + " IDs from:" + toBeGotten );
            try ( InputStream is = toBeGotten.openStream() ) {
                return parser.parse( is );
            } catch ( ParserConfigurationException | SAXException | ESearchException e ) {
                throw new RuntimeException( e );
            }
        }, "retrieving " + searchQuery );
    }

    private Collection<BibliographicReference> fetchById( Collection<String> ids ) throws IOException {
        Collection<BibliographicReference> results = new HashSet<>();

        if ( ids == null || ids.isEmpty() ) return results;

        for ( List<String> batch : ListUtils.partition( new ArrayList<>( ids ), CHUNK_SIZE ) ) {
            log.debug( "Fetching PubMed IDs " + batch );
            results.addAll( fetcher.retrieveByHTTP( batch.stream().map( Integer::parseInt ).collect( Collectors.toSet() ) ) );
            try {
                Thread.sleep( 100 );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                return results;
            }
        }

        return results;
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}

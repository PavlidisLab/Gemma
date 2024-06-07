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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.core.config.Settings;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;

/**
 * Search PubMed for terms, retrieve document records.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class PubMedSearch {
    private static final Log log = LogFactory.getLog( PubMedSearch.class );
    private static final int CHUNK_SIZE = 10; // don't retrive too many at once, it isn't nice.
    private static final int MAX_TRIES = 3;
    private final String uri;

    public PubMedSearch() {
        String baseURL = ( String ) Settings.getProperty( "entrez.esearch.baseurl" );
        String db = ( String ) Settings.getProperty( "entrez.efetch.pubmed.db" );
        // String idtag = ( String ) config.getProperty( "entrez.efetch.pubmed.idtag" );
        String retmode = ( String ) Settings.getProperty( "entrez.efetch.pubmed.retmode" );
        String rettype = ( String ) Settings.getProperty( "entrez.efetch.pubmed.rettype" );
        String apikey = Settings.getString( "entrez.efetch.apikey" );
        uri = baseURL + "&" + db + "&" + retmode + "&" + rettype + ( StringUtils.isNotBlank( apikey ) ? "&api_key=" + apikey : "" );
    }

    /**
     * Search based on terms
     *
     * @param  searchTerms                  search terms
     * @return BibliographicReference representing the publication
     * @throws IOException                  IO problems
     * @throws SAXException                 sax exception
     * @throws ParserConfigurationException parser config exception
     */
    public Collection<BibliographicReference> searchAndRetrieveByHTTP( Collection<String> searchTerms )
            throws IOException, SAXException, ParserConfigurationException, ESearchException {
        StringBuilder builder = new StringBuilder();
        builder.append( uri );
        builder.append( "&term=" );
        for ( String string : searchTerms ) {
            builder.append( string );
            builder.append( "+" );
        }
        URL toBeGotten = new URL( StringUtils.chomp( builder.toString() ) );
        log.info( "Fetching " + toBeGotten );

        ESearchXMLParser parser = new ESearchXMLParser();
        Collection<String> ids = null;
        int numTries = 0;
        while ( ids == null && numTries < MAX_TRIES ) {
            try {
                numTries++;
                ids = parser.parse( toBeGotten.openStream() );
            } catch ( IOException e ) {
                if ( numTries == MAX_TRIES ) throw e;
                log.warn( "Failed attempt (" + numTries + "/" + MAX_TRIES + ") " + e.getMessage() );
                try {
                    // be nice
                    Thread.sleep( 200 );
                } catch ( InterruptedException e1 ) {
                }
            }
        }

        Collection<BibliographicReference> results = fetchById( ids );

        log.info( "Fetched " + results.size() + " references" );

        return results;
    }

    public Collection<BibliographicReference> searchAndRetrieveIdByHTTP( Collection<String> searchTerms )
            throws IOException {

        Collection<BibliographicReference> results;

        results = fetchById( searchTerms );

        log.info( "Fetched " + results.size() + " references" );

        return results;
    }

    /**
     * Gets all the pubmed ID's that would be returned given a list of input terms, using two eUtil calls.
     *
     * @param  searchTerms                  search terms
     * @return The PubMed ids (as strings) for the search results.
     * @throws IOException                  IO problems
     * @throws SAXException                 SAX exception
     * @throws ParserConfigurationException config problems
     */
    public Collection<String> searchAndRetrieveIdsByHTTP( Collection<String> searchTerms )
            throws IOException, SAXException, ParserConfigurationException, ESearchException {
        StringBuilder builder = new StringBuilder();
        for ( String word : searchTerms ) {
            // space them out, then let the overloaded method urlencode them
            builder.append( word );
            builder.append( " " );
        }
        return searchAndRetrieveIdsByHTTP( builder.toString() );
    }

    /**
     * Gets all the pubmed ID's that would be returned from a pubmed search string, using two eUtil calls.
     *
     * @param  searchQuery                  - what would normally be typed into pubmed search box for example "Neural
     *                                      Pathways"[MeSH]
     * @return The PubMed ids (as strings) for the search results.
     * @throws IOException                  IO problems
     * @throws SAXException                 SAX exception
     * @throws ParserConfigurationException config problems
     */
    public Collection<String> searchAndRetrieveIdsByHTTP( String searchQuery )
            throws IOException, SAXException, ParserConfigurationException, ESearchException {
        ESearchXMLParser parser = new ESearchXMLParser();
        // encode it
        searchQuery = URLEncoder.encode( searchQuery, "UTF-8" );

        // build URL
        String URLString = uri + "&term=" + searchQuery;
        // builder.append("&retmax=" + 70000);
        URL toBeGotten = new URL( URLString );
        // log.info( "Fetching " + toBeGotten );
        // parse how many
        int count = parser.getCount( toBeGotten.openStream() );

        // now get them all
        URLString += "&retmax=" + count;
        toBeGotten = new URL( URLString );
        log.info( "Fetching " + count + " IDs from:" + toBeGotten );

        return parser.parse( toBeGotten.openStream() );
    }

    /**
     *
     */
    private Collection<BibliographicReference> fetchById( Collection<String> ids ) throws IOException {
        Collection<BibliographicReference> results = new HashSet<>();

        if ( ids == null || ids.isEmpty() ) return results;

        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
        Collection<Integer> ints = new HashSet<>();
        int count = 0;

        for ( String str : ids ) {
            log.debug( "Fetching pubmed " + str );

            ints.add( Integer.parseInt( str ) );

            count++;

            if ( count >= CHUNK_SIZE ) {
                results.addAll( fetcher.retrieveByHTTP( ints ) );
                ints = new HashSet<>();
                count = 0;
            }

            // be nice
            try {
                Thread.sleep( 100 );
            } catch ( InterruptedException e ) {

            }
        }

        if ( count > 0 ) {
            results.addAll( fetcher.retrieveByHTTP( ints ) );
        }
        return results;
    }
}

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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import edu.columbia.gemma.common.description.BibliographicReference;

/**
 * Search PubMed for terms, retrieve document records.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedSearch {
    protected static final Log log = LogFactory.getLog( PubMedSearch.class );
    private String uri;

    private static final int CHUNK_SIZE = 10; // don't retrive too many at once, it isn't nice.

    /**
     * 
     */
    public PubMedSearch() {
        try {
            Configuration config = new PropertiesConfiguration( "Gemma.properties" );
            String baseURL = ( String ) config.getProperty( "entrez.esearch.baseurl" );
            String db = ( String ) config.getProperty( "entrez.efetch.pubmed.db" );
            // String idtag = ( String ) config.getProperty( "entrez.efetch.pubmed.idtag" );
            String retmode = ( String ) config.getProperty( "entrez.efetch.pubmed.retmode" );
            String rettype = ( String ) config.getProperty( "entrez.efetch.pubmed.rettype" );
            uri = baseURL + "&" + db + "&" + retmode + "&" + rettype;
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * For an integer pubmed id
     * 
     * @param pubMedId
     * @return BibliographicReference representing the publication
     * @throws IOException
     */
    public Collection<BibliographicReference> searchAndRetriveByHTTP( Collection<String> searchTerms )
            throws IOException, SAXException, ParserConfigurationException {

        StringBuilder builder = new StringBuilder();
        builder.append( uri );
        builder.append( "&term=" );
        for ( String string : searchTerms ) {
            builder.append( string );
            builder.append( "+" );
        }
        URL toBeGotten = new URL( StringUtils.chomp( builder.toString() ) );
        log.info( "Fetching " + toBeGotten );

        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
        ESearchXMLParser parser = new ESearchXMLParser();
        Collection<String> ids = parser.parse( toBeGotten.openStream() );

        Collection<BibliographicReference> results = new HashSet<BibliographicReference>();

        Collection<Integer> ints = new HashSet<Integer>();
        int count = 0;
        for ( String str : ids ) {
            ints.add( Integer.parseInt( str ) );
            count++;

            if ( count >= CHUNK_SIZE ) {
                results.addAll( fetcher.retrieveByHTTP( ints ) );
                ints = new HashSet<Integer>();
                count = 0;
            }

        }

        if ( count > 0 ) {
            results.addAll( fetcher.retrieveByHTTP( ints ) );
        }

        log.info( "Fetched " + results.size() + " references" );

        return results;
    }
}

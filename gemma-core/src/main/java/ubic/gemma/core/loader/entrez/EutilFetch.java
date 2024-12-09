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
package ubic.gemma.core.loader.entrez;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.XMLUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

import static ubic.gemma.core.loader.entrez.NcbiXmlUtils.createDocumentBuilder;

/**
 * @author paul
 */
@CommonsLog
public class EutilFetch {

    private static final String EFETCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=";
    private static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=";
    private static final String EQUERY = "https://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=";
    private static final int MAX_TRIES = 3;

    private final String apiKey;
    private final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( MAX_TRIES, 1000, 1.5, IOException.class, EutilFetch.class.getName() );

    public EutilFetch( String apiKey ) {
        this.apiKey = apiKey;
    }

    /**
     * Attempts to fetch data via Eutils; failures will be re-attempted several times.
     * <p>
     * See <a href="http://www.ncbi.nlm.nih.gov/corehtml/query/static/esummary_help.html">ncbi help</a>
     *
     * @param db           e.g., gds.
     * @param searchString search string
     * @param limit        - Maximum number of records to return.
     * @throws IOException if there is a problem while manipulating the file
     */
    public String fetch( String db, String searchString, int limit ) throws IOException {
        URL searchUrl = new URL( EutilFetch.ESEARCH + urlEncode( db )
                + "&usehistory=y"
                + "&term=" + urlEncode( searchString )
                + ( StringUtils.isNotBlank( apiKey ) ? "&api_key=" + urlEncode( apiKey ) : "" ) );
        Document document = retryTemplate.execute( ( attempt, lastAttempt ) -> {
            try ( InputStream is = searchUrl.openStream() ) {
                DocumentBuilder builder = createDocumentBuilder();
                return builder.parse( is );
            } catch ( SAXException | ParserConfigurationException e ) {
                throw new RuntimeException( e );
            }
        }, "retrieve " + searchUrl );

        NodeList countNode = document.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count;
        try {
            count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        } catch ( NumberFormatException e ) {
            throw new IOException( "Could not parse count from: " + searchUrl );
        }

        if ( count == 0 )
            throw new IOException( "Got no records from: " + searchUrl );

        NodeList qnode = document.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = document.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL( EutilFetch.EFETCH + urlEncode( db )
                + "&mode=text"
                + "&query_key=" + urlEncode( queryId )
                + "&WebEnv=" + urlEncode( cookie )
                + "&retmax=" + limit
                + ( StringUtils.isNotBlank( apiKey ) ? "&api_key=" + urlEncode( apiKey ) : "" ) );
        return retryTemplate.execute( ( attempt, lastAttempt ) -> {
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( fetchUrl.openStream() ) ) ) {
                StringBuilder buf = new StringBuilder();
                String line;
                while ( ( line = br.readLine() ) != null ) {
                    buf.append( line );
                }
                return buf.toString();
            }
        }, "retrieve " + fetchUrl );
    }

    public Collection<String> query( String db, String query ) throws IOException {
        URL url = new URL( EutilFetch.EQUERY + urlEncode( db )
                + "&term=" + urlEncode( query )
                + "&cmd=search"
                + ( StringUtils.isNotBlank( apiKey ) ? "&api_key=" + urlEncode( apiKey ) : "" ) );
        return retryTemplate.execute( ( attempt, lastAttempt ) -> {
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream() ) ) ) {
                return br.lines().collect( Collectors.toList() );
            }
        }, "retrieve " + url );
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}

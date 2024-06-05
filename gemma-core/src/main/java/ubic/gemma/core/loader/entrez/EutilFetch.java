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
import org.apache.tools.ant.filters.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.core.config.Settings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author paul
 */
@CommonsLog
public class EutilFetch {

    private static final String EFETCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=";
    private static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=";
    private static final String APIKEY = Settings.getString( "entrez.efetch.apikey" );
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static final int MAX_TRIES = 3;

    /**
     * Attempts to fetch data via Eutils; failures will be re-attempted several times.
     *
     * @param db           db
     * @param searchString search string
     * @param limit        limit
     * @return data
     * @throws IOException when there are IO problems
     */
    public static String fetch( String db, String searchString, int limit ) throws IOException {
        return EutilFetch.fetch( db, searchString, Mode.TEXT, limit );
    }

    public static Document parseStringInputStream( String details )
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder builder = EutilFetch.factory.newDocumentBuilder();
        int tries = 0;

        while ( true ) {
            try ( InputStream is = new StringInputStream( details ) ) {
                return builder.parse( is );
            } catch ( IOException e ) {
                // FIXME this can't happen, can it?
                tries = EutilFetch.tryAgainOrFail( tries, e );
            }
        }
    }

    /**
     * see <a href="http://www.ncbi.nlm.nih.gov/corehtml/query/static/esummary_help.html">ncbi help</a>
     *
     * @param db           e.g., gds.
     * @param searchString search string
     * @param mode         HTML,TEXT or XML FIXME only provides XML.
     * @param limit        - Maximum number of records to return.
     * @return XML
     * @throws IOException if there is a problem while manipulating the file
     */
    @SuppressWarnings("SameParameterValue") // Only TEXT is used, also observe the parameter javadoc fix me note
    private static String fetch( String db, String searchString, Mode mode, int limit ) throws IOException {

        URL searchUrl = new URL(
                EutilFetch.ESEARCH + db + "&usehistory=y&term=" + searchString + ( StringUtils.isNotBlank( APIKEY ) ?
                        "&api_key=" + APIKEY :
                        "" ) );
        URLConnection conn = null;
        int numTries = 0;

        while ( conn == null && numTries < MAX_TRIES ) {
            try {
                numTries++;
                EutilFetch.factory.setIgnoringComments( true );
                EutilFetch.factory.setValidating( false );

                Document document = EutilFetch.parseSUrlInputStream( searchUrl );

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

                URL fetchUrl = new URL(
                        EutilFetch.EFETCH + db + "&mode=" + mode.toString().toLowerCase() + "&query_key=" + queryId
                                + "&WebEnv=" + cookie + "&retmax=" + limit + ( StringUtils.isNotBlank( APIKEY ) ?
                                "&api_key=" + APIKEY :
                                "" ) );

                conn = fetchUrl.openConnection();
                conn.connect();
            } catch ( ParserConfigurationException | SAXException e1 ) {
                throw new RuntimeException( "Failed to parse XML.", e1 );
            } catch ( IOException e2 ) {
                if ( numTries == MAX_TRIES )
                    throw e2;
                log.warn( "I/O error occurred while parsing XML, will sleep for 500ms and try again", e2 );
                EutilFetch.trySleep( 500 );
            }
        }

        if ( conn == null )
            throw new IllegalStateException( "Connection was null" );

        try ( InputStream is = conn.getInputStream() ) {

            try ( BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {
                StringBuilder buf = new StringBuilder();
                String line;
                while ( ( line = br.readLine() ) != null ) {
                    buf.append( line );
                }

                return buf.toString();
            }
        }

    }

    private static Document parseSUrlInputStream( URL url )
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder builder = EutilFetch.factory.newDocumentBuilder();
        int tries = 0;

        while ( true ) {
            URLConnection conn = url.openConnection();
            conn.connect();
            try ( InputStream is = conn.getInputStream() ) {
                return builder.parse( is );
            } catch ( IOException e ) {
                tries = EutilFetch.tryAgainOrFail( tries, e );
            }
        }
    }

    /**
     * Does this ever get called?
     *
     * @param tries amount of tries
     * @param e     error to be thrown on failure
     * @return the amount of tries it took already after this run
     * @throws IOException the given exception
     */
    private static int tryAgainOrFail( int tries, IOException e ) throws IOException {
        if ( e.getMessage().contains( "429" ) ) {
            tries++;
            if ( tries > MAX_TRIES ) {
                log.warn( "Got HTTP 429 " + tries + " times" );
                throw e;
            }
            log.warn( "got HTTP 429 " + tries + " time(s), letting the server rest." );
            EutilFetch.trySleep( 500 * tries );
        } else {
            throw e;
        }
        return tries;
    }

    private static void trySleep( int milliseconds ) {
        try {
            Thread.sleep( milliseconds );
        } catch ( InterruptedException e1 ) {
            log.error( "Sleep for " + milliseconds + "ms was interrupted.", e1 ); // Log and try to continue
        }
    }

    @SuppressWarnings("unused")
    // EutilFetch.fetch(java.lang.String, java.lang.String, ubic.gemma.core.loader.entrez.EutilFetch.Mode, int) fix me note
    public enum Mode {
        HTML, TEXT, XML
    }

}

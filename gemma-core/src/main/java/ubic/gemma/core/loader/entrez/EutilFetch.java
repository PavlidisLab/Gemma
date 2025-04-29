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
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.XMLUtils;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

import static ubic.gemma.core.loader.entrez.NcbiXmlUtils.createDocumentBuilder;

/**
 * @author paul
 */
@CommonsLog
public class EutilFetch {

    private static final int MAX_TRIES = 3;

    private final String apiKey;
    private final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( new SimpleRetryPolicy( MAX_TRIES, 1000, 1.5 ), IOException.class, EutilFetch.class.getName() );

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
    @Nullable
    public String fetch( String db, String searchString, int limit ) throws IOException {
        URL searchUrl = EntrezUtils.search( db, searchString, true, apiKey );
        return retryTemplate.execute( ( ctx ) -> {
            Document document;
            try ( InputStream is = searchUrl.openStream() ) {
                DocumentBuilder builder = createDocumentBuilder();
                document = builder.parse( is );
            } catch ( SAXException | ParserConfigurationException e ) {
                throw new RuntimeException( e );
            }

            int count;
            try {
                count = Integer.parseInt( XMLUtils.getTextValue( document.getElementsByTagName( "Count" ).item( 0 ) ) );
            } catch ( NumberFormatException e ) {
                throw new IOException( "Could not parse count from: " + searchUrl );
            }

            if ( count == 0 ) {
                return null;
            }

            String queryId = XMLUtils.getTextValue( document.getElementsByTagName( "QueryKey" ).item( 0 ) );
            String cookie = XMLUtils.getTextValue( document.getElementsByTagName( "WebEnv" ).item( 0 ) );

            URL fetchUrl = EntrezUtils.summary( db, queryId, "xml", 0, limit, cookie, apiKey );
            return IOUtils.toString( fetchUrl, StandardCharsets.UTF_8 );
        }, "retrieve " + searchUrl );
    }

    public Collection<String> query( String db, String query ) throws IOException {
        URL url = EntrezUtils.query( db, query, "search", apiKey );
        return retryTemplate.execute( ( ctx ) -> {
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream() ) ) ) {
                return br.lines().collect( Collectors.toList() );
            }
        }, "retrieve " + url );
    }
}

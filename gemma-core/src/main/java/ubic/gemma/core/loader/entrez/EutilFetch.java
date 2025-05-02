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
import org.w3c.dom.Document;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * High-level API for interacting with NCBI Entrez utilities.
 * @author paul
 */
@CommonsLog
public class EutilFetch {

    private static final int MAX_TRIES = 3;
    private static final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( new SimpleRetryPolicy( MAX_TRIES, 1000, 1.5 ), IOException.class, EutilFetch.class.getName() );

    /**
     * Attempts to fetch data via Eutils; failures will be re-attempted several times.
     * <p>
     * See <a href="http://www.ncbi.nlm.nih.gov/corehtml/query/static/esummary_help.html">ncbi help</a>
     *
     * @param db     e.g., gds.
     * @param term   search string
     * @param limit  maximum number of records to return.
     * @param apiKey
     * @throws IOException if there is a problem while manipulating the file
     * @see EntrezUtils#summary(String, EntrezQuery, EntrezRetmode, int, int, String)
     */
    @Nullable
    public static Document summary( String db, String term, int limit, @Nullable String apiKey ) throws IOException {
        URL searchUrl = EntrezUtils.search( db, term, EntrezRetmode.XML, apiKey );
        return retryTemplate.execute( ( ctx ) -> {
            Document document = EntrezUtils.doNicely( () -> {
                try ( InputStream is = searchUrl.openStream() ) {
                    return EntrezXmlUtils.parse( is );
                }
            }, apiKey );

            EntrezQuery query = EntrezXmlUtils.getQuery( document );
            if ( query.getTotalRecords() == 0 ) {
                return null;
            }

            URL fetchUrl = EntrezUtils.summary( db, query, EntrezRetmode.XML, 0, limit, apiKey );
            return EntrezUtils.doNicely( () -> {
                try ( InputStream is = fetchUrl.openStream() ) {
                    return EntrezXmlUtils.parse( is );
                }
            }, apiKey );
        }, "retrieve " + searchUrl );
    }
}

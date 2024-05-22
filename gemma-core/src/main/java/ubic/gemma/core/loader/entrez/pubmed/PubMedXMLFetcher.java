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
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.persistence.util.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

/**
 * Class that can retrieve pubmed records (in XML format) via HTTP. The url used is configured via a resource.
 *
 * @author pavlidis
 */
public class PubMedXMLFetcher {

    private static final Log log = LogFactory.getLog( PubMedXMLFetcher.class );
    private final static int MAX_TRIES = 2;
    private final String uri;

    public PubMedXMLFetcher() {
        String baseURL = Settings.getString( "entrez.efetch.baseurl" );
        String db = Settings.getString( "entrez.efetch.pubmed.db" );
        String idtag = Settings.getString( "entrez.efetch.pubmed.idtag" );
        String retmode = Settings.getString( "entrez.efetch.pubmed.retmode" );
        String rettype = Settings.getString( "entrez.efetch.pubmed.rettype" );
        String apikey = Settings.getString( "entrez.efetch.apikey" );
        uri = baseURL + "&" + db + "&" + retmode + "&" + rettype + ( StringUtils.isNotBlank( apikey ) ? "&api_key=" + apikey : "" ) + "&" + idtag;

    }

    public Collection<BibliographicReference> retrieveByHTTP( Collection<Integer> pubMedIds ) throws IOException {
        StringBuilder buf = new StringBuilder();
        for ( Integer integer : pubMedIds ) {
            buf.append( integer ).append( "," );
        }
        URL toBeGotten = new URL( uri + StringUtils.chomp( buf.toString() ) );
        log.debug( "Fetching " + toBeGotten );
        PubMedXMLParser pmxp = new PubMedXMLParser();
        try ( InputStream is = toBeGotten.openStream() ) {
            return pmxp.parse( is );
        }
    }

    public BibliographicReference retrieveByHTTP( int pubMedId ) throws IOException {
        Collection<BibliographicReference> results = null;

        for ( int i = 0; i < MAX_TRIES; i++ ) {
            try {
                URL toBeGotten = new URL( uri + pubMedId );
                log.debug( "Fetching " + toBeGotten );
                PubMedXMLParser pmxp = new PubMedXMLParser();
                try ( InputStream is = toBeGotten.openStream() ) {
                    results = pmxp.parse( is );
                }
                if ( results != null && !results.isEmpty() )
                    break;
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException e ) {
                    // noop
                }

            } catch ( IOException e ) {
                if ( e.getMessage().contains( "429" ) ) { // too many requests 
                    try {
                        Thread.sleep( 5000 );
                    } catch ( InterruptedException e1 ) {
                        // noop
                    }
                } else {
                    throw e;
                }
            }
        }
        if ( results == null || results.isEmpty() ) {
            return null;
        }
        assert results.size() == 1;
        return results.iterator().next();

    }
}

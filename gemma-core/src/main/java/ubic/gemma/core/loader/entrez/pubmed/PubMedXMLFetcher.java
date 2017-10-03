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
        uri = baseURL + "&" + db + "&" + retmode + "&" + rettype + "&" + idtag;
    }

    public Collection<BibliographicReference> retrieveByHTTP( Collection<Integer> pubMedIds ) throws IOException {
        StringBuilder buf = new StringBuilder();
        for ( Integer integer : pubMedIds ) {
            buf.append( integer ).append( "," );
        }
        URL toBeGotten = new URL( uri + StringUtils.chomp( buf.toString() ) );
        log.debug( "Fetching " + toBeGotten );
        PubMedXMLParser pmxp = new PubMedXMLParser();
        return pmxp.parse( toBeGotten.openStream() );
    }

    public BibliographicReference retrieveByHTTP( int pubMedId ) {
        Collection<BibliographicReference> results = null;
        try {
            for ( int i = 0; i < MAX_TRIES; i++ ) {
                URL toBeGotten = new URL( uri + pubMedId );
                log.debug( "Fetching " + toBeGotten );
                PubMedXMLParser pmxp = new PubMedXMLParser();
                results = pmxp.parse( toBeGotten.openStream() );
                if ( results != null && results.size() > 0 )
                    break;
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException e ) {
                    // noop
                }
            }
            if ( results == null || results.size() == 0 ) {
                return null;
            }
            assert results.size() == 1;
            return results.iterator().next();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}

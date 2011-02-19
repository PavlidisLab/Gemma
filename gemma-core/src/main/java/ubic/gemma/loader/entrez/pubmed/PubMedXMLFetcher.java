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
package ubic.gemma.loader.entrez.pubmed;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.util.ConfigUtils;

/**
 * Class that can retrieve pubmed records (in XML format) via HTTP. The url used is configured via a resource.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLFetcher {

    protected static final Log log = LogFactory.getLog( PubMedXMLFetcher.class );
    private String uri;

    public PubMedXMLFetcher() {
        String baseURL = ConfigUtils.getString( "entrez.efetch.baseurl" );
        String db = ConfigUtils.getString( "entrez.efetch.pubmed.db" );
        String idtag = ConfigUtils.getString( "entrez.efetch.pubmed.idtag" );
        String retmode = ConfigUtils.getString( "entrez.efetch.pubmed.retmode" );
        String rettype = ConfigUtils.getString( "entrez.efetch.pubmed.rettype" );
        uri = baseURL + "&" + db + "&" + retmode + "&" + rettype + "&" + idtag;
    }

    /**
     * For an integer pubmed id
     * 
     * @param pubMedId
     * @return BibliographicReference for the id given.
     */
    public BibliographicReference retrieveByHTTP( int pubMedId ) {
        try {
            URL toBeGotten = new URL( uri + pubMedId );
            log.debug( "Fetching " + toBeGotten );
            PubMedXMLParser pmxp = new PubMedXMLParser();
            Collection<BibliographicReference> results = pmxp.parse( toBeGotten.openStream() );
            if ( results == null || results.size() == 0 ) {
                return null;
            }
            assert results.size() == 1;
            return results.iterator().next();
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * For collection of integer pubmed ids.
     * 
     * @param pubMedIds
     * @return Collection<BibliographicReference>
     * @throws IOException
     */
    public Collection<BibliographicReference> retrieveByHTTP( Collection<Integer> pubMedIds ) throws IOException {
        StringBuilder buf = new StringBuilder();
        for ( Integer integer : pubMedIds ) {
            buf.append( integer + "," );
        }
        URL toBeGotten = new URL( uri + StringUtils.chomp( buf.toString() ) );
        log.debug( "Fetching " + toBeGotten );
        PubMedXMLParser pmxp = new PubMedXMLParser();
        return pmxp.parse( toBeGotten.openStream() );
    }
}

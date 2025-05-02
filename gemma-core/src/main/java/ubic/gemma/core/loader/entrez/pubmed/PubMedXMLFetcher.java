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
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.model.common.description.BibliographicReference;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Class that can retrieve pubmed records (in XML format) via HTTP. The url used is configured via a resource.
 *
 * @author pavlidis
 */
public class PubMedXMLFetcher {

    private static final Log log = LogFactory.getLog( PubMedXMLFetcher.class );
    private final static int MAX_TRIES = 2;
    private final String uri;
    private final PubMedXMLParser pubMedXMLParser = new PubMedXMLParser();
    private final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( MAX_TRIES, 1000, 1.5, IOException.class, PubMedSearch.class.getName() );

    public PubMedXMLFetcher( String apiKey ) {
        uri = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=full" + ( StringUtils.isNotBlank( apiKey ) ? "&api_key=" + apiKey : "" );
    }

    public Collection<BibliographicReference> retrieveByHTTP( Collection<Integer> pubMedIds ) throws IOException {
        if ( pubMedIds.isEmpty() ) {
            return Collections.emptyList();
        }
        URL toBeGotten = new URL( uri + "&id=" + urlEncode( pubMedIds.stream().map( String::valueOf ).collect( Collectors.joining( "," ) ) ) );
        log.debug( "Fetching " + toBeGotten );
        return retryTemplate.execute( ( attempt, lastAttempt ) -> {
            try ( InputStream is = toBeGotten.openStream() ) {
                return pubMedXMLParser.parse( is );
            }
        }, "fetching " + toBeGotten );
    }

    @Nullable
    public BibliographicReference retrieveByHTTP( int pubMedId ) throws IOException {
        URL toBeGotten = new URL( uri + "&id=" + pubMedId );
        log.debug( "Fetching " + toBeGotten );
        Collection<BibliographicReference> results = retryTemplate.execute( ( attempt, lastAttempt ) -> {
            try ( InputStream is = toBeGotten.openStream() ) {
                return pubMedXMLParser.parse( is );
            }
        }, "fetching " + toBeGotten );

        if ( results == null || results.isEmpty() ) {
            return null;
        }
        assert results.size() == 1;
        return results.iterator().next();
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}

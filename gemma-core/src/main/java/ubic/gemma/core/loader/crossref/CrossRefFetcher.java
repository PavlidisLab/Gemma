/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.core.loader.crossref;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

/**
 * Fetch bibliographic metadata for a DOI from the CrossRef REST API
 * (<a href="https://api.crossref.org">api.crossref.org</a>).
 * <p>
 * Complements {@link ubic.gemma.core.loader.entrez.pubmed.PubMedSearch}: PubMed covers DOIs that NCBI
 * indexes, CrossRef covers the rest — most journal articles and, crucially, bioRxiv / medRxiv preprints
 * that PubMed does not carry. This is what lets a curator attach a preprint to an experiment instead of
 * noting it in a free-text comment.
 * <p>
 * Plain (non-Spring) helper, constructed at the use site like {@code PubMedSearch}. No API key is
 * required; a contact-bearing {@code User-Agent} opts into CrossRef's "polite pool".
 *
 * @author gemma
 */
@Slf4j
public class CrossRefFetcher {

    private static final String CROSSREF_WORKS_URL = "https://api.crossref.org/works/";
    private static final String USER_AGENT = "Gemma/2.0 (https://gemma.msl.ubc.ca; mailto:pavlab-support@msl.ubc.ca)";
    private static final int TIMEOUT_MS = 15000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retrieve a {@link BibliographicReference} for the given DOI from CrossRef.
     *
     * @param doi a DOI, already normalized to its bare form (no {@code https://doi.org/} prefix).
     * @return a transient {@link BibliographicReference} with its {@code pubAccession} set to the DOI
     *         (under the {@link ExternalDatabases#DOI} external database), or {@code null} if CrossRef has
     *         no record for the DOI (HTTP 404).
     * @throws IOException on a transport error or an unexpected (non-200, non-404) CrossRef response.
     */
    @Nullable
    public BibliographicReference retrieveByDoi( String doi ) throws IOException {
        String encoded = URLEncoder.encode( doi, StandardCharsets.UTF_8 );
        URL url = new URL( CROSSREF_WORKS_URL + encoded );
        HttpURLConnection conn = ( HttpURLConnection ) url.openConnection();
        conn.setRequestMethod( "GET" );
        conn.setRequestProperty( "Accept", "application/json" );
        conn.setRequestProperty( "User-Agent", USER_AGENT );
        conn.setConnectTimeout( TIMEOUT_MS );
        conn.setReadTimeout( TIMEOUT_MS );
        try {
            int status = conn.getResponseCode();
            if ( status == HttpURLConnection.HTTP_NOT_FOUND ) {
                log.debug( "CrossRef has no record for DOI " + doi );
                return null;
            }
            if ( status != HttpURLConnection.HTTP_OK ) {
                throw new IOException( "CrossRef returned HTTP " + status + " for DOI " + doi );
            }
            JsonNode root;
            try ( InputStream is = conn.getInputStream() ) {
                root = objectMapper.readTree( is );
            }
            JsonNode message = root.path( "message" );
            if ( message.isMissingNode() || message.isNull() ) {
                return null;
            }
            return toBibliographicReference( doi, message );
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Map a CrossRef {@code message} node to a transient {@link BibliographicReference}. Package-visible for
     * fixture-based testing without a live CrossRef call.
     */
    BibliographicReference toBibliographicReference( String doi, JsonNode message ) {
        BibliographicReference ref = BibliographicReference.Factory.newInstance();

        ref.setTitle( firstText( message.path( "title" ) ) );
        ref.setAuthorList( formatAuthors( message.path( "author" ) ) );
        ref.setPublicationDate( extractDate( message ) );

        // container-title for journal articles; institution / group-title for preprints (bioRxiv/medRxiv)
        String publication = firstText( message.path( "container-title" ) );
        if ( StringUtils.isBlank( publication ) ) {
            publication = firstObjectName( message.path( "institution" ) );
        }
        if ( StringUtils.isBlank( publication ) ) {
            publication = firstText( message.path( "group-title" ) );
        }
        if ( StringUtils.isBlank( publication ) ) {
            publication = textOrNull( message.path( "publisher" ) );
        }
        ref.setPublication( publication );

        ref.setVolume( textOrNull( message.path( "volume" ) ) );
        ref.setIssue( textOrNull( message.path( "issue" ) ) );
        ref.setPages( textOrNull( message.path( "page" ) ) );
        ref.setPublisher( textOrNull( message.path( "publisher" ) ) );

        String fullTextUri = textOrNull( message.path( "URL" ) );
        ref.setFullTextUri( fullTextUri != null ? fullTextUri : "https://doi.org/" + doi );

        String abstractText = textOrNull( message.path( "abstract" ) );
        if ( abstractText != null ) {
            // CrossRef abstracts are JATS XML; strip the tags for a plain-text abstract.
            ref.setAbstractText( abstractText.replaceAll( "<[^>]+>", "" ).trim() );
        }

        ExternalDatabase doiDb = ExternalDatabase.Factory.newInstance();
        doiDb.setName( ExternalDatabases.DOI );
        ref.setPubAccession( DatabaseEntry.Factory.newInstance( doi, doiDb ) );

        return ref;
    }

    /**
     * Format CrossRef's structured author array to the same "Family, Given; Family, Given; " shape the
     * PubMed parser produces, so DOI- and PubMed-sourced references read consistently.
     */
    @Nullable
    private static String formatAuthors( JsonNode authors ) {
        if ( !authors.isArray() || authors.isEmpty() ) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for ( JsonNode a : authors ) {
            String family = textOrNull( a.path( "family" ) );
            String given = textOrNull( a.path( "given" ) );
            if ( family == null && given == null ) {
                String name = textOrNull( a.path( "name" ) ); // organizational author
                if ( name != null ) {
                    sb.append( name ).append( "; " );
                }
                continue;
            }
            if ( family != null ) {
                sb.append( family );
            }
            if ( given != null ) {
                sb.append( ", " ).append( given );
            }
            sb.append( "; " );
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Extract a publication date from CrossRef's {@code date-parts}. Prefers {@code published}, falling
     * back to {@code issued} then {@code created}. Missing month/day default to January / the 1st.
     */
    @Nullable
    private static Date extractDate( JsonNode message ) {
        for ( String field : new String[] { "published", "published-online", "published-print", "issued", "created" } ) {
            JsonNode parts = message.path( field ).path( "date-parts" );
            if ( parts.isArray() && !parts.isEmpty() && parts.get( 0 ).isArray() && !parts.get( 0 ).isEmpty() ) {
                JsonNode dp = parts.get( 0 );
                int year = dp.get( 0 ).asInt();
                int month = dp.size() > 1 ? dp.get( 1 ).asInt() : 1;
                int day = dp.size() > 2 ? dp.get( 2 ).asInt() : 1;
                Calendar c = Calendar.getInstance();
                c.clear();
                c.set( year, Math.max( 0, month - 1 ), Math.max( 1, day ) );
                return c.getTime();
            }
        }
        return null;
    }

    @Nullable
    private static String firstText( JsonNode arrayNode ) {
        if ( arrayNode.isArray() && !arrayNode.isEmpty() ) {
            return textOrNull( arrayNode.get( 0 ) );
        }
        return null;
    }

    @Nullable
    private static String firstObjectName( JsonNode arrayNode ) {
        if ( arrayNode.isArray() && !arrayNode.isEmpty() ) {
            return textOrNull( arrayNode.get( 0 ).path( "name" ) );
        }
        return null;
    }

    @Nullable
    private static String textOrNull( JsonNode node ) {
        if ( node == null || node.isMissingNode() || node.isNull() ) {
            return null;
        }
        String t = node.asText();
        return StringUtils.isBlank( t ) ? null : t;
    }
}

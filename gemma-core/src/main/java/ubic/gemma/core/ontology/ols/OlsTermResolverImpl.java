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
package ubic.gemma.core.ontology.ols;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Resolves a term IRI against the EBI OLS4 REST API ({@code GET {base}/api/terms?iri={iri}}).
 * <p>
 * Both positive and negative lookups are cached (in the admin-evictable {@link #CACHE_NAME} cache) so a
 * repeatedly-submitted good — or fabricated — URI is only sent to OLS once. Transient failures are NOT
 * cached: they surface as {@link OlsUnavailableException} and are retried on the next call.
 *
 * @author gemma
 */
@Slf4j
@Component
public class OlsTermResolverImpl implements OlsTermResolver {

    /**
     * Admin-evictable cache of IRI → {@link OlsTerm}. Registered in {@code EhcacheConfig#APP_CACHES}.
     */
    public static final String CACHE_NAME = "OlsTermResolver.terms";

    private static final String USER_AGENT = "Gemma/2.0 (https://gemma.msl.ubc.ca; mailto:pavlab-support@msl.ubc.ca)";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemma.ols.baseurl}")
    private String baseUrl;

    @Value("${gemma.ols.timeout.ms}")
    private int timeoutMs;

    @Autowired
    private CacheManager cacheManager;

    @Nullable
    private Cache cache;

    /**
     * Lazily resolve the cache: the {@link CacheManager} is not reliably populated at construction time on
     * some Spring boot orderings, so we look the cache up on first use.
     */
    @Nullable
    private Cache getCache() {
        if ( cache == null && cacheManager != null ) {
            cache = cacheManager.getCache( CACHE_NAME );
        }
        return cache;
    }

    @Nullable
    @Override
    public OlsTerm resolve( String iri ) throws OlsUnavailableException {
        Assert.isTrue( StringUtils.isNotBlank( iri ), "IRI must not be blank." );
        Cache c = getCache();
        if ( c != null ) {
            Cache.ValueWrapper wrapper = c.get( iri );
            if ( wrapper != null ) {
                OlsTerm cached = ( OlsTerm ) wrapper.get();
                return cached != null && cached.isFound() ? cached : null;
            }
        }
        OlsTerm term = query( iri );
        if ( c != null ) {
            c.put( iri, term != null ? term : OlsTerm.notFound( iri ) );
        }
        return term;
    }

    /**
     * Perform the live OLS call. Returns {@code null} when OLS has no term for the IRI; throws when OLS is
     * unreachable or returns an unexpected status.
     */
    @Nullable
    private OlsTerm query( String iri ) throws OlsUnavailableException {
        String url = baseUrl + "/api/terms?iri=" + URLEncoder.encode( iri, StandardCharsets.UTF_8 );
        HttpURLConnection conn = null;
        try {
            conn = ( HttpURLConnection ) new URL( url ).openConnection();
            conn.setRequestMethod( "GET" );
            conn.setRequestProperty( "Accept", "application/json" );
            conn.setRequestProperty( "User-Agent", USER_AGENT );
            conn.setConnectTimeout( timeoutMs );
            conn.setReadTimeout( timeoutMs );
            int status = conn.getResponseCode();
            if ( status == HttpURLConnection.HTTP_NOT_FOUND ) {
                return null;
            }
            if ( status != HttpURLConnection.HTTP_OK ) {
                throw new OlsUnavailableException( "OLS returned HTTP " + status + " for IRI " + iri );
            }
            JsonNode root;
            try ( InputStream is = conn.getInputStream() ) {
                root = objectMapper.readTree( is );
            }
            return parseTerm( root, iri );
        } catch ( IOException e ) {
            throw new OlsUnavailableException( "Failed to reach OLS for IRI " + iri, e );
        } finally {
            if ( conn != null ) {
                conn.disconnect();
            }
        }
    }

    /**
     * Extract the resolved term from an OLS {@code /api/terms} response. When OLS returns the same IRI from
     * several ontologies, the term flagged {@code is_defining_ontology} is preferred; otherwise the first is
     * taken (labels agree across ontologies for a shared IRI). Package-visible for fixture-based testing.
     *
     * @return an {@link OlsTerm} with the resolved label, or {@code null} when the response carries no
     *         labelled term for the IRI.
     */
    @Nullable
    static OlsTerm parseTerm( JsonNode root, String iri ) {
        JsonNode terms = root.path( "_embedded" ).path( "terms" );
        if ( !terms.isArray() || terms.isEmpty() ) {
            return null;
        }
        JsonNode chosen = null;
        for ( JsonNode term : terms ) {
            if ( term.path( "is_defining_ontology" ).asBoolean( false ) ) {
                chosen = term;
                break;
            }
        }
        if ( chosen == null ) {
            chosen = terms.get( 0 );
        }
        JsonNode labelNode = chosen.path( "label" );
        if ( labelNode.isMissingNode() || labelNode.isNull() ) {
            return null;
        }
        String label = labelNode.asText();
        if ( StringUtils.isBlank( label ) ) {
            return null;
        }
        return new OlsTerm( iri, label );
    }
}

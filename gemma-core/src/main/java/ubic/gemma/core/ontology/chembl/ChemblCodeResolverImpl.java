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
 */
package ubic.gemma.core.ontology.chembl;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a trial code against ChEMBL's synonym table
 * ({@code GET {base}/chembl/api/data/molecule?molecule_synonyms__molecule_synonym__iexact=...}).
 *
 * <p>Both positive and negative identifications are cached in the admin-evictable
 * {@link #CACHE_NAME} cache, so a repeatedly-submitted code — recognised or not — is sent upstream
 * once. Transient failures are not cached and are never surfaced as errors; see
 * {@link ChemblCodeResolver#identify}.</p>
 *
 * @see ChemblCodeResolver for why the fuzzy search endpoint is deliberately not used
 */
@Slf4j
@Component
public class ChemblCodeResolverImpl implements ChemblCodeResolver {

    /** Admin-evictable cache of code → {@link ChemblCompound}. Registered in {@code EhcacheConfig#APP_CACHES}. */
    public static final String CACHE_NAME = "ChemblCodeResolver.compounds";

    private static final String USER_AGENT = "Gemma/2.0 (https://gemma.msl.ubc.ca; mailto:pavlab-support@msl.ubc.ca)";

    /** Splits a code into its alphabetic prefix and its numeric remainder, for spelling variants. */
    private static final Pattern PREFIX_AND_NUMBER = Pattern.compile( "^([A-Za-z]{1,5})[- ]?([0-9].*)$" );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemma.chembl.baseurl}")
    private String baseUrl;

    @Value("${gemma.chembl.timeout.ms}")
    private int timeoutMs;

    @Value("${gemma.chembl.enabled}")
    private boolean enabled;

    @Autowired
    private CacheManager cacheManager;

    @Nullable
    private Cache cache;

    /** Resolved once and reused; the release only changes when EBI ships a new ChEMBL. */
    @Nullable
    private volatile String release;

    /**
     * Lazily resolve the cache: the {@link CacheManager} is not reliably populated at construction
     * time on some Spring boot orderings, so look it up on first use.
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
    public ChemblCompound identify( String code ) {
        if ( !enabled || StringUtils.isBlank( code ) ) {
            return null;
        }
        String key = code.trim().toLowerCase( Locale.ROOT );
        Cache c = getCache();
        if ( c != null ) {
            Cache.ValueWrapper wrapper = c.get( key );
            if ( wrapper != null ) {
                ChemblCompound cached = ( ChemblCompound ) wrapper.get();
                return cached != null && cached.isFound() ? cached : null;
            }
        }
        ChemblCompound resolved;
        try {
            resolved = query( code.trim() );
        } catch ( IOException e ) {
            // Advisory enrichment on a search path: a naming authority being down must not fail a
            // user's query, and must not be cached as "no such compound" either.
            log.debug( "ChEMBL unreachable while identifying '{}'; reporting unidentified", code, e );
            return null;
        }
        if ( c != null ) {
            c.put( key, resolved != null ? resolved : ChemblCompound.notFound( code ) );
        }
        return resolved;
    }

    /**
     * Try each plausible spelling of the code against the exact-synonym filter, stopping at the
     * first hit. ChEMBL stores {@code WP 1066} spaced and {@code LLY-283} hyphenated, and the
     * submitter writes whichever they please, so the separator is varied rather than assumed.
     */
    @Nullable
    private ChemblCompound query( String code ) throws IOException {
        for ( String variant : spellingVariants( code ) ) {
            ChemblCompound hit = queryExactSynonym( code, variant );
            if ( hit != null ) {
                return hit;
            }
        }
        return null;
    }

    @Nullable
    private ChemblCompound queryExactSynonym( String code, String variant ) throws IOException {
        String url = baseUrl + "/chembl/api/data/molecule?format=json&limit=1"
                + "&molecule_synonyms__molecule_synonym__iexact="
                + URLEncoder.encode( variant, StandardCharsets.UTF_8 );
        JsonNode root = getJson( url );
        if ( root == null ) {
            return null;
        }
        JsonNode molecules = root.path( "molecules" );
        if ( !molecules.isArray() || molecules.isEmpty() ) {
            return null;
        }
        JsonNode molecule = molecules.get( 0 );
        String chemblId = text( molecule, "molecule_chembl_id" );
        if ( chemblId == null ) {
            return null;
        }
        return new ChemblCompound( code, chemblId, text( molecule, "pref_name" ), variant, getRelease() );
    }

    /**
     * Spelling variants of a code, most faithful first. Deduplicated case-insensitively so a code
     * with no separator does not get queried several times over.
     */
    static List<String> spellingVariants( String code ) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for ( String candidate : buildVariants( code ) ) {
            if ( StringUtils.isNotBlank( candidate ) && seen.add( candidate.toLowerCase( Locale.ROOT ) ) ) {
                out.add( candidate );
            }
        }
        return out;
    }

    private static List<String> buildVariants( String code ) {
        List<String> out = new ArrayList<>();
        out.add( code );
        out.add( code.replace( '-', ' ' ) );
        out.add( code.replace( "-", "" ) );
        out.add( code.replace( " ", "" ) );
        out.add( code.replace( ' ', '-' ) );
        Matcher m = PREFIX_AND_NUMBER.matcher( code );
        if ( m.matches() ) {
            out.add( m.group( 1 ) + "-" + m.group( 2 ) );
            out.add( m.group( 1 ) + " " + m.group( 2 ) );
            out.add( m.group( 1 ) + m.group( 2 ) );
        }
        return out;
    }

    /** ChEMBL's current release, for provenance. Best-effort: absent provenance beats a failed query. */
    @Nullable
    private String getRelease() {
        String r = release;
        if ( r != null ) {
            return r;
        }
        try {
            JsonNode root = getJson( baseUrl + "/chembl/api/data/status.json" );
            if ( root != null ) {
                r = text( root, "chembl_db_version" );
                release = r;
                return r;
            }
        } catch ( IOException e ) {
            log.debug( "could not read ChEMBL release for provenance", e );
        }
        return null;
    }

    @Nullable
    private JsonNode getJson( String url ) throws IOException {
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
                throw new IOException( "ChEMBL returned HTTP " + status + " for " + url );
            }
            try ( InputStream is = conn.getInputStream() ) {
                return objectMapper.readTree( is );
            }
        } finally {
            if ( conn != null ) {
                conn.disconnect();
            }
        }
    }

    @Nullable
    private static String text( JsonNode node, String field ) {
        JsonNode n = node.path( field );
        if ( n.isMissingNode() || n.isNull() ) {
            return null;
        }
        String s = n.asText();
        return StringUtils.isBlank( s ) ? null : s;
    }
}

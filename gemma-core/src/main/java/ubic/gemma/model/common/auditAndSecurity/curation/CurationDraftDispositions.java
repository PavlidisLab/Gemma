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
package ubic.gemma.model.common.auditAndSecurity.curation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Derive per-element {@link Disposition} for a {@link CurationDraft} by
 * diffing {@link CurationDraft#getPayloadJson()} against
 * {@link CurationDraft#getProposalSnapshotJson()} and consulting
 * {@link CurationDraft#getParkedElements()}.
 *
 * <p>The diff is shallow: payload and snapshot are expected to be JSON
 * objects whose top-level keys are opaque element identifiers (e.g.
 * {@code "factor:42:0"}, {@code "tag:42:3"}) and whose values are the
 * element contents. Elements appearing only in payload are NEW (not a
 * disposition signal — skipped). Elements appearing only in snapshot are
 * REJECTED. Elements in both with equal content are RETAINED; with
 * different content, EDITED. The PARKED list overrides everything: any
 * key in the parkedElements array gets {@code PARKED} regardless of the
 * diff.</p>
 *
 * <p>This class is pure — no Spring, no DB, no transactions. It's safe to
 * call from REST handlers or from unit tests.</p>
 */
public final class CurationDraftDispositions {

    /** Result of a single-element diff. */
    public enum Disposition {
        /** Payload still carries the proposal element verbatim. */
        RETAINED,
        /** Payload has the key but with different content. */
        EDITED,
        /** Snapshot had the key but payload dropped it. */
        REJECTED,
        /** Curator parked the element; overrides any other classification. */
        PARKED
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CurationDraftDispositions() {
        // utility
    }

    /**
     * Derive dispositions for every element the draft sees.
     *
     * @return a map keyed by element identifier; entries with no useful
     *         signal (new-in-payload only) are omitted. Returns an empty
     *         map when the draft has no snapshot (i.e. wasn't seeded from
     *         a proposal).
     */
    public static Map<String, Disposition> derive( CurationDraft draft ) {
        if ( draft == null ) {
            return Collections.emptyMap();
        }
        Set<String> parked = parseKeyArray( draft.getParkedElements() );
        Map<String, JsonNode> snapshot = parseObject( draft.getProposalSnapshotJson() );
        Map<String, JsonNode> payload = parseObject( draft.getPayloadJson() );
        // No snapshot => no diff baseline; the only signal we can emit is
        // PARKED. Return only those (callers can still infer "still pending"
        // by absence from the map).
        Map<String, Disposition> out = new LinkedHashMap<>();
        if ( snapshot.isEmpty() && payload.isEmpty() ) {
            for ( String k : parked ) {
                out.put( k, Disposition.PARKED );
            }
            return out;
        }
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll( snapshot.keySet() );
        allKeys.addAll( payload.keySet() );
        for ( String key : allKeys ) {
            if ( parked.contains( key ) ) {
                out.put( key, Disposition.PARKED );
                continue;
            }
            boolean inSnap = snapshot.containsKey( key );
            boolean inPayload = payload.containsKey( key );
            if ( inSnap && inPayload ) {
                JsonNode a = snapshot.get( key );
                JsonNode b = payload.get( key );
                if ( a == null ? b == null : a.equals( b ) ) {
                    out.put( key, Disposition.RETAINED );
                } else {
                    out.put( key, Disposition.EDITED );
                }
            } else if ( inSnap ) {
                out.put( key, Disposition.REJECTED );
            }
            // else: only-in-payload (new element); not a disposition signal.
        }
        // Parked entries that don't appear in either side (the curator parked
        // an element that has since been removed from both views) still get
        // listed so the UI can show them as parked rather than vanished.
        for ( String k : parked ) {
            out.putIfAbsent( k, Disposition.PARKED );
        }
        return out;
    }

    private static Set<String> parseKeyArray( String json ) {
        if ( json == null || json.isBlank() ) return Collections.emptySet();
        try {
            JsonNode node = MAPPER.readTree( json );
            if ( node == null || !node.isArray() ) return Collections.emptySet();
            Set<String> out = new HashSet<>();
            for ( JsonNode el : node ) {
                if ( el != null && el.isTextual() ) {
                    out.add( el.asText() );
                }
            }
            return out;
        } catch ( IOException e ) {
            return Collections.emptySet();
        }
    }

    private static Map<String, JsonNode> parseObject( String json ) {
        if ( json == null || json.isBlank() ) return Collections.emptyMap();
        try {
            JsonNode root = MAPPER.readTree( json );
            if ( root == null || !root.isObject() ) return Collections.emptyMap();
            Map<String, JsonNode> out = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while ( it.hasNext() ) {
                Map.Entry<String, JsonNode> e = it.next();
                out.put( e.getKey(), e.getValue() );
            }
            return out;
        } catch ( IOException e ) {
            return Collections.emptyMap();
        }
    }
}

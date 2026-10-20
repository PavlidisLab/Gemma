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

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Derive per-element {@link Disposition} for a {@code DRAFT}
 * {@link AnnotationSet} by diffing its payload against the {@code PROPOSAL}
 * it was seeded from, and consulting {@link AnnotationSet#getParkedElements()}.
 *
 * <p>The diff is shallow: both payloads are expected to be JSON objects whose
 * top-level keys are opaque element identifiers (e.g. {@code "factor:42:0"},
 * {@code "tag:42:3"}) and whose values are the element contents. Elements
 * appearing only in the draft are NEW (not a disposition signal — skipped).
 * Elements appearing only in the proposal are {@code REJECTED}. Elements in
 * both with equal content are {@code RETAINED}; with different content,
 * {@code EDITED}. The parked list overrides everything: any key named in
 * {@code parkedElements} gets {@code PARKED} regardless of the diff.</p>
 *
 * <p><b>Dispositions are derived, never stored.</b> Accept, edit and reject
 * all fall out of one comparison, so there is no per-element status column to
 * drift out of sync with the payload it describes. What the curator
 * deliberately set aside is the one thing the diff cannot see — a parked
 * element leaves the payload unchanged, exactly like an untouched one — which
 * is why {@code parkedElements} needs explicit storage and the rest does not.
 * See {@code AnnotationSet#getParkedElements()}.</p>
 *
 * <p>🛑 This is per-element bookkeeping within one draft, not the per-finding
 * audit disposition ({@code pending | accepted | dismissed |
 * needs_more_info}) the curation-agents side stores, nor
 * {@link AnnotationSetTriage}. Three different questions at three scopes:
 * what did the curator do to this element, what did they rule on that
 * finding, and how much does this whole set matter.</p>
 *
 * <p>This class is pure — no Spring, no DB, no transactions. It is safe to
 * call from REST handlers or from unit tests. The string overload exists so a
 * caller that already holds both payloads does not have to reach through a
 * lazy {@code parent} association to use it.</p>
 *
 * <p>History: this existed before, keyed to the retired {@code CurationDraft}
 * entity and its embedded {@code proposalSnapshotJson} copy, and was deleted
 * in {@code fa4363e24f} when {@code AgentProposal} / {@code CurationDraft}
 * were folded into {@link AnnotationSet}. The lineage is now the
 * {@link AnnotationSet#getParent() parent} pointer rather than an embedded
 * copy, so the proposal payload is read through it.</p>
 */
public final class CurationDraftDispositions {

    /** Result of a single-element diff. */
    public enum Disposition {
        /** Draft still carries the proposal's element verbatim. */
        RETAINED,
        /** Draft has the key but with different content. */
        EDITED,
        /** The proposal had the key but the draft dropped it. */
        REJECTED,
        /** Curator parked the element; overrides any other classification. */
        PARKED
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CurationDraftDispositions() {
        // utility
    }

    /**
     * Derive dispositions for a draft, reading the proposal payload through
     * the draft's {@link AnnotationSet#getParent() parent}.
     * <p>
     * ⚠️ {@code parent} is a {@code LAZY} association. Call this inside the
     * transaction that loaded the draft, or use
     * {@link #derive(String, String, String)} with a payload the caller
     * already has.
     *
     * @return a map keyed by element identifier; entries with no useful
     *         signal (new-in-draft only) are omitted. Empty when the draft is
     *         null or was not seeded from a proposal.
     */
    public static Map<String, Disposition> derive( @Nullable AnnotationSet draft ) {
        if ( draft == null ) {
            return Collections.emptyMap();
        }
        AnnotationSet parent = draft.getParent();
        return derive( parent != null ? parent.getPayloadJson() : null,
                draft.getPayloadJson(), draft.getParkedElements() );
    }

    /**
     * Derive dispositions from the three payloads directly.
     *
     * @param proposalPayloadJson the {@code PROPOSAL} payload the draft was
     *                            seeded from; null / blank means there is no
     *                            diff baseline, in which case only parked
     *                            elements can be reported
     * @param draftPayloadJson    the draft's current payload
     * @param parkedElements      JSON array of parked element keys
     */
    public static Map<String, Disposition> derive( @Nullable String proposalPayloadJson,
            @Nullable String draftPayloadJson, @Nullable String parkedElements ) {
        Set<String> parked = parseKeyArray( parkedElements );
        Map<String, JsonNode> proposal = parseObject( proposalPayloadJson );
        Map<String, JsonNode> draft = parseObject( draftPayloadJson );
        Map<String, Disposition> out = new LinkedHashMap<>();
        // No baseline on either side => the only signal available is PARKED.
        // Callers infer "still pending" from absence, so returning the parked
        // keys alone is complete rather than partial.
        if ( proposal.isEmpty() && draft.isEmpty() ) {
            for ( String k : parked ) {
                out.put( k, Disposition.PARKED );
            }
            return out;
        }
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll( proposal.keySet() );
        allKeys.addAll( draft.keySet() );
        for ( String key : allKeys ) {
            if ( parked.contains( key ) ) {
                out.put( key, Disposition.PARKED );
                continue;
            }
            boolean inProposal = proposal.containsKey( key );
            boolean inDraft = draft.containsKey( key );
            if ( inProposal && inDraft ) {
                JsonNode a = proposal.get( key );
                JsonNode b = draft.get( key );
                if ( a == null ? b == null : a.equals( b ) ) {
                    out.put( key, Disposition.RETAINED );
                } else {
                    out.put( key, Disposition.EDITED );
                }
            } else if ( inProposal ) {
                out.put( key, Disposition.REJECTED );
            }
            // else: only-in-draft (new element); not a disposition signal.
        }
        // A parked element that has since vanished from both sides is still
        // listed, so the UI shows it as parked rather than as gone.
        for ( String k : parked ) {
            out.putIfAbsent( k, Disposition.PARKED );
        }
        return out;
    }

    private static Set<String> parseKeyArray( @Nullable String json ) {
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

    private static Map<String, JsonNode> parseObject( @Nullable String json ) {
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

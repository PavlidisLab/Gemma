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
import org.springframework.lang.Nullable;

/**
 * <h2>How many factors and tags an annotation-set payload proposes.</h2>
 *
 * <p>What an inbox card prints. The cross-corpus list serves a thin projection with no
 * {@code payloadJson}, so before these were stored, drawing that list cost one whole-set fetch per
 * row — 94 KB on the set uib sampled, scaling with the corpus rather than with the screen.</p>
 *
 * <h2>⚠️ A hint, not a contract</h2>
 *
 * <p>{@link AnnotationSet#getPayloadJson()} belongs to its producer — the curation-agents client for
 * an {@code AGENT} row, the curation UI for a {@code CURATOR} one — and Gemma persists it verbatim
 * and serves it unread. Counting inside it is the one place that rule is bent, and it is bent as far
 * as it has to go and no further: the counts are derived once when the row is written, they are
 * advisory, and <b>anything unrecognized yields {@code null} rather than a guess</b>.</p>
 *
 * <p>🛑 <b>{@code null} means UNKNOWN, never zero.</b> A payload that carries no factors reports
 * {@code 0}; a payload this cannot read reports {@code null}. Collapsing the two would let a shape
 * change downstream read as "this proposal changes nothing", which is the one wrong answer that
 * looks plausible.</p>
 *
 * <p>The shape read is the {@code CurationDocument} both sides already speak — Gemma's own snapshot
 * writer emits it and CAB's {@code curation_commit.py} mirrors it. If a producer moves, this returns
 * null and the card loses two numbers; nothing else breaks, and no stored payload is altered.</p>
 */
public final class AnnotationSetPayloadCounts {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nullable
    private final Integer factorCount;
    @Nullable
    private final Integer tagCount;

    private AnnotationSetPayloadCounts( @Nullable Integer factorCount, @Nullable Integer tagCount ) {
        this.factorCount = factorCount;
        this.tagCount = tagCount;
    }

    /** Both counts unknown — what an unparseable, absent or unrecognized payload yields. */
    public static AnnotationSetPayloadCounts unknown() {
        return new AnnotationSetPayloadCounts( null, null );
    }

    /**
     * Read the counts off a payload, best-effort.
     *
     * @param payloadJson the stored payload, or {@code null}
     * @return the counts; either or both {@code null} when that part of the shape was not found.
     *         Never throws — a payload Gemma cannot read is not an error, because Gemma never
     *         promised to be able to read it.
     */
    public static AnnotationSetPayloadCounts of( @Nullable String payloadJson ) {
        if ( payloadJson == null || payloadJson.isEmpty() ) {
            return unknown();
        }
        JsonNode root;
        try {
            root = MAPPER.readTree( payloadJson );
        } catch ( Exception e ) {
            return unknown();
        }
        if ( root == null || !root.isObject() ) {
            return unknown();
        }
        return new AnnotationSetPayloadCounts(
                sizeAt( root, "design", "factors", "items" ),
                sizeAt( root, "tags", "items" ) );
    }

    /**
     * The length of the array at a path, or {@code null} if any step is missing or the leaf is not
     * an array. Deliberately does not treat a missing step as an empty array — see the null-means-
     * unknown rule in the class comment.
     */
    @Nullable
    private static Integer sizeAt( JsonNode root, String... path ) {
        JsonNode n = root;
        for ( String step : path ) {
            n = n.get( step );
            if ( n == null ) {
                return null;
            }
        }
        return n.isArray() ? n.size() : null;
    }

    @Nullable
    public Integer getFactorCount() {
        return factorCount;
    }

    @Nullable
    public Integer getTagCount() {
        return tagCount;
    }
}

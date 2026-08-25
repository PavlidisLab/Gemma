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

import javax.annotation.Nullable;

/**
 * The triage verdict on an {@link AnnotationSet}: how much this set matters,
 * and therefore whether anyone needs to act on it.
 *
 * <p>Written first by the producing side as its QC verdict, then overwritten
 * by a curator who disagrees. {@link AnnotationSet#getTriagedBy()} names
 * whoever set the value that is stored; the prior value is recoverable from
 * the audit note. One value, not a machine column beside a human one — if the
 * disagreement rate turns out to be worth querying, that is a second column
 * later and not a redesign.</p>
 *
 * <p>🛑 <b>This is not the per-finding disposition.</b> The curation-agents
 * side stores {@code pending | accepted | dismissed | needs_more_info} per
 * finding, keyed by {@code target_id}, in the local store's
 * {@code curation_review.body_json}. That answers "do I agree with finding
 * #7". This answers "how much does this whole set matter". A finding can be
 * {@code dismissed} inside a set triaged {@link #MustFix}. The two vocabularies
 * both contain the words "won't fix"; they are not the same field and must not
 * be merged.</p>
 *
 * <p>🛑 It is also not {@link CurationDraftDispositions.Disposition}, which is
 * per-element within one draft and is derived rather than stored.</p>
 *
 * <p>Persisted as the enum {@link #name()}. {@code NULL} in the column means
 * {@link #Pending} — not yet triaged, which is deliberately distinguishable
 * from {@link #Fine} (looked at, nothing to do).</p>
 */
public enum AnnotationSetTriage {

    /** Nobody has ruled on this set yet. The stored form is {@code NULL}. */
    Pending,

    /** Looked at; there is nothing to do. */
    Fine,

    /** There is something here, and we are deliberately not acting on it. */
    WontFix,

    /** Worth doing eventually; not now. */
    MightFix,

    /** Act on this. */
    MustFix;

    /**
     * @return the lowercase external form for JSON / wire surfaces, matching
     *         the convention {@link AnnotationSetRole#getDbValue()} set.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the external form, accepting either case and the snake_case
     * spellings the curation-agents side emits ({@code wont_fix},
     * {@code might_fix}, {@code must_fix}).
     *
     * @param v the wire value; {@code null} or blank yields {@link #Pending}
     * @throws IllegalArgumentException if {@code v} names no triage value
     */
    public static AnnotationSetTriage fromDbValue( @Nullable String v ) {
        if ( v == null || v.isBlank() ) {
            return Pending;
        }
        String normalized = v.replace( "_", "" ).replace( "-", "" );
        for ( AnnotationSetTriage t : values() ) {
            if ( t.name().equalsIgnoreCase( normalized ) ) {
                return t;
            }
        }
        throw new IllegalArgumentException( "Unknown triage value '" + v
                + "'; expected one of pending, fine, wont_fix, might_fix, must_fix." );
    }
}

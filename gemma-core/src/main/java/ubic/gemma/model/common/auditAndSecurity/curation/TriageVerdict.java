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
 * One judge's ruling on how much an {@link AnnotationSet} matters.
 *
 * <p>There is no {@code Pending} value: a set nobody has ruled on carries no
 * {@link AnnotationSetTriage} row, and absence is the state. Storing "pending"
 * would give the queue two spellings of the same thing and force an
 * {@code OR} into every query on it.</p>
 *
 * <p>🛑 <b>Not the per-finding disposition.</b> The curation-agents side
 * stores {@code pending | accepted | dismissed | needs_more_info} per finding,
 * keyed by {@code target_id}. That answers "do I agree with finding #7"; this
 * answers "how much does this whole set matter". A finding can be dismissed
 * inside a set ruled {@link #MustFix}. Both vocabularies contain a "won't
 * fix"; they are different fields.</p>
 *
 * <p>🛑 Nor is it {@link CurationDraftDispositions.Disposition}, which is
 * per-element within one draft and is derived rather than stored. Set,
 * finding and element are three scopes.</p>
 */
public enum TriageVerdict {

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
     * Parse the external form, accepting either case and the snake_case and
     * kebab-case spellings other producers emit ({@code wont_fix},
     * {@code might-fix}).
     *
     * @throws IllegalArgumentException if {@code v} names no verdict. Null and
     *                                  blank throw too: "no verdict" is the
     *                                  absence of a row, so a caller that
     *                                  reaches here with nothing has a bug
     *                                  rather than a default.
     */
    public static TriageVerdict fromDbValue( @Nullable String v ) {
        if ( v != null ) {
            String normalized = v.replace( "_", "" ).replace( "-", "" );
            for ( TriageVerdict t : values() ) {
                if ( t.name().equalsIgnoreCase( normalized ) ) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException( "Unknown triage verdict '" + v
                + "'; expected one of fine, wont_fix, might_fix, must_fix."
                + " There is no 'pending' verdict -- an un-triaged set has no triage row." );
    }
}

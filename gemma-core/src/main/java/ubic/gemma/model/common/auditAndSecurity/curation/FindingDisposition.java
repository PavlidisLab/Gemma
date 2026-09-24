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
 * <h2>A curator's ruling on ONE finding inside an audit.</h2>
 *
 * <p>🛑 <b>Not {@link TriageVerdict}.</b> That answers how much a whole
 * {@link AnnotationSet} matters; this answers whether the curator agrees with
 * finding #7 inside it. A finding can be {@link #DISMISSED} inside a set ruled
 * {@link TriageVerdict#MustFix}. Both vocabularies read like verdicts and they
 * are different fields — the distinction {@code V30__annotation_set_triage.sql}
 * spelled out, and this enum is the other half of it.</p>
 *
 * <p><b>Per-finding rather than per-set is a constraint, not a preference</b>
 * (uib, 2026-09-03): a curator routinely accepts one finding and rejects
 * another on the same target, and one set-level verdict cannot carry that
 * outcome.</p>
 *
 * <p><b>The three values are the curation store's own, adopted rather than
 * re-invented</b> — {@code accepted | dismissed | needs_more_info}, as filed by
 * the producing side and recorded in {@code V30__annotation_set_triage.sql}.
 * Gemma spelling a fourth value or a different word here would split one
 * vocabulary across two systems whose rows have to be compared.</p>
 *
 * <p>There is no {@code PENDING} value, for the same reason
 * {@link TriageVerdict} has none: a finding nobody has ruled on carries no
 * disposition row, and absence is the state. A stored pending would be a
 * second spelling of the same thing and would put an {@code OR} into every
 * query over an audit's outstanding work.</p>
 */
public enum FindingDisposition {

    /** The curator agrees; the finding should be acted on. */
    ACCEPTED,

    /** The curator disagrees; the finding is wrong or does not matter. */
    DISMISSED,

    /**
     * Neither yet — ruling on this finding is blocked until someone supplies
     * something the finding does not carry.
     * <p>
     * Distinct from having no row at all: absence means nobody has looked,
     * this means someone looked and could not decide on what was in front of
     * them.
     */
    NEEDS_MORE_INFO;

    /**
     * @return the snake_case external form for JSON / wire surfaces —
     *         {@code accepted}, {@code dismissed}, {@code needs_more_info}.
     *         The underscore is in {@link #name()} already, so unlike
     *         {@link TriageVerdict#getDbValue()} no word-boundary insertion is
     *         needed.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the external form, accepting either case and the snake_case and
     * kebab-case spellings other producers emit ({@code needs_more_info},
     * {@code needs-more-info}).
     *
     * @param v the external spelling
     * @return the disposition it names
     * @throws IllegalArgumentException if {@code v} names no disposition. Null
     *                                  and blank throw too: "no disposition"
     *                                  is the absence of a row, so a caller
     *                                  reaching here with nothing has a bug
     *                                  rather than a default.
     */
    public static FindingDisposition fromDbValue( @Nullable String v ) {
        if ( v != null ) {
            String normalized = squash( v );
            for ( FindingDisposition d : values() ) {
                if ( squash( d.name() ).equalsIgnoreCase( normalized ) ) {
                    return d;
                }
            }
        }
        throw new IllegalArgumentException( "Unknown finding disposition '" + v
                + "'; expected one of accepted, dismissed, needs_more_info."
                + " There is no 'pending' disposition -- an un-ruled finding has no row." );
    }

    /**
     * Strip the separators so the comparison is on letters alone — applied to
     * BOTH sides, because {@link #NEEDS_MORE_INFO}'s own {@link #name()}
     * carries underscores. Comparing a squashed input against a raw name would
     * make the one multi-word value in this enum the one value that cannot be
     * parsed.
     */
    private static String squash( String v ) {
        return v.replace( "_", "" ).replace( "-", "" );
    }
}

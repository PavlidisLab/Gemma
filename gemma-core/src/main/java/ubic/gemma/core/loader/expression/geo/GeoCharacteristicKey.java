/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.loader.expression.geo;

/**
 * Where a GEO {@code "key: value"} characteristic actually divides.
 *
 * <p>🛑 <b>One place, because there were two and they disagreed silently.</b>
 * {@code GeoConverterImpl} split a characteristic into CATEGORY/VALUE and
 * {@code GeoSourceMetadataBuilder} split the same string into a blob key, each with its own
 * {@code indexOf(':')}. Both cut at the FIRST colon, so a value containing one was divided inside
 * itself, and the two produced separately-wrong copies of the same mistake &mdash; the stored
 * characteristic {@code CATEGORY "cervical cancer (post chemotherapy"}, and the blob key
 * {@code "gender (m"} on GSE205450 (frinkbro, 2026-09-11). A second copy of a parse is a second
 * place for it to be wrong; put the rule here and call it.</p>
 *
 * <h2>The rule</h2>
 *
 * <p>A category is a short label the submitter chose, so the separating colon is the first one whose
 * key half stands on its own &mdash; brackets balanced. An unbalanced {@code (} means the colon was
 * inside a parenthesised aside, which is frinkbro's signature for the defect, and scanning on to the
 * next candidate rather than giving up recovers the real key:
 * {@code "cervical cancer (post chemotherapy: TP 2cycle): IIB"} divides at the second colon.</p>
 *
 * <p>⚠️ Deliberately NOT rejecting a key containing {@code =}. Legend keys legitimately carry them
 * &mdash; {@code "lithium use (non-user=0, user = 1)"} is a real category pinned by
 * {@code GeoCharacteristicParseTest} &mdash; and rejecting it sent the string on to the {@code =}
 * split, which truncated it at the first {@code =}. That traded one bug for another; bracket balance
 * alone covers the legend family.</p>
 *
 * @author gembro
 */
public final class GeoCharacteristicKey {

    /**
     * Divide at the separating colon, which is not always the first.
     *
     * @return two elements when a separating colon was found, otherwise one holding the input
     */
    public static String[] split( String field ) {
        for ( int i = field.indexOf( ':' ); i >= 0; i = field.indexOf( ':', i + 1 ) ) {
            if ( isPlausibleKey( field.substring( 0, i ) ) ) {
                return new String[] { field.substring( 0, i ), field.substring( i + 1 ) };
            }
        }
        return new String[] { field };
    }

    /**
     * Whether a candidate key half looks like something a submitter would write as a key, rather than
     * the front of a value that happened to contain a colon.
     */
    private static boolean isPlausibleKey( String candidate ) {
        int depth = 0;
        for ( int i = 0; i < candidate.length(); i++ ) {
            char c = candidate.charAt( i );
            if ( c == '(' || c == '[' ) {
                depth++;
            } else if ( c == ')' || c == ']' ) {
                depth--;
            }
        }
        return depth == 0;
    }

    private GeoCharacteristicKey() {
    }
}

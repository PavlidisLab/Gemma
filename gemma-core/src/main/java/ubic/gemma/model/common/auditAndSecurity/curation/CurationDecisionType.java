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
 * Which way a standing {@link CurationDecision} went.
 *
 * <p>The two are one field rather than two tables because an approval of an
 * edit that never landed and a refusal of one that was proposed are the same
 * shape with the polarity flipped: a decision a curator made, about a change,
 * that produced no artifact to hang it off.</p>
 */
public enum CurationDecisionType {

    /**
     * Do not make this change. The whole content of the decision is the "no" —
     * there is nothing to commit, and the record exists so the same edit is
     * not proposed again.
     */
    REFUSED,

    /**
     * This change is agreed to. Recorded only where the agreement itself has
     * to survive without a commit carrying it; where curation happens directly
     * in Gemma the commit IS the approval and no row belongs here.
     */
    ALLOWED;

    /**
     * @return the lowercase external form for JSON / wire surfaces.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the external form, accepting either case.
     *
     * @param v the external spelling
     * @return the decision it names
     * @throws IllegalArgumentException if {@code v} names no decision
     */
    public static CurationDecisionType fromDbValue( @Nullable String v ) {
        if ( v != null ) {
            for ( CurationDecisionType t : values() ) {
                if ( t.name().equalsIgnoreCase( v.trim() ) ) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException( "Unknown curation decision '" + v
                + "'; expected refused or allowed." );
    }
}

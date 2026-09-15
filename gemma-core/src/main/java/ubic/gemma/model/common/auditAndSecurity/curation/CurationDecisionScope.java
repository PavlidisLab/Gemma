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
 * How wide a {@link CurationDecision} reaches — the difference between the
 * refusal verbs the curation ledger actually uses.
 */
public enum CurationDecisionScope {

    /**
     * One thing: this tag, this value, this deletion. The narrowest ruling,
     * and the one {@code reject_add} and {@code reject_drop} make.
     */
    ITEM,

    /**
     * Everything under one key, INCLUDING siblings that have not been proposed
     * yet. What {@code reject_factor} means: the ruling gates the whole key,
     * not the one factor that happened to be in front of the curator.
     */
    KEY,

    /**
     * A whole proposal at once — {@code reject_all}. Names the annotation set
     * it answered rather than a key.
     */
    PROPOSAL;

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
     * @return the scope it names
     * @throws IllegalArgumentException if {@code v} names no scope
     */
    public static CurationDecisionScope fromDbValue( @Nullable String v ) {
        if ( v != null ) {
            for ( CurationDecisionScope s : values() ) {
                if ( s.name().equalsIgnoreCase( v.trim() ) ) {
                    return s;
                }
            }
        }
        throw new IllegalArgumentException( "Unknown curation decision scope '" + v
                + "'; expected item, key or proposal." );
    }
}

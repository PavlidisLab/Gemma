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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The wire form of {@link TriageVerdict}.
 * <p>
 * Worth its own test because the emitter and the parser are separate code and
 * drifted apart once already: {@code name().toLowerCase()} emitted
 * {@code wontfix} while {@link TriageVerdict#fromDbValue} accepted
 * {@code wont_fix}. A client that echoes back whatever it was handed keeps
 * working through that mismatch, so nothing fails until someone compares an
 * emitted value against the documented one.
 */
public class TriageVerdictTest {

    @Test
    public void emitsSnakeCase() {
        assertThat( TriageVerdict.Fine.getDbValue() ).isEqualTo( "fine" );
        assertThat( TriageVerdict.WontFix.getDbValue() ).isEqualTo( "wont_fix" );
        assertThat( TriageVerdict.MightFix.getDbValue() ).isEqualTo( "might_fix" );
        assertThat( TriageVerdict.MustFix.getDbValue() ).isEqualTo( "must_fix" );
    }

    @Test
    public void everyValueRoundTrips() {
        for ( TriageVerdict v : TriageVerdict.values() ) {
            assertThat( TriageVerdict.fromDbValue( v.getDbValue() ) )
                    .as( "%s must parse back from its own wire form", v )
                    .isEqualTo( v );
        }
    }

    @Test
    public void acceptsTheSpellingsOtherProducersEmit() {
        assertThat( TriageVerdict.fromDbValue( "WONT_FIX" ) ).isEqualTo( TriageVerdict.WontFix );
        assertThat( TriageVerdict.fromDbValue( "wont-fix" ) ).isEqualTo( TriageVerdict.WontFix );
        assertThat( TriageVerdict.fromDbValue( "wontfix" ) ).isEqualTo( TriageVerdict.WontFix );
        assertThat( TriageVerdict.fromDbValue( "WontFix" ) ).isEqualTo( TriageVerdict.WontFix );
    }

    /**
     * There is no pending verdict — an un-triaged set has no row — so a caller
     * sending one is told rather than quietly given a verdict nobody chose.
     */
    @Test
    public void pendingIsNotAVerdict() {
        assertThatThrownBy( () -> TriageVerdict.fromDbValue( "pending" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "no 'pending' verdict" );
    }

    @Test
    public void nullAndBlankAreRejectedRatherThanDefaulted() {
        assertThatThrownBy( () -> TriageVerdict.fromDbValue( null ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> TriageVerdict.fromDbValue( "  " ) )
                .isInstanceOf( IllegalArgumentException.class );
    }
}

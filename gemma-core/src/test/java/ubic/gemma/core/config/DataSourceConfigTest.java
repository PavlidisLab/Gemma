/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ubic.gemma.core.config.DataSourceConfig.maxExecutionTimeOf;
import static ubic.gemma.core.config.DataSourceConfig.withMaxExecutionTime;

/**
 * Composition of the Connector/J {@code sessionVariables} list.
 *
 * @see DataSourceConfig#withMaxExecutionTime(String, String)
 */
class DataSourceConfigTest {

    /**
     * The shipped value, whose sql_mode carries commas inside its quotes. Connector/J splits the
     * list on commas outside quotes, so what is appended has to survive beside it.
     */
    private static final String SHIPPED_SESSION_VARIABLES =
            "sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'";

    @Test
    void whenNoTimeoutIsConfigured_thenTheSessionVariablesAreUntouched() {
        assertThat( withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, null ) ).isEqualTo( SHIPPED_SESSION_VARIABLES );
        assertThat( withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "" ) ).isEqualTo( SHIPPED_SESSION_VARIABLES );
        assertThat( withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "   " ) ).isEqualTo( SHIPPED_SESSION_VARIABLES );
    }

    @Test
    void whenATimeoutIsConfigured_thenItIsAppendedAndTheSqlModeSurvives() {
        String composed = withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "300000" );
        assertThat( composed )
                .startsWith( SHIPPED_SESSION_VARIABLES )
                .endsWith( ",max_execution_time=300000" );
        // the sql_mode value is one quoted token; appending must not have opened a second one
        assertThat( composed.chars().filter( c -> c == '\'' ).count() ).isEqualTo( 2 );
    }

    @Test
    void whenTheTimeoutIsPadded_thenItIsTrimmed() {
        assertThat( withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, " 300000 " ) )
                .endsWith( ",max_execution_time=300000" );
    }

    /**
     * A unit is a mistake worth catching here: MySQL wants milliseconds, and "300s" or "5 minutes"
     * would otherwise reach the server as an unparseable SET and fail every connection attempt with
     * an error naming sql_mode, which is the part that is correct.
     */
    @Test
    void whenTheTimeoutIsNotANumber_thenTheConfigurationIsRejected() {
        assertThatThrownBy( () -> withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "5 minutes" ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "milliseconds" );
        assertThatThrownBy( () -> withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "300s" ) )
                .isInstanceOf( IllegalStateException.class );
        assertThatThrownBy( () -> withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "-1" ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "negative" );
    }

    /**
     * The reader is the composer read backwards, and the round trip is the guarantee: a deployment
     * asks {@code GET /admin/db/pool} whether the cap is on, and an answer derived by a second,
     * independent parse would eventually disagree with what was actually sent to MySQL.
     */
    @Test
    void whenATimeoutWasComposed_thenItIsReadBack() {
        assertThat( maxExecutionTimeOf( withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "300000" ) ) )
                .isEqualTo( 300000L );
        assertThat( maxExecutionTimeOf( withMaxExecutionTime( SHIPPED_SESSION_VARIABLES, "0" ) ) )
                .isEqualTo( 0L );
    }

    /**
     * No cap composed means no cap reported — including against the shipped list, whose quoted
     * sql_mode carries commas and underscored words of its own.
     */
    @Test
    void whenNoTimeoutWasComposed_thenNothingIsReadBackFromTheQuotedSqlMode() {
        assertThat( maxExecutionTimeOf( SHIPPED_SESSION_VARIABLES ) ).isNull();
        assertThat( maxExecutionTimeOf( null ) ).isNull();
        assertThat( maxExecutionTimeOf( "" ) ).isNull();
    }

    /**
     * A variable whose name merely ENDS with the one being read is a different setting.
     */
    @Test
    void whenAnotherVariableSharesTheSuffix_thenItIsNotMistakenForTheTimeout() {
        assertThat( maxExecutionTimeOf( SHIPPED_SESSION_VARIABLES + ",other_max_execution_time=99" ) ).isNull();
    }
}

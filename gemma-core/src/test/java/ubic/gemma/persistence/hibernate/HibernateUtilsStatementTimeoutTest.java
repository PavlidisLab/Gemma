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
package ubic.gemma.persistence.hibernate;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The SQL {@link HibernateUtils#liftStatementTimeout(Session)} issues, and the cases in which it
 * issues none.
 * <p>
 * These are assertions about statement text and ordering, which is what a mock can see;
 * {@code AbstractDaoStatementTimeoutTest} covers the other half — that MySQL honours the SET, and
 * that the value is really back on the connection afterwards.
 *
 * @see HibernateUtils#liftStatementTimeout(Session)
 */
class HibernateUtilsStatementTimeoutTest {

    private static final String PROBE = "select @@session.max_execution_time";

    private Session session;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws SQLException {
        session = mock( Session.class );
        connection = mock( Connection.class );
        statement = mock( Statement.class );
        resultSet = mock( ResultSet.class );
        DatabaseMetaData metaData = mock( DatabaseMetaData.class );

        // run the Work against our connection, the way Hibernate would
        doAnswer( invocation -> {
            invocation.getArgument( 0, Work.class ).execute( connection );
            return null;
        } ).when( session ).doWork( any() );

        when( connection.getMetaData() ).thenReturn( metaData );
        when( metaData.getDatabaseProductName() ).thenReturn( "MySQL" );
        when( connection.createStatement() ).thenReturn( statement );
        // stubbed on the exact probe: a change to the SQL leaves this unstubbed rather than passing
        when( statement.executeQuery( PROBE ) ).thenReturn( resultSet );
        when( resultSet.next() ).thenReturn( true );
    }

    @Test
    void whenATimeoutIsInForce_thenItIsClearedAndTheExactValueIsPutBack() throws SQLException {
        when( resultSet.getLong( 1 ) ).thenReturn( 4321L );

        Runnable restore = HibernateUtils.liftStatementTimeout( session );

        verify( statement ).execute( "set session max_execution_time = 0" );
        verify( statement, never() ).execute( "set session max_execution_time = 4321" );

        restore.run();

        // the value it had, not a default: a deployment's cap is whatever it configured
        verify( statement ).execute( "set session max_execution_time = 4321" );
    }

    @Test
    void whenNoTimeoutIsInForce_thenNothingIsWritten() throws SQLException {
        when( resultSet.getLong( 1 ) ).thenReturn( 0L );

        Runnable restore = HibernateUtils.liftStatementTimeout( session );
        restore.run();

        verify( statement, never() ).execute( startsWith( "set session" ) );
    }

    /**
     * gemma-cli and the H2-backed tests are not MySQL, and {@code @@session.max_execution_time} does
     * not exist there — the probe has to be skipped rather than issued and caught, because the error
     * would land inside whatever transaction the streamed read is running in.
     */
    @Test
    void whenTheDatabaseIsNotMysql_thenItIsNotEvenProbed() throws SQLException {
        DatabaseMetaData h2 = mock( DatabaseMetaData.class );
        when( h2.getDatabaseProductName() ).thenReturn( "H2" );
        when( connection.getMetaData() ).thenReturn( h2 );

        Runnable restore = HibernateUtils.liftStatementTimeout( session );
        restore.run();

        verify( connection, never() ).createStatement();
    }

    /**
     * A failed probe must not take the read down with it — the caller is about to stream, and running
     * under the timeout is a worse outcome than not streaming at all only if the stream then dies,
     * which is the caller's business, not the probe's.
     */
    @Test
    void whenTheProbeFails_thenTheReadStillProceeds() throws SQLException {
        when( statement.executeQuery( PROBE ) ).thenThrow( new SQLException( "no such variable" ) );

        Runnable restore = HibernateUtils.liftStatementTimeout( session );

        assertThat( restore ).isNotNull();
        assertThatCode( restore::run ).doesNotThrowAnyException();
        verify( statement, never() ).execute( startsWith( "set session" ) );
    }

    /**
     * A restore that fails leaves the connection without its cap, which is worth a warning but not an
     * exception thrown from {@link java.util.stream.Stream#close()}.
     */
    @Test
    void whenTheRestoreFails_thenClosingTheStreamStillSucceeds() throws SQLException {
        when( resultSet.getLong( 1 ) ).thenReturn( 4321L );
        Runnable restore = HibernateUtils.liftStatementTimeout( session );
        when( connection.createStatement() ).thenThrow( new SQLException( "connection is closed" ) );

        assertThatCode( restore::run ).doesNotThrowAnyException();
    }
}

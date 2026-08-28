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
package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The statement timeout ({@code gemma.db.hikari.maxExecutionTime}) and the streamed reads that opt
 * out of it.
 * <p>
 * Runs against MySQL rather than {@code BaseDatabaseTest5}'s in-memory H2, because
 * {@code max_execution_time} is a MySQL session variable and the whole mechanism is invisible
 * anywhere else.
 *
 * @see AbstractDao#streamAll(boolean)
 */
@Transactional
class AbstractDaoStatementTimeoutTest extends BaseIntegrationTest5 {

    /** Any value a deployment would not pick, so a stale one is recognisable. */
    private static final long A_TIMEOUT_MS = 4321L;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private TaxonDao taxonDao;

    /**
     * SET SESSION is not transactional, so the rollback at end-of-test does not undo it and the
     * connection would go back to the pool carrying the timeout.
     */
    @AfterEach
    void clearStatementTimeout() {
        setStatementTimeout( 0 );
    }

    @Test
    void whenATimeoutIsInForce_thenAStreamedReadLiftsItAndRestoresItOnClose() {
        setStatementTimeout( A_TIMEOUT_MS );

        try ( Stream<Taxon> taxa = taxonDao.streamAll( false ) ) {
            assertThat( currentStatementTimeout() )
                    .withFailMessage( "a streamed read has to run without the statement timeout: a stream finishes when its consumer does, so the cap would kill it mid-drain" )
                    .isZero();
            taxa.findFirst();
        }

        assertThat( currentStatementTimeout() )
                .withFailMessage( "the timeout has to come back before the connection returns to the pool, or its next borrower runs unbounded" )
                .isEqualTo( A_TIMEOUT_MS );
    }

    /**
     * The other half of the rule: only streams opt out. Without this, a lift that leaked to every
     * read on the session would still pass the test above.
     */
    @Test
    void whenATimeoutIsInForce_thenAnOrdinaryReadKeepsIt() {
        setStatementTimeout( A_TIMEOUT_MS );

        taxonDao.loadAll();

        assertThat( currentStatementTimeout() ).isEqualTo( A_TIMEOUT_MS );
    }

    /**
     * The default everywhere except a gemma-rest deployment — gemma-cli reads the same
     * {@code default.properties} and ships with no cap, so streaming must not start writing
     * session state on its behalf.
     */
    @Test
    void whenNoTimeoutIsInForce_thenAStreamedReadLeavesTheSessionAlone() {
        setStatementTimeout( 0 );

        try ( Stream<Taxon> taxa = taxonDao.streamAll( false ) ) {
            assertThat( currentStatementTimeout() ).isZero();
            taxa.findFirst();
        }

        assertThat( currentStatementTimeout() ).isZero();
    }

    private void setStatementTimeout( long ms ) {
        sessionFactory.getCurrentSession().doWork( connection -> {
            try ( Statement stmt = connection.createStatement() ) {
                stmt.execute( "set session max_execution_time = " + ms );
            }
        } );
    }

    private long currentStatementTimeout() {
        long[] value = { -1L };
        sessionFactory.getCurrentSession().doWork( connection -> {
            try ( Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery( "select @@session.max_execution_time" ) ) {
                value[0] = rs.next() ? rs.getLong( 1 ) : -1L;
            }
        } );
        return value[0];
    }
}

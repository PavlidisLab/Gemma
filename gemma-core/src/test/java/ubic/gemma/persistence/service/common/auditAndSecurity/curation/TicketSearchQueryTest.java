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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketSearchHitValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketDaoImpl.buildSearchHitHql;
import static ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketDaoImpl.searchLikePattern;

/**
 * Query construction behind {@code GET /tickets/search}: the HQL
 * {@link TicketDaoImpl#buildSearchHitHql} emits, the {@code LIKE} pattern
 * {@link TicketDaoImpl#searchLikePattern} builds, and the row projection
 * {@link TicketSearchHitValueObject#fromRow}.
 * <p>
 * These assert on the generated HQL rather than on rows from a database, which pins the shape of
 * the query — notably that {@code targetCount} is a {@code count()} the database evaluates and that
 * the {@code targets} collection is never named — without needing gemdtest. It does NOT prove the
 * HQL executes; {@code TicketPersistenceIT.search*} does that against a real schema.
 *
 * @author paul
 */
public class TicketSearchQueryTest {

    @Test
    public void titleQuery_matchesACaseInsensitiveSubstringOfTheTitle() {
        String hql = buildSearchHitHql( false, false, false );
        assertThat( hql ).contains( "lower(t.name) like :titleFragment escape '~'" );
        // the id predicate belongs to the other half of the search; it is not ORed in here
        assertThat( hql ).doesNotContain( ":ticketId" );
    }

    @Test
    public void idQuery_matchesTheIdColumnVerbatim() {
        String hql = buildSearchHitHql( true, false, false );
        assertThat( hql ).contains( "t.id = :ticketId" );
        assertThat( hql ).doesNotContain( "like" );
    }

    /**
     * The reason this endpoint exists rather than {@code GET /tickets?query=}: a picker showing 20
     * hits must not fetch 500 target rows for one of them.
     */
    @Test
    public void targetCountIsCountedBySql_andTheTargetsCollectionIsNeverNamed() {
        for ( boolean byId : new boolean[] { true, false } ) {
            String hql = buildSearchHitHql( byId, true, true );
            assertThat( hql )
                    .as( "byId=%s counts targets in SQL", byId )
                    .contains( "(select count(tt.id) from TicketTarget tt where tt.ticket = t)" );
            // t.targets is the collection; naming it at all (select, join, or join fetch) is the
            // bug this endpoint exists to avoid.
            assertThat( hql ).as( "byId=%s does not touch the collection", byId )
                    .doesNotContain( "t.targets" );
            assertThat( hql ).as( "byId=%s fetches nothing", byId )
                    .doesNotContain( "fetch" );
            // scalar projection only: no `select t from Ticket t`, so no entity is hydrated either
            assertThat( hql ).startsWith( "select t.id, t.name, t.state, t.type, " );
        }
    }

    @Test
    public void openOnly_restrictsToTheOpenStates() {
        assertThat( buildSearchHitHql( false, true, false ) ).contains( "and t.state in :openStates" );
        assertThat( buildSearchHitHql( true, true, false ) ).contains( "and t.state in :openStates" );
    }

    @Test
    public void openOnlyFalse_leavesTheStatePredicateOut() {
        assertThat( buildSearchHitHql( false, false, false ) ).doesNotContain( ":openStates" );
        assertThat( buildSearchHitHql( true, false, false ) ).doesNotContain( ":openStates" );
    }

    /**
     * Another curator's scratchpad is not somewhere to file work, so it is not offered; when nobody
     * is identified, no scratchpad is.
     */
    @Test
    public void scratchpads_areExcludedOutrightWhenTheCallerIsUnknown() {
        String hql = buildSearchHitHql( false, true, false );
        assertThat( hql ).contains( "and t.type <> :scratchpadType" );
        assertThat( hql ).doesNotContain( ":scratchpadOwnerId" );
    }

    @Test
    public void scratchpads_areOfferedToTheCuratorWhoReportedThem() {
        String hql = buildSearchHitHql( false, true, true );
        assertThat( hql )
                .contains( "and ( t.type <> :scratchpadType or t.reporter.id = :scratchpadOwnerId )" );
    }

    @Test
    public void titleQuery_ordersByUpdatedAtDescending() {
        assertThat( buildSearchHitHql( false, true, true ) ).endsWith( "order by t.updatedAt desc" );
    }

    /**
     * The id lookup returns at most one row, and the service puts it first. An ORDER BY here would
     * be noise, and one that tried to interleave the two halves would put a stale exact match
     * behind fresher title matches.
     */
    @Test
    public void idQuery_hasNoOrderBy() {
        assertThat( buildSearchHitHql( true, true, true ) ).doesNotContain( "order by" );
    }

    @Test
    public void likePattern_isASubstringMatchAgainstLowercasedText() {
        assertThat( searchLikePattern( "Reference 500" ) ).isEqualTo( "%reference 500%" );
        assertThat( searchLikePattern( "CURATION" ) ).isEqualTo( "%curation%" );
    }

    /**
     * A curator's own wildcards are data, not syntax: {@code 50%} looks for a title containing
     * "50%", and {@code TNF_alpha} does not match "TNFXalpha".
     */
    @Test
    public void likePattern_escapesTheCallersOwnWildcards() {
        assertThat( searchLikePattern( "50%" ) ).isEqualTo( "%50~%%" );
        assertThat( searchLikePattern( "TNF_alpha" ) ).isEqualTo( "%tnf~_alpha%" );
        // the escape character itself survives being typed
        assertThat( searchLikePattern( "a~b" ) ).isEqualTo( "%a~~b%" );
    }

    @Test
    public void fromRow_projectsTheColumnsInTheOrderTheQuerySelectsThem() {
        Date updated = new Date( 1756675380000L );
        TicketSearchHitValueObject hit = TicketSearchHitValueObject.fromRow( new Object[] {
                6L, "Reference 500 — ongoing curation review", TicketState.OPEN, TicketType.CURATION,
                500L, updated } );
        assertThat( hit.getId() ).isEqualTo( 6L );
        assertThat( hit.getTitle() ).isEqualTo( "Reference 500 — ongoing curation review" );
        assertThat( hit.getState() ).isEqualTo( TicketState.OPEN );
        assertThat( hit.getType() ).isEqualTo( TicketType.CURATION );
        assertThat( hit.getTargetCount() ).isEqualTo( 500L );
        assertThat( hit.getUpdatedAt() ).isEqualTo( updated );
    }

    @Test
    public void fromRow_readsAZeroTargetTicketAsZeroNotNull() {
        TicketSearchHitValueObject hit = TicketSearchHitValueObject.fromRow( new Object[] {
                7L, "empty", TicketState.OPEN, TicketType.GENERIC, 0L, new Date() } );
        assertThat( hit.getTargetCount() ).isZero();
    }
}

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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rule that decides whether a caller sees troubled entities.
 * <p>
 * Tested here, on the rule itself, rather than through a DAO query: a filtered query for a
 * non-administrator also carries the ACL EXISTS clause, and test-created entities have no ACL rows,
 * so every such query returns nothing whatever the troubled filter says. A test asserting emptiness
 * there would pass with the rule broken.
 *
 * @author gembro
 */
public class AbstractCuratableDaoTest {

    @BeforeEach
    public void authenticateAsOrdinaryUser() {
        SecurityContextHolder.getContext().setAuthentication( new UsernamePasswordAuthenticationToken(
                "curator", "x", Collections.singletonList( new SimpleGrantedAuthority( "GROUP_USER" ) ) ) );
    }

    @AfterEach
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** The default is unchanged: an ordinary caller does not see troubled entities. */
    @Test
    public void testTroubledAreHiddenByDefault() {
        assertThat( AbstractCuratableDao.shouldHideTroubled( Filters.empty(), "ee", "s" ) ).isTrue();
    }

    /**
     * 🛑 ...but a caller who asks about trouble is not contradicted.
     * <p>
     * ANDing {@code troubled = false} onto a query that says {@code troubled = true} yields an empty
     * list rather than an error, so a curator asking which of their datasets are troubled was told
     * "none" — the one answer that is never useful and never obviously wrong. The hiding is
     * editorial, not access control: a troubled dataset is not secret, it is one ordinary users are
     * being told not to rely on.
     */
    @Test
    public void testAskingAboutTroubleIsNotNegated() {
        Filters filters = Filters.by( "ee", "curationDetails.troubled", Boolean.class, Filter.Operator.eq, true );

        assertThat( AbstractCuratableDao.shouldHideTroubled( filters, "ee", "s" ) )
                .as( "the caller's own condition stands, unargued with" )
                .isFalse();
    }

    /** A filter on a DIFFERENT alias must not disarm the rule for this one. */
    @Test
    public void testAnotherAliasDoesNotDisarmIt() {
        Filters filters = Filters.by( "ad", "curationDetails.troubled", Boolean.class, Filter.Operator.eq, true );

        assertThat( AbstractCuratableDao.shouldHideTroubled( filters, "ee", "s" ) )
                .as( "the platform's trouble says nothing about the dataset's" )
                .isTrue();
    }

    /**
     * 🛑 The short spelling counts too. {@code troubled} is advertised as an alias for
     * {@code curationDetails.troubled} and lands on the curation-details alias instead of the object
     * alias; a rule matching only the long name left the short one hidden AND contradicted, so
     * {@code filter=troubled = true} answered 0 where the flag was set on 4.
     */
    @Test
    public void testTheAdvertisedShortAliasDisarmsItToo() {
        Filters filters = Filters.by( "s", "troubled", Boolean.class, Filter.Operator.eq, true );

        assertThat( AbstractCuratableDao.shouldHideTroubled( filters, "ee", "s" ) )
                .as( "the spelling the API calls an alias behaves like one" )
                .isFalse();
    }

    /** ...but only for the entity it belongs to: it is this dataset's flag, not its platform's. */
    @Test
    public void testTheShortAliasSaysNothingAboutAnAssociatedEntity() {
        Filters filters = Filters.by( "s", "troubled", Boolean.class, Filter.Operator.eq, true );

        assertThat( AbstractCuratableDao.shouldHideTroubled( filters, "ad", null ) )
                .as( "the dataset's own trouble is not a statement about the platform's" )
                .isTrue();
    }

    /**
     * 🛑 A curator sees troubled entities without asking. Paul, 2026-09-09: "troubled experiments
     * have to be reachable by curators, how else can they fix the trouble."
     * <p>
     * The authority is spelled out rather than inherited: an administrator satisfies
     * {@code hasAuthority('GROUP_CURATOR')} through the role hierarchy only inside a
     * {@code @PreAuthorize} expression, and this rule is a plain method call that sees just the
     * granted authorities. A caller holding {@code GROUP_CURATOR} alone — {@code gemmaAgent} is
     * one — was hidden from every troubled dataset before this.
     */
    @Test
    public void testACuratorSeesTroubledWithoutAskingForThem() {
        authenticateAs( "GROUP_CURATOR" );

        assertThat( AbstractCuratableDao.shouldHideTroubled( Filters.empty(), "ee", "s" ) )
                .as( "a curator does not have to know to ask for the work they are meant to do" )
                .isFalse();
    }

    /** An administrator is unchanged by the widening. */
    @Test
    public void testAnAdministratorStillSeesTroubled() {
        authenticateAs( "GROUP_ADMIN" );

        assertThat( AbstractCuratableDao.shouldHideTroubled( Filters.empty(), "ee", "s" ) ).isFalse();
    }

    /**
     * And the widening does not reach anyone else: {@code GROUP_AGENT} on its own is not a curating
     * role, so an agent that holds only it is still shown the ordinary view.
     */
    @Test
    public void testAnAgentAuthorityAloneDoesNotSeeTroubled() {
        authenticateAs( "GROUP_AGENT" );

        assertThat( AbstractCuratableDao.shouldHideTroubled( Filters.empty(), "ee", "s" ) )
                .as( "only the curating roles are widened" )
                .isTrue();
    }

    private void authenticateAs( String authority ) {
        SecurityContextHolder.getContext().setAuthentication( new UsernamePasswordAuthenticationToken(
                "someone", "x", Collections.singletonList( new SimpleGrantedAuthority( authority ) ) ) );
    }
}

/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.audit;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.junit.Before;
import org.junit.Test;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fast unit tests for the Phase C-1 additions on {@link AuditTrailEventListener}:
 * the {@link PostInsertEventListener} and {@link PreDeleteEventListener} interfaces.
 * No Spring context, no Hibernate flush, no database — collaborators ({@link UserManager},
 * {@link SessionFactory}, {@link Session}) are Mockito stubs and Hibernate event objects
 * are not actually constructable in isolation (their constructors require a real
 * EventSource), so we exercise the {@code onPostInsert} / {@code onPreDelete} logic via
 * a lightweight test seam that calls the same internal emit path.
 * <p>
 * Coverage:
 * <ul>
 *   <li>listener implements {@link PostInsertEventListener} and {@link PreDeleteEventListener};</li>
 *   <li>{@link AuditTrailEventListener#requiresPostCommitHandling(org.hibernate.persister.entity.EntityPersister)}
 *       returns false (in-transaction semantics);</li>
 *   <li>no-arg constructor → CREATE/DELETE emission is a no-op (no UserManager wired);</li>
 *   <li>two-arg constructor + anonymous user → no AuditEvent is appended to the trail;</li>
 *   <li>two-arg constructor + authenticated user → exactly one AuditEvent with the
 *       expected {@link AuditAction} and performer is appended;</li>
 *   <li>existing non-empty trail + CREATE → no duplicate CREATE row is appended
 *       (matches {@code AuditAdvice.addAuditEvent} cascade-visit guard);</li>
 *   <li>AuditTrail / AuditEvent are filtered out of the lifecycle target (no recursion
 *       into the audit infrastructure itself).</li>
 * </ul>
 */
public class AuditTrailEventListenerTest {

    private UserManager userManager;
    private SessionFactory sessionFactory;
    private Session session;

    @Before
    public void setUp() {
        userManager = mock( UserManager.class );
        sessionFactory = mock( SessionFactory.class );
        session = mock( Session.class );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        when( session.getHibernateFlushMode() ).thenReturn( FlushMode.AUTO );
    }

    // ----------------------------------------------------------------------------------
    // Interface contract
    // ----------------------------------------------------------------------------------

    @Test
    public void listener_implements_postInsert_preDelete_and_persist() {
        AuditTrailEventListener listener = new AuditTrailEventListener();
        assertThat( listener ).isInstanceOf( PersistEventListener.class );
        assertThat( listener ).isInstanceOf( PostInsertEventListener.class );
        assertThat( listener ).isInstanceOf( PreDeleteEventListener.class );
    }

    @Test
    public void requiresPostCommitHandling_isFalse_soAuditWritesRunInTx() {
        AuditTrailEventListener listener = new AuditTrailEventListener();
        assertThat( listener.requiresPostCommitHandling( null ) ).isFalse();
    }

    // ----------------------------------------------------------------------------------
    // No-arg constructor: persist-guard only, lifecycle emission is a no-op
    // ----------------------------------------------------------------------------------

    @Test
    public void noArgConstructor_postInsert_doesNotTouchUserManagerOrTrail() {
        AuditTrailEventListener listener = new AuditTrailEventListener();
        FakeAuditable auditable = newAuditable( 42L );

        // Reach the same code path that onPostInsert reaches:
        invokeOnPostInsert( listener, auditable );

        assertThat( auditable.getAuditTrail().getEvents() ).isEmpty();
        verify( userManager, never() ).getCurrentUser();
    }

    @Test
    public void noArgConstructor_preDelete_doesNotTouchUserManagerOrTrail() {
        AuditTrailEventListener listener = new AuditTrailEventListener();
        FakeAuditable auditable = newAuditable( 42L );

        invokeOnPreDelete( listener, auditable );

        assertThat( auditable.getAuditTrail().getEvents() ).isEmpty();
        verify( userManager, never() ).getCurrentUser();
    }

    // ----------------------------------------------------------------------------------
    // Two-arg constructor: anonymous skip
    // ----------------------------------------------------------------------------------

    @Test
    public void anonymousUser_postInsert_skipsAuditEmission() {
        when( userManager.getCurrentUser() ).thenReturn( null );
        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        FakeAuditable auditable = newAuditable( 42L );

        invokeOnPostInsert( listener, auditable );

        assertThat( auditable.getAuditTrail().getEvents() )
                .as( "anonymous user should never produce an audit row" )
                .isEmpty();
    }

    @Test
    public void anonymousUser_preDelete_skipsAuditEmission() {
        when( userManager.getCurrentUser() ).thenReturn( null );
        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        FakeAuditable auditable = newAuditable( 42L );

        invokeOnPreDelete( listener, auditable );

        assertThat( auditable.getAuditTrail().getEvents() ).isEmpty();
    }

    @Test
    public void getCurrentUser_isCalledWithManualFlushMode_andPreviousModeRestored() {
        // AuditAdvice.doAuditAdvice flips the session's FlushMode to MANUAL before resolving
        // the current user, to prevent UserManager from triggering a mid-flush flush
        // (Gemma#1093). The listener must preserve this behaviour AND restore the
        // previous mode in a finally block. Track the call ordering via an in-test list.
        User dummyUser = new User();
        dummyUser.setUserName( "alice" );
        java.util.List<String> calls = new java.util.ArrayList<>();
        when( session.getHibernateFlushMode() ).thenReturn( FlushMode.AUTO );
        org.mockito.Mockito.doAnswer( inv -> {
            calls.add( "set:" + inv.getArgument( 0 ) );
            return null;
        } ).when( session ).setHibernateFlushMode( any( FlushMode.class ) );
        when( userManager.getCurrentUser() ).thenAnswer( inv -> {
            calls.add( "getCurrentUser" );
            return dummyUser;
        } );

        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        invokeOnPostInsert( listener, newAuditable( 42L ) );

        // Expected order: flip to MANUAL → call getCurrentUser → restore to AUTO.
        assertThat( calls ).containsExactly(
                "set:MANUAL",
                "getCurrentUser",
                "set:AUTO"
        );
    }

    // ----------------------------------------------------------------------------------
    // Two-arg constructor: authenticated emission
    // ----------------------------------------------------------------------------------

    @Test
    public void authenticatedUser_postInsert_appendsCreateAuditEvent() {
        User alice = new User();
        alice.setUserName( "alice" );
        when( userManager.getCurrentUser() ).thenReturn( alice );

        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        FakeAuditable auditable = newAuditable( 42L );

        invokeOnPostInsert( listener, auditable );

        assertThat( auditable.getAuditTrail().getEvents() ).hasSize( 1 );
        AuditEvent ev = auditable.getAuditTrail().getEvents().get( 0 );
        assertThat( ev.getAction() ).isEqualTo( AuditAction.CREATE );
        assertThat( ev.getPerformer() ).isSameAs( alice );
        assertThat( ev.getDate() ).isNotNull();
    }

    @Test
    public void authenticatedUser_preDelete_appendsDeleteAuditEvent() {
        User alice = new User();
        alice.setUserName( "alice" );
        when( userManager.getCurrentUser() ).thenReturn( alice );

        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        FakeAuditable auditable = newAuditable( 42L );

        invokeOnPreDelete( listener, auditable );

        assertThat( auditable.getAuditTrail().getEvents() ).hasSize( 1 );
        AuditEvent ev = auditable.getAuditTrail().getEvents().get( 0 );
        assertThat( ev.getAction() ).isEqualTo( AuditAction.DELETE );
        assertThat( ev.getPerformer() ).isSameAs( alice );
    }

    // ----------------------------------------------------------------------------------
    // Duplicate-CREATE guard
    // ----------------------------------------------------------------------------------

    @Test
    public void existingNonEmptyTrail_postInsert_doesNotAppendDuplicateCreate() {
        User alice = new User();
        alice.setUserName( "alice" );
        when( userManager.getCurrentUser() ).thenReturn( alice );

        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        FakeAuditable auditable = newAuditable( 42L );
        // Pre-existing CREATE row (could have been written by AuditAdvice in C-1's
        // dual-emission window, or by a previous flush in a cascade).
        AuditEvent pre = AuditEvent.Factory.newInstance( new java.util.Date(), AuditAction.CREATE,
                "pre", null, alice, null );
        auditable.getAuditTrail().getEvents().add( pre );

        invokeOnPostInsert( listener, auditable );

        assertThat( auditable.getAuditTrail().getEvents() )
                .as( "non-empty trail at CREATE time should not get a second CREATE row" )
                .hasSize( 1 )
                .containsExactly( pre );
    }

    // ----------------------------------------------------------------------------------
    // Audit-infrastructure self-recursion guard
    // ----------------------------------------------------------------------------------

    @Test
    public void auditTrailEntity_postInsert_isNotItselfAudited() {
        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        AuditTrail trail = new AuditTrail();

        invokeOnPostInsert( listener, trail );

        verify( userManager, never() ).getCurrentUser();
    }

    @Test
    public void auditEventEntity_postInsert_isNotItselfAudited() {
        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        AuditEvent ev = AuditEvent.Factory.newInstance( new java.util.Date(), AuditAction.CREATE,
                "", null, new User(), null );

        invokeOnPostInsert( listener, ev );

        verify( userManager, never() ).getCurrentUser();
    }

    @Test
    public void nonAuditable_postInsert_isIgnored() {
        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );

        invokeOnPostInsert( listener, "I am not Auditable" );

        verify( userManager, never() ).getCurrentUser();
    }

    // ----------------------------------------------------------------------------------
    // Test helpers
    // ----------------------------------------------------------------------------------

    /**
     * Hibernate's {@link PostInsertEvent} constructor requires a live
     * {@code EventSource} — we can't build one in an isolated unit test. Instead,
     * fabricate a Mockito stub and have it answer just the calls the listener makes.
     */
    private static void invokeOnPostInsert( AuditTrailEventListener listener, Object entity ) {
        PostInsertEvent event = mock( PostInsertEvent.class );
        when( event.getEntity() ).thenReturn( entity );
        listener.onPostInsert( event );
    }

    private static void invokeOnPreDelete( AuditTrailEventListener listener, Object entity ) {
        PreDeleteEvent event = mock( PreDeleteEvent.class );
        when( event.getEntity() ).thenReturn( entity );
        listener.onPreDelete( event );
    }

    /**
     * Construct a minimal {@link Auditable} with a fresh {@link AuditTrail} and an
     * assigned id, so the listener's "trail is empty / has id" branches behave as they
     * would in a real flush.
     */
    private static FakeAuditable newAuditable( long id ) {
        return new FakeAuditable( id );
    }

    /**
     * Local Auditable that doesn't depend on any specific domain entity (avoids
     * accidental side-effects from Hibernate-loaded UserGroup state in unit context).
     */
    private static class FakeAuditable implements Auditable {
        private final Long id;
        private AuditTrail trail = new AuditTrail();

        FakeAuditable( Long id ) {
            this.id = id;
        }

        @Override
        public AuditTrail getAuditTrail() {
            return trail;
        }

        @Override
        public void setAuditTrail( AuditTrail auditTrail ) {
            this.trail = auditTrail;
        }

        public Long getId() {
            return id;
        }
    }
}

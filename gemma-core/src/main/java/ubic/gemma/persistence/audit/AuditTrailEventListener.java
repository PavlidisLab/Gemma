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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.lang.Nullable;
import ubic.gemma.core.security.audit.AuditLogger;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.User;

import java.util.Date;

/**
 * Phase 3 persister retirement (roadmap step 2): a Hibernate {@link PersistEventListener}
 * that guarantees every {@link Auditable} entity has a non-null {@link AuditTrail} attached
 * before {@code session.persist} hands it off to the cascade machinery. Replaces the
 * explicit priming in {@code PersisterHelperImpl.doPersist}, which previously called
 * {@code AuditTrailDao.create} ahead of the chain.
 * <p>
 * Once this listener is in place, removing the per-Auditable priming from the persister
 * chain becomes safe — the listener fires before any cascade, and every Auditable arriving
 * at the listener gets a fresh AuditTrail if one is somehow missing. In practice
 * {@code AbstractAuditable} initialises the field on construction so this defensive branch
 * is rarely exercised; the listener exists to document the invariant and to provide a
 * single chokepoint for the audit-trail lifecycle now that the persister hierarchy is
 * being dismantled.
 * <p>
 * HBM cascade declarations ({@code cascade="all"} on every Auditable's auditTrail
 * many-to-one — see Investigation.hbm.xml, ArrayDesign.hbm.xml, ExternalDatabase.hbm.xml,
 * GeneSet.hbm.xml, UserGroup.hbm.xml, ExpressionExperimentSet.hbm.xml) take it from there:
 * Hibernate persists the AuditTrail ahead of its parent, satisfying the
 * {@code AUDIT_TRAIL_FK NOT NULL} constraint on the parent table.
 *
 * <h3>Audit Phase C-2: auto-CREATE / auto-DELETE via Hibernate listeners</h3>
 *
 * As of Phase C-2 (per AUDIT_MIGRATION_PHASE_C_RECCE.md §2.1) this class additionally
 * implements {@link PostInsertEventListener} and {@link PreDeleteEventListener} and
 * is registered for the corresponding Hibernate event types in
 * {@link AuditTrailEventListenerConfig}. It has taken over the auto-CREATE /
 * auto-DELETE emission that {@code AuditAdvice.doCreateAdvice} and
 * {@code AuditAdvice.doDeleteAdvice} previously performed via {@code @Before} AOP
 * advices (those advices were deleted in the C-2 commit).
 * <p>
 * Why PostInsert (not PersistEventListener) for CREATE: at PostInsert the entity's
 * {@code AuditTrail} row already has its DB-assigned id, so adding an {@code AuditEvent}
 * to the trail's {@code <bag>} doesn't trigger transient-entity-on-flush errors.
 * PersistEventListener fires BEFORE the insert SQL — both parent and cascaded AuditTrail
 * child still have null ids, and enqueuing a child AuditEvent there requires deferred
 * flushing this listener should not own.
 * <p>
 * Why PreDelete (not PostDelete) for DELETE: at PostDelete the AuditTrail row itself
 * has already been removed from the session — we cannot write a DELETE row into a trail
 * that no longer exists. PreDelete fires before the SQL DELETE; the trail is still
 * attached, the AuditEvent insert fits into the same flush as the parent's removal.
 *
 * @author phase3-agent
 * @see ubic.gemma.persistence.audit.AuditTrailEventListenerConfig
 */
public class AuditTrailEventListener implements PersistEventListener, PostInsertEventListener, PreDeleteEventListener {

    private static final Log log = LogFactory.getLog( AuditTrailEventListener.class );

    @Nullable
    private final UserManager userManager;
    @Nullable
    private final SessionFactory sessionFactory;
    private final AuditLogger auditLogger = new AuditLogger();

    /**
     * Legacy no-arg constructor: keeps the persist-side guard behaviour available without
     * pulling in the auto-CREATE / auto-DELETE machinery. Used by callers that only want
     * the {@link PersistEventListener} side; {@link #onPostInsert} and {@link #onPreDelete}
     * become no-ops because they can't reach the {@link UserManager}.
     */
    public AuditTrailEventListener() {
        this( null, null );
    }

    /**
     * Full-fat constructor: persist-side guard PLUS auto-CREATE / auto-DELETE emission
     * (Phase C-2). Pass a non-null {@link UserManager} + {@link SessionFactory} to enable
     * the Hibernate-listener-driven CREATE/DELETE rows that replace the deleted
     * {@code AuditAdvice.doCreateAdvice()/doDeleteAdvice()} AOP advices.
     */
    public AuditTrailEventListener( @Nullable UserManager userManager, @Nullable SessionFactory sessionFactory ) {
        this.userManager = userManager;
        this.sessionFactory = sessionFactory;
    }

    // ----------------------------------------------------------------------------------
    // PersistEventListener — pre-existing AuditTrail invariant guard
    // ----------------------------------------------------------------------------------

    @Override
    public void onPersist( PersistEvent event ) {
        ensureAuditTrail( event.getObject() );
    }

    @Override
    public void onPersist( PersistEvent event, PersistContext createdAlready ) {
        ensureAuditTrail( event.getObject() );
    }

    private void ensureAuditTrail( Object entity ) {
        if ( entity instanceof Auditable ) {
            Auditable auditable = ( Auditable ) entity;
            if ( auditable.getAuditTrail() == null ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( String.format(
                            "Auditable %s arrived at persist without an AuditTrail; initialising one defensively.",
                            entity.getClass().getSimpleName() ) );
                }
                auditable.setAuditTrail( new AuditTrail() );
            }
            // The HBM cascade="all" on the auditTrail many-to-one walks into the
            // AuditTrail and persists it ahead of the parent. No explicit priming
            // (session.persist of the AuditTrail) is needed here — doing so would
            // duplicate the cascade.
        }
    }

    // ----------------------------------------------------------------------------------
    // PostInsertEventListener — auto-CREATE emission (Phase C-2)
    // ----------------------------------------------------------------------------------

    @Override
    public void onPostInsert( PostInsertEvent event ) {
        Object entity = event.getEntity();
        if ( !isLifecycleTarget( entity ) ) {
            return;
        }
        emitLifecycleEvent( ( Auditable ) entity, AuditAction.CREATE );
    }

    // ----------------------------------------------------------------------------------
    // PreDeleteEventListener — auto-DELETE emission (Phase C-2)
    // ----------------------------------------------------------------------------------

    @Override
    public boolean onPreDelete( PreDeleteEvent event ) {
        Object entity = event.getEntity();
        if ( !isLifecycleTarget( entity ) ) {
            return false;
        }
        emitLifecycleEvent( ( Auditable ) entity, AuditAction.DELETE );
        return false; // never veto the delete
    }

    /**
     * Filter for CREATE/DELETE emission: only fire on real Auditables, never recurse into
     * the audit infrastructure itself (the AuditTrail / AuditEvent rows we write would
     * otherwise re-enter this listener and either no-op or stack-overflow).
     */
    private static boolean isLifecycleTarget( @Nullable Object entity ) {
        return entity instanceof Auditable
                && !( entity instanceof AuditTrail )
                && !( entity instanceof AuditEvent );
    }

    /**
     * Emit a lifecycle audit event ({@link AuditAction#CREATE} or {@link AuditAction#DELETE})
     * for the given Auditable. Mirrors the UserManager-current-user dance + anonymous-skip
     * from {@code AuditAdvice.doAuditAdvice} (lines 161-178):
     * <ol>
     *   <li>Set the current session's flush mode to {@code MANUAL} while resolving the
     *       current user — {@code UserManager.getCurrentUser()} can otherwise trigger a
     *       Hibernate flush mid-flush (see PavlidisLab/Gemma#1093);</li>
     *   <li>If no user is resolvable (anonymous request), skip silently;</li>
     *   <li>Otherwise append a typed-null {@link AuditEvent} to the auditable's trail.</li>
     * </ol>
     * <p>
     * If this listener was constructed without a {@link UserManager} / {@link SessionFactory}
     * (the no-arg constructor used by the legacy persist-guard config), the method is a
     * no-op — callers that want CREATE/DELETE emission must use the two-arg constructor.
     */
    private void emitLifecycleEvent( Auditable auditable, AuditAction action ) {
        if ( userManager == null || sessionFactory == null ) {
            // No-op: persist-guard-only configuration. Wiring path for C-2.
            return;
        }
        Session session = sessionFactory.getCurrentSession();
        FlushMode previousFlushMode = session.getHibernateFlushMode();
        User user;
        try {
            session.setHibernateFlushMode( FlushMode.MANUAL );
            user = userManager.getCurrentUser();
        } finally {
            session.setHibernateFlushMode( previousFlushMode );
        }
        if ( user == null ) {
            if ( log.isInfoEnabled() ) {
                log.info( String.format( "User could not be determined (anonymous?), %s audit skipped for %s.",
                        action, auditable.getClass().getSimpleName() ) );
            }
            return;
        }
        if ( auditable.getAuditTrail() == null ) {
            // Defensive: persist-guard SHOULD have created the trail. If it didn't, we cannot
            // attach an event. Log and bail rather than NPE.
            log.warn( String.format( "Auditable %s reached %s lifecycle event without an AuditTrail; skipping audit emission.",
                    auditable.getClass().getSimpleName(), action ) );
            return;
        }
        if ( action == AuditAction.CREATE && !auditable.getAuditTrail().getEvents().isEmpty() ) {
            // Match AuditAdvice.addAuditEvent line 327-330: don't double-emit a CREATE on
            // an already-populated trail (cascade visits + back-fill on existing trails).
            if ( log.isTraceEnabled() ) {
                log.trace( String.format( "Skipped CREATE on %s since its audit trail has already been filled.", auditable ) );
            }
            return;
        }
        AuditEvent ev = AuditEvent.Factory.newInstance( new Date(), action, null, null, user, null );
        // AuditTrail#addEvent appends to the events bag AND repoints the
        // denormalised lastEvent pointer (perf hotspot B fix; see V6 migration
        // + AuditEventDaoImpl#getLastEvents rewrite). The cascade insert that
        // runs on flush assigns ev.id strictly greater than any sibling, so by
        // the (date desc, id desc) ordering this append wins; addEvent handles
        // the pre-flush id-null edge case explicitly.
        auditable.getAuditTrail().addEvent( ev );
        auditLogger.log( auditable, ev );
    }

    /**
     * Run inside the flushing transaction, not deferred to post-commit. The cascading
     * AuditEvent insert rolls back with the parent on transaction failure — same semantics
     * the current {@code AuditAdvice.creator()} @Before advice provides via being inside
     * the DAO's transaction.
     */
    @Override
    public boolean requiresPostCommitHandling( EntityPersister persister ) {
        return false;
    }
}

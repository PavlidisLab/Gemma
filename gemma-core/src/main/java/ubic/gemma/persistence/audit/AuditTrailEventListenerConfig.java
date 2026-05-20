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
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.core.security.authentication.UserManager;

/**
 * Phase 3 persister retirement (roadmap step 2): registers
 * {@link AuditTrailEventListener} with Hibernate's {@link EventListenerRegistry}
 * so every {@code session.persist} of an {@code Auditable} flows through a single
 * audit-trail guard, replacing the per-call priming previously hand-coded in
 * {@code PersisterHelperImpl.doPersist}.
 * <p>
 * Mirrors the pattern in
 * {@link ubic.gemma.core.security.acl.AclEventListenerConfig} (renovations
 * Phase 3 ACL listener cutover). Registration happens in
 * {@link InitializingBean#afterPropertiesSet()} so the SessionFactory is fully
 * initialised by then; the listener is prepended on {@code PERSIST} so it runs
 * before Hibernate's default {@code DefaultPersistEventListener} and cascade
 * machinery — the AuditTrail has to be non-null on the parent before cascade
 * walks into it.
 *
 * <h3>Audit Phase C-2: PostInsert / PreDelete wired</h3>
 * The listener implements {@code PostInsertEventListener} +
 * {@code PreDeleteEventListener} to drive auto-CREATE / auto-DELETE emission
 * (see {@code AUDIT_MIGRATION_PHASE_C_RECCE.md} §2.1). Phase C-2 cuts those
 * over: the listener is now constructed with a {@link UserManager} +
 * {@link SessionFactory} and registered on {@code POST_INSERT} +
 * {@code PRE_DELETE}. The {@code AuditAdvice.doCreateAdvice} +
 * {@code doDeleteAdvice} @Before advices are deleted in the same commit so
 * the two emitters never both fire on the same lifecycle event.
 */
@Configuration
public class AuditTrailEventListenerConfig implements InitializingBean {

    private static final Log log = LogFactory.getLog( AuditTrailEventListenerConfig.class );

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private UserManager userManager;

    @Override
    public void afterPropertiesSet() {
        // Two-arg constructor: persist-guard + auto-CREATE / auto-DELETE emission.
        // AuditAdvice.doCreateAdvice / doDeleteAdvice are deleted in the same C-2
        // commit; without their deletion this configuration would double-emit
        // CREATE/DELETE rows.
        AuditTrailEventListener listener = new AuditTrailEventListener( userManager, sessionFactory );
        SessionFactoryImplementor sfi = sessionFactory.unwrap( SessionFactoryImplementor.class );
        EventListenerRegistry registry = sfi.getServiceRegistry().getService( EventListenerRegistry.class );
        if ( registry == null ) {
            throw new IllegalStateException( "Hibernate EventListenerRegistry not available on SessionFactory" );
        }
        // PERSIST: prepend so the guard runs before Hibernate's default persist
        // listener + cascade walker. The Auditable must already carry a non-null
        // AuditTrail when cascade walks in; cascade="all" on the HBM mapping
        // carries the AuditTrail into the session along with its parent.
        registry.prependListeners( EventType.PERSIST, listener );
        registry.prependListeners( EventType.PERSIST_ONFLUSH, listener );
        // POST_INSERT / PRE_DELETE: append so any other lifecycle listeners that
        // care about ordering (e.g. AclEventListener on POST_INSERT) run first.
        // The audit emit is additive and has no ordering dependency.
        registry.appendListeners( EventType.POST_INSERT, listener );
        registry.appendListeners( EventType.PRE_DELETE, listener );
        log.info( "Registered AuditTrailEventListener on Hibernate PERSIST, PERSIST_ONFLUSH, "
                + "POST_INSERT and PRE_DELETE (Audit Phase C-2)." );
    }
}

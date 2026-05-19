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
 */
@Configuration
public class AuditTrailEventListenerConfig implements InitializingBean {

    private static final Log log = LogFactory.getLog( AuditTrailEventListenerConfig.class );

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public void afterPropertiesSet() {
        AuditTrailEventListener listener = new AuditTrailEventListener();
        SessionFactoryImplementor sfi = sessionFactory.unwrap( SessionFactoryImplementor.class );
        EventListenerRegistry registry = sfi.getServiceRegistry().getService( EventListenerRegistry.class );
        if ( registry == null ) {
            throw new IllegalStateException( "Hibernate EventListenerRegistry not available on SessionFactory" );
        }
        // Prepend so the guard runs before Hibernate's default persist listener +
        // cascade walker. By then the Auditable must already carry a non-null
        // AuditTrail; cascade="all" on the HBM mapping carries the AuditTrail
        // into the session along with its parent.
        registry.prependListeners( EventType.PERSIST, listener );
        registry.prependListeners( EventType.PERSIST_ONFLUSH, listener );
        log.info( "Registered AuditTrailEventListener on Hibernate PERSIST and PERSIST_ONFLUSH." );
    }
}

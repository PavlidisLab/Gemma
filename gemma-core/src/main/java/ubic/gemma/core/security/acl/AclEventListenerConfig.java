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
package ubic.gemma.core.security.acl;

import ubic.gemma.core.security.gsec.acl.AclEventListener;
import ubic.gemma.core.security.gsec.acl.BaseAclAdvice;
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
 * Renovations Phase 3: register {@link AclEventListener} with Hibernate's
 * {@link EventListenerRegistry} so PostInsert and PostDelete events drive ACL maintenance
 * directly, replacing the {@code @AfterReturning} AOP advice that walked DAO arguments via
 * reflection.
 * <p>
 * Listener registration happens in {@link InitializingBean#afterPropertiesSet()} so it runs
 * after the SessionFactory and AclAdvice beans are fully initialized. The listener is appended
 * (not prepended) so any other registered post-insert/post-delete listeners run first; ours is
 * purely additive and has no dependency on the order.
 * <p>
 * During the phase-3 transition both this listener and the AOP advice fire on the same
 * inserts. {@code BaseAclAdvice.addOrUpdateAcl} is idempotent — it reads the ACL first and
 * either updates the parent reference or returns — so the second invocation is a cheap re-read
 * rather than a duplicate create. Once we've verified the listener fully covers the advice's
 * responsibilities, the AOP wiring in {@code applicationContext-security.xml} can be removed.
 */
@Configuration
public class AclEventListenerConfig implements InitializingBean {

    private static final Log log = LogFactory.getLog( AclEventListenerConfig.class );

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private BaseAclAdvice aclAdvice;

    @Override
    public void afterPropertiesSet() {
        AclEventListener listener = new AclEventListener( aclAdvice, sessionFactory );
        SessionFactoryImplementor sfi = sessionFactory.unwrap( SessionFactoryImplementor.class );
        EventListenerRegistry registry = sfi.getServiceRegistry().getService( EventListenerRegistry.class );
        if ( registry == null ) {
            throw new IllegalStateException( "Hibernate EventListenerRegistry not available on SessionFactory" );
        }
        registry.appendListeners( EventType.POST_INSERT, listener );
        registry.appendListeners( EventType.POST_DELETE, listener );
        log.info( "Registered AclEventListener on Hibernate POST_INSERT and POST_DELETE." );
    }
}

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

import ubic.gemma.core.security.acl.AclEventListener;
import ubic.gemma.core.security.acl.BaseAclAdvice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

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
/**
 * {@code @Lazy(false)} is required, not decorative. This configuration exists purely for the side
 * effect in {@link #afterPropertiesSet()}; no other bean injects it. In CLI contexts
 * {@link ubic.gemma.core.context.LazyInitByDefaultPostProcessor} marks every non-infrastructure
 * bean definition lazy-init, so without this annotation the bean is defined but never
 * instantiated, and the listener below is never registered — silently, since nothing fails.
 * <p>
 * That is not hypothetical: it is what happened between the AOP-advice cutover (commit
 * {@code 21e4fc412e}, 2026-05-18) and 2026-08-08. Every experiment created by a CLI in that window
 * got no ACL at all, and every experiment deleted by a CLI left its entire ACL tree behind. The
 * post-processor skips any definition annotated {@code @Lazy} regardless of its value, so this
 * annotation both keeps the bean eager and opts it out of the sweep.
 */
@Configuration
@Lazy(false)
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

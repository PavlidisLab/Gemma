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

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ubic.gemma.core.context.LazyInitByDefaultPostProcessor;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.audit.AuditTrailEventListener;
import ubic.gemma.persistence.audit.AuditTrailEventListenerConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression guard for the CLI-only failure in which the Hibernate event listeners were
 * never registered.
 *
 * <h2>Why the existing coverage missed it</h2>
 * {@code AclAdviceTest.testExpressionExperimentAcls} already deletes an experiment and
 * asserts its ACLs are gone, and it passed throughout the three months the listener was
 * dead in production. It could not have failed: no test context registers
 * {@link LazyInitByDefaultPostProcessor}, so every test context is eager,
 * {@link AclEventListenerConfig} always instantiates there, and the listener always
 * registers. The defect was never in the deletion logic — it was in whether the listener
 * was wired at all, and that depends on the *shape* of the context, not on the ACL code.
 * <p>
 * So the missing test is not another deletion assertion. It is this: build a context that
 * behaves like a CLI context — lazy-by-default — and assert the listeners still register.
 * That is the only configuration in which the bug is observable.
 * <p>
 * Fails (no interactions with the registry) if {@code @Lazy(false)} is removed from either
 * configuration. Complements {@code SideEffectConfigurationEagerTest}, which guards the
 * annotation generically for any future side-effect configuration; this one pins the
 * behaviour of the two that actually broke.
 */
public class EventListenerRegistrationLazyContextTest {

    /**
     * Minimal stub of the chain {@code SessionFactory -> SessionFactoryImplementor ->
     * ServiceRegistry -> EventListenerRegistry} that both configurations walk in their
     * {@code afterPropertiesSet}.
     */
    private static SessionFactory sessionFactoryReporting( EventListenerRegistry registry ) {
        SessionFactory sessionFactory = mock( SessionFactory.class );
        SessionFactoryImplementor implementor = mock( SessionFactoryImplementor.class );
        ServiceRegistryImplementor services = mock( ServiceRegistryImplementor.class );
        when( sessionFactory.unwrap( SessionFactoryImplementor.class ) ).thenReturn( implementor );
        when( implementor.getServiceRegistry() ).thenReturn( services );
        when( services.getService( EventListenerRegistry.class ) ).thenReturn( registry );
        return sessionFactory;
    }

    /**
     * A context wired the way {@code CliComponentScanConfig} wires one: the lazy-by-default
     * post-processor is active, and nothing injects the configuration under test.
     */
    private static <T> AnnotationConfigApplicationContext lazyContext( Class<?> configClass,
            SessionFactory sessionFactory, Class<T> collaboratorType, T collaborator ) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.addBeanFactoryPostProcessor( new LazyInitByDefaultPostProcessor() );
        // The collaborator type is passed explicitly rather than derived from the mock: a
        // Mockito mock of an interface reports Object as its superclass, so inferring it
        // would register UserManager under the wrong type.
        ctx.registerBean( SessionFactory.class, () -> sessionFactory );
        ctx.registerBean( collaboratorType, () -> collaborator );
        ctx.register( configClass );
        ctx.refresh();
        return ctx;
    }

    @Test
    public void aclListenerRegistersDespiteLazyByDefault() {
        EventListenerRegistry registry = mock( EventListenerRegistry.class );
        BaseAclAdvice aclAdvice = mock( BaseAclAdvice.class );

        try ( AnnotationConfigApplicationContext ctx =
                      lazyContext( AclEventListenerConfig.class, sessionFactoryReporting( registry ),
                              BaseAclAdvice.class, aclAdvice ) ) {
            verify( registry ).appendListeners( eq( EventType.POST_INSERT ), any( AclEventListener.class ) );
            verify( registry ).appendListeners( eq( EventType.POST_DELETE ), any( AclEventListener.class ) );
        }
    }

    @Test
    public void auditListenerRegistersDespiteLazyByDefault() {
        EventListenerRegistry registry = mock( EventListenerRegistry.class );
        UserManager userManager = mock( UserManager.class );

        try ( AnnotationConfigApplicationContext ctx =
                      lazyContext( AuditTrailEventListenerConfig.class, sessionFactoryReporting( registry ),
                              UserManager.class, userManager ) ) {
            verify( registry ).prependListeners( eq( EventType.PERSIST ), any( AuditTrailEventListener.class ) );
            verify( registry ).appendListeners( eq( EventType.POST_INSERT ), any( AuditTrailEventListener.class ) );
            verify( registry ).appendListeners( eq( EventType.PRE_DELETE ), any( AuditTrailEventListener.class ) );
        }
    }
}

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
package ubic.gemma.core.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.intercept.AfterInvocationManager;
import org.springframework.security.access.intercept.AfterInvocationProviderManager;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Method-security configuration for Gemma, replacing the deprecated XML
 * {@code <s:global-method-security>} block in {@code applicationContext-security.xml}.
 * <p>
 * Despite the user-facing renaming to {@code @EnableMethodSecurity}, Spring Security 6 still ships
 * the legacy {@link EnableGlobalMethodSecurity} / {@link GlobalMethodSecurityConfiguration} stack
 * that uses {@link AccessDecisionManager}, {@link AfterInvocationManager}, and {@link RunAsManager}.
 * The new {@code @EnableMethodSecurity} annotation is an
 * {@link org.springframework.security.authorization.AuthorizationManager}-based replacement that
 * has no {@link AfterInvocationManager} concept — porting Gemma's after-invocation providers to
 * the new model would require a full re-architecture (custom {@code AuthorizationManager} + a
 * post-invocation {@code MethodInterceptor}). We deliberately stay on the legacy stack via
 * {@code @EnableGlobalMethodSecurity} so the existing after-invocation provider beans, the existing
 * {@code accessDecisionManager} (UnanimousBased + voter list), and the existing {@code runAsManager}
 * keep working without modification. {@link AfterInvocationProviderManager} and friends are marked
 * deprecated in Spring 6 but remain fully functional through the 6.x line.
 * <p>
 * <b>Why this migration.</b> The deprecated XML {@code <s:global-method-security>} namespace was
 * suspected (Phase 2) of not reliably wiring the configured {@link
 * DefaultMethodSecurityExpressionHandler} into the SpEL evaluation path used by
 * {@code @PreAuthorize("hasPermission(...)")}, and a {@code hasAuthority('GROUP_ADMIN') or ...}
 * workaround was kept in {@code UserService.removeUserFromGroup}. Re-testing under the Java config
 * showed the suspicion was wrong: {@link
 * GlobalMethodSecurityConfiguration#setMethodSecurityExpressionHandler} is autowired by Spring and
 * — when the context contains a single {@code MethodSecurityExpressionHandler} bean (gsec's
 * {@code securityExpressionHandler}) — that bean is injected and the SpEL voter calls the correct
 * handler with the correct {@link PermissionEvaluator}/{@link RoleHierarchy}. The {@code
 * hasPermission} failure on the admin test path is a separate ACL-evaluation issue (likely a Sid /
 * permission-mask / cache mismatch in the {@code AclPermissionEvaluator} → {@code AclImpl.isGranted}
 * chain) and is tracked as a Phase-3 follow-up; the workaround stays for now. This migration still
 * delivers (1) deprecated-XML removal, (2) explicit Java-config control over the method-security
 * chain so any future SpEL wiring needs are obvious, and (3) a survivable path for follow-up work
 * on the underlying ACL bug without having to touch the XML namespace again.
 * <p>
 * <b>Bean references.</b> All of the collaborators ({@code permissionEvaluator}, {@code
 * roleHierarchy}, {@code accessDecisionManager}, {@code runAsManager}, and the 14 after-invocation
 * provider beans) are defined elsewhere (gsec's {@code applicationContext-gsec.xml} and Gemma's
 * {@code applicationContext-security.xml}) and only referenced here — never duplicated.
 */
@Configuration
// proxyTargetClass = false is the framework default and matches the lab-wide
// JDK-proxy invariant carried forward from the legacy XML config (where
// <s:global-method-security> left proxy-target-class unset, also defaulting
// to false). Made explicit here per the AspectJ deeper recce
// (ASPECTJ_DEEPER_AUDIT.md, recommendation #3) so the invariant is visible at
// the call site and protected against accidental future drift. The
// method-security MethodInterceptor produced by this annotation participates
// in the same JDK-proxy stack as the @Transactional / @Secured advisors
// configured in XML; keeping all of them on interface-based proxies is what
// allows Gemma's heavy use of "FooService extends BaseService<...>" parametric
// interfaces (and the corresponding @PreAuthorize SpEL on those interface
// methods) to be intercepted cleanly.
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, order = 1, proxyTargetClass = false)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

    /**
     * Bean ids of the after-invocation providers to register, in the same order they were listed
     * in the XML {@code <s:global-method-security>} block. Listed by name (rather than autowiring
     * a {@code List<AfterInvocationProvider>}) so we don't accidentally pick up provider beans that
     * exist in the context but were deliberately NOT registered in the legacy XML (e.g.
     * {@code afterAclCompositeSequenceRead}, {@code afterAclDifferentialExpressionAnalysisResultCollectionRead},
     * {@code afterAclValueObjectMapValue}). Keeping the wired set exactly equal to what the XML
     * produced is the contract for the migration.
     */
    private static final List<String> AFTER_INVOCATION_PROVIDER_BEAN_NAMES = Arrays.asList(
            // afterAclRead removed in Phase 3 Phase A — AFTER_ACL_READ callsites moved
            //   to @PostAuthorize("hasPermission(returnObject, ...)") which dispatches via
            //   postInvocationAdviceProvider + the wired AclPermissionEvaluator.
            // afterAclCollectionRead removed in Phase 3 Phase A — AFTER_ACL_COLLECTION_READ
            //   callsites moved to @PostFilter("hasPermission(filterObject, ...)").
            // afterAclMapRead removed in Phase 3 Phase A — AFTER_ACL_MAP_READ callsites
            //   moved to @PostFilter("hasPermission(filterObject.key, ...)").
            // afterAclMapValuesRead removed in Phase 3 Phase A — no live callsites (only one
            //   commented-out FIXME reference in CharacteristicService).
            "afterAclReadQuiet", // AFTER_ACL_READ_QUIET — deferred to Phase B (return-null-on-denial)
            "afterAclCompositeSequenceCollectionRead", // Phase B — bulk fetch by ArrayDesign
            "afterAclDataVectorCollectionRead", // Phase B — bulk fetch by ExpressionExperiment
            "afterAclMyDataRead", // Phase B — needs ACL owner SpEL helper
            "afterAclMyPrivateDataRead", // Phase B — needs ACL private SpEL helper
            "afterAclValueObjectCollection", // Phase B — VO metadata side-effect
            "afterAclValueObjectMap", // Phase B — VO metadata side-effect
            "afterAclValueObject", // Phase B — VO metadata side-effect
            "afterAclStreamRead", // Phase B — Stream<?> return type
            "postInvocationAdviceProvider" // for @PostAuthorize / @PostFilter — REQUIRED for Phase A annotations
    );

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private RoleHierarchy roleHierarchy;

    @Autowired
    @Qualifier("accessDecisionManager")
    private ObjectProvider<AccessDecisionManager> accessDecisionManagerProvider;

    @Autowired
    @Qualifier("runAsManager")
    private ObjectProvider<RunAsManager> runAsManagerProvider;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Build a {@link MethodSecurityExpressionHandler} wired with the lab's permission evaluator
     * (gsec's {@code AclPermissionEvaluator}) and the role hierarchy.
     * <p>
     * Note: {@link GlobalMethodSecurityConfiguration#setMethodSecurityExpressionHandler} is
     * autowired by Spring and, when the application context contains exactly one
     * {@code MethodSecurityExpressionHandler} bean (the {@code securityExpressionHandler} from
     * gsec's {@code applicationContext-gsec.xml}), that bean is injected into the framework's
     * {@code expressionHandler} field at startup and {@code createExpressionHandler()} is never
     * invoked. The handler the SpEL voter ends up using is therefore the gsec one — already wired
     * with the same {@code permissionEvaluator} and {@code roleHierarchy} we'd attach here. This
     * override exists so the legacy XML bean could be retired in a follow-up without losing the
     * SpEL wiring (it becomes the active definition the moment the XML bean is removed).
     */
    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator( permissionEvaluator );
        handler.setRoleHierarchy( roleHierarchy );
        return handler;
    }

    /**
     * Return the existing {@code accessDecisionManager} bean (UnanimousBased with all the
     * securable read/edit + collection + map voters wired in gsec XML). Falls back to the
     * superclass default if the bean isn't present — that shouldn't happen in Gemma, but the
     * defensive lookup keeps test contexts that don't import the gsec XML from blowing up at
     * startup.
     */
    @Override
    protected AccessDecisionManager accessDecisionManager() {
        AccessDecisionManager configured = accessDecisionManagerProvider.getIfAvailable();
        return configured != null ? configured : super.accessDecisionManager();
    }

    /**
     * Aggregate the after-invocation providers from {@link #AFTER_INVOCATION_PROVIDER_BEAN_NAMES}
     * into an {@link AfterInvocationProviderManager}. This preserves the post-invocation
     * filtering chain (ACL_READ filters, value-object filters, {@code @PostAuthorize} /
     * {@code @PostFilter} advice) that the XML {@code <s:after-invocation-provider>} elements
     * used to wire.
     * <p>
     * {@link AfterInvocationProviderManager} is deprecated in Spring 6 but still functional; this
     * is the documented bridge for code that hasn't yet migrated to the AuthorizationManager
     * model. Removing the bridge would require rewriting all 14 providers, which is out of scope.
     */
    @Override
    @SuppressWarnings("deprecation")
    protected AfterInvocationManager afterInvocationManager() {
        List<AfterInvocationProvider> providers = new ArrayList<>( AFTER_INVOCATION_PROVIDER_BEAN_NAMES.size() );
        for ( String beanName : AFTER_INVOCATION_PROVIDER_BEAN_NAMES ) {
            providers.add( applicationContext.getBean( beanName, AfterInvocationProvider.class ) );
        }
        AfterInvocationProviderManager manager = new AfterInvocationProviderManager();
        manager.setProviders( providers );
        return manager;
    }

    /**
     * Return the existing {@code runAsManager} bean ({@link
     * org.springframework.security.access.intercept.RunAsManagerImpl} with the {@code GROUP_}
     * role prefix and the configured run-as key). This is what {@code @Secured("RUN_AS_ADMIN")}
     * needs to elevate authentications for signup / user-management flows.
     */
    @Override
    protected RunAsManager runAsManager() {
        RunAsManager configured = runAsManagerProvider.getIfAvailable();
        return configured != null ? configured : super.runAsManager();
    }
}

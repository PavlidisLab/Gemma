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

import org.springframework.security.acls.afterinvocation.AbstractAclProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.intercept.RunAsImplAuthenticationProvider;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.access.intercept.RunAsManagerImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.event.LoggerListener;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import ubic.gemma.core.security.authentication.GemmaLegacyAwarePasswordEncoder;
import ubic.gemma.core.security.authentication.LegacyAwareDaoAuthenticationProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationCompositeSequenceByArrayDesignFilteringProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationCompositeSequenceCollectionByArrayDesignFilteringProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationOwnedCollectionFilteringProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationPrivateCollectionFilteringProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationQuietReadProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationStreamFilteringProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationValueObjectCollectionReadProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationValueObjectMapReadProvider;
import ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationValueObjectReadProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Core Spring Security wiring for Gemma, replacing the legacy
 * {@code applicationContext-security.xml}.
 *
 * <p>This class is the Phase-3 XML&rarr;Java config migration for everything in that
 * XML file <em>except</em> the parts that already moved:
 * <ul>
 *   <li>{@code <s:global-method-security>} &rarr; {@link MethodSecurityConfig}</li>
 *   <li>ACL stack (JdbcMutableAclService, AclCache, AclAuthorizationStrategy, etc.) &rarr;
 *       {@link ubic.gemma.core.security.acl.GemmaAclConfiguration}</li>
 *   <li>Hibernate PostInsert/PostDelete ACL listener &rarr;
 *       {@link ubic.gemma.core.security.acl.AclEventListenerConfig}</li>
 * </ul>
 *
 * <p>What this class still defines:
 * <ul>
 *   <li>Re-imports the gsec context (gsec's {@code applicationContext-gsec.xml} defined
 *       {@code anonymousAuthenticationProvider}, {@code permissionEvaluator},
 *       {@code roleHierarchy}, the 14 other after-invocation providers, etc.). Done via
 *       {@link ImportResource} since the gsec artifact still ships its bean definitions
 *       as XML.</li>
 *   <li>{@code passwordEncoder} (Gemma-specific legacy-aware encoder).</li>
 *   <li>{@code daoAuthenticationProvider} (Gemma's {@link LegacyAwareDaoAuthenticationProvider}
 *       binding the username to the encoder's ThreadLocal for SHA-1 legacy hashes).</li>
 *   <li>{@code runAsManager} + {@code runAsAuthenticationProvider} (both keyed by
 *       {@code ${gemma.runas.password}}).</li>
 *   <li>{@code authenticationManager} (Spring Security 6 {@link ProviderManager} wiring up
 *       the three providers in the legacy XML's order).</li>
 *   <li>{@code sessionRegistry} (used by the web-side concurrency-control filter).</li>
 *   <li>{@code authenticationLoggerListener} (Spring's stock {@link LoggerListener}).</li>
 *   <li>Four Gemma-specific after-invocation provider beans (composite-sequence,
 *       data-vector, differential-expression-result collection filters) defined here so
 *       they can be looked up by name from {@link MethodSecurityConfig#AFTER_INVOCATION_PROVIDER_BEAN_NAMES}.
 *       Only three of the four are currently wired; the fourth (and a fifth — composite
 *       sequence single) are present-but-unwired pending future hook-up.</li>
 *   <li>AspectJ autoproxy via {@link EnableAspectJAutoProxy} (replaces
 *       {@code <aop:aspectj-autoproxy/>}). The other autoproxy declarations in
 *       {@code applicationContext-hibernate.xml} and {@code applicationContext-serviceBeans.xml}
 *       are idempotent &mdash; Spring registers a single {@code AnnotationAwareAspectJAutoProxyCreator}
 *       regardless of how many sources request it.</li>
 * </ul>
 *
 * <p><b>Spring Security 6 idioms.</b> This is the modern stack: an explicit
 * {@link ProviderManager} bean rather than the deprecated XML
 * {@code <s:authentication-manager>} namespace, no
 * {@code WebSecurityConfigurerAdapter}, no {@code SecurityFilterChain} (URL-filter chains
 * for Gemma live in {@code gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml}
 * and are out of scope for this migration).
 *
 * <p><b>Bean naming.</b> {@code @Bean(name = "...")} is used everywhere a downstream
 * consumer references the bean id literally (e.g. the four after-invocation provider beans
 * looked up by name from {@link MethodSecurityConfig}; the {@code passwordEncoder} that
 * gsec's {@code userManager} consumes by id; the {@code runAsManager} that
 * {@link MethodSecurityConfig#runAsManager()} pulls via {@code @Qualifier("runAsManager")}).
 * For beans referenced only by type the default method-name id is used.
 *
 * <p><b>userDetailsManager / groupManager aliases.</b> gsec expects two
 * {@link UserDetailsService}-shaped beans by those names. Both are aliases for Gemma's
 * {@code userManager} ({@code UserManagerImpl}). The XML used {@code <alias>}; Java config
 * does this with secondary {@code @Bean} methods that return the same instance under the
 * required name, achieved by injecting the existing bean and re-exposing it.
 */
@Configuration
@EnableAspectJAutoProxy
@ImportResource("classpath:gemma/gsec/applicationContext-*.xml")
public class SecurityConfig {

    /**
     * Register the {@code userDetailsManager} and {@code groupManager} aliases for the
     * {@code userManager} bean ({@code UserManagerImpl}). gsec's
     * {@code applicationContext-gsec.xml} consumes both names by reference (see the
     * {@code securityService} constructor); the legacy XML used {@code <alias>} elements
     * to satisfy that contract.
     *
     * <p>Java config has no native alias declaration, so we register the aliases via a
     * {@link BeanFactoryPostProcessor} that runs before any bean is instantiated. Both
     * aliases must be wired at registry level (not via additional {@code @Bean} methods)
     * so the underlying singleton is the actual {@code userManager} instance &mdash;
     * gsec's code that pulls {@code userDetailsManager} and {@code userService} gets the
     * same object back, preserving the legacy semantics.
     *
     * <p>Declared {@code static} so Spring instantiates it early (before any
     * {@code @Configuration} class processing) without leaking a partial
     * {@link SecurityConfig} instance.
     */
    @Bean
    public static BeanFactoryPostProcessor userManagerAliases() {
        return beanFactory -> {
            if ( beanFactory instanceof BeanDefinitionRegistry ) {
                BeanDefinitionRegistry registry = ( BeanDefinitionRegistry ) beanFactory;
                registry.registerAlias( "userManager", "userDetailsManager" );
                registry.registerAlias( "userManager", "groupManager" );
            }
        };
    }

    /**
     * Gemma-specific legacy-aware password encoder. Recognizes both the legacy
     * SHA-1(rawPassword + "{" + username + "}") format (bare 40-hex digest, no prefix --
     * see sql/init-data.sql) AND new BCrypt hashes ({bcrypt} prefix per
     * DelegatingPasswordEncoder convention). New encodings produce BCrypt; legacy hashes
     * are flagged for upgrade-on-next-login via
     * {@link PasswordEncoder#upgradeEncoding(String)}.
     *
     * <p>Declared with the explicit id {@code passwordEncoder} so it is injected by id
     * into {@code daoAuthenticationProvider} below AND into
     * {@code ubic.gemma.core.security.authentication.UserManagerImpl} (which still uses
     * field injection by type but which gsec's XML may shadow with a fallback bean of the
     * same id).
     */
    @Bean(name = "passwordEncoder")
    public PasswordEncoder passwordEncoder() {
        return new GemmaLegacyAwarePasswordEncoder();
    }

    /**
     * Custom {@code DaoAuthenticationProvider} that binds the username to the password
     * encoder's ThreadLocal so {@link GemmaLegacyAwarePasswordEncoder} can recompute the
     * legacy SHA-1(rawPassword + "{" + username + "}") hash.
     *
     * <p>The XML configured this as a plain bean rather than via the
     * {@code <s:authentication-provider user-service-ref="...">} namespace shortcut,
     * because that shortcut only builds a vanilla
     * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}.
     * The Java equivalent is the same: build the provider, set its
     * {@link UserDetailsService} and {@link PasswordEncoder} explicitly.
     */
    @Bean(name = "daoAuthenticationProvider")
    public LegacyAwareDaoAuthenticationProvider daoAuthenticationProvider(
            @Qualifier("userManager") UserDetailsService userManager,
            @Qualifier("passwordEncoder") PasswordEncoder passwordEncoder,
            @Qualifier("userManager") UserDetailsPasswordService userDetailsPasswordService ) {
        LegacyAwareDaoAuthenticationProvider provider = new LegacyAwareDaoAuthenticationProvider();
        provider.setUserDetailsService( userManager );
        provider.setPasswordEncoder( passwordEncoder );
        // Spring Security 6 password-upgrade hook: when the encoder reports
        // upgradeEncoding(stored) == true after a successful auth, the framework
        // calls userDetailsPasswordService.updatePassword(user, newEncoded) automatically.
        // UserManagerImpl IS the userDetailsService AND implements UserDetailsPasswordService,
        // so this is the same bean — but the property must be set explicitly because
        // DaoAuthenticationProvider does not auto-detect when the two implementations
        // coincide. See LegacyAwareDaoAuthenticationProvider class javadoc.
        provider.setUserDetailsPasswordService( userDetailsPasswordService );
        return provider;
    }

    /**
     * {@link RunAsManagerImpl} keyed by {@code ${gemma.runas.password}} with role prefix
     * {@code GROUP_}. This is the manager consulted by the method-security interceptor
     * when {@code @Secured("RUN_AS_ADMIN")} is encountered, producing a temporary
     * authentication with the {@code GROUP_RUN_AS_ADMIN} authority.
     */
    @Bean(name = "runAsManager")
    public RunAsManager runAsManager( @Value("${gemma.runas.password}") String runAsPassword ) {
        RunAsManagerImpl m = new RunAsManagerImpl();
        m.setRolePrefix( "GROUP_" );
        m.setKey( runAsPassword );
        return m;
    }

    /**
     * Provider that validates the temporary {@code RunAsUserToken} produced by
     * {@link #runAsManager(String)}. The key MUST match the run-as manager's key, else
     * {@link AuthorizationServiceException} is thrown at the {@code authenticate} call.
     */
    @Bean(name = "runAsAuthenticationProvider")
    public RunAsImplAuthenticationProvider runAsAuthenticationProvider(
            @Value("${gemma.runas.password}") String runAsPassword ) {
        RunAsImplAuthenticationProvider provider = new RunAsImplAuthenticationProvider();
        provider.setKey( runAsPassword );
        return provider;
    }

    /**
     * The Gemma {@link AuthenticationManager}.
     *
     * <p>Spring Security 6 retires the deprecated XML
     * {@code <s:authentication-manager>} namespace in favour of an explicit
     * {@link ProviderManager} bean. Order of providers preserved from the XML
     * (daoAuthenticationProvider first, then runAsAuthenticationProvider, then
     * anonymousAuthenticationProvider). The first provider that {@code supports} the
     * incoming token wins; ordering matters for the case where multiple providers claim
     * a token type.
     *
     * <p>The XML used {@code alias="authenticationManager"} which produced a bean
     * registered under both the namespace's internal id AND {@code authenticationManager};
     * the explicit {@code @Bean(name = "authenticationManager")} produces the same
     * effective registration.
     *
     * <p>{@code anonymousAuthenticationProvider} comes from gsec's
     * {@code applicationContext-gsec.xml} (re-imported here via {@link ImportResource}).
     */
    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManager(
            @Qualifier("daoAuthenticationProvider") AuthenticationProvider daoAuthenticationProvider,
            @Qualifier("runAsAuthenticationProvider") AuthenticationProvider runAsAuthenticationProvider,
            @Qualifier("anonymousAuthenticationProvider") AuthenticationProvider anonymousAuthenticationProvider ) {
        return new ProviderManager( Arrays.asList(
                daoAuthenticationProvider,
                runAsAuthenticationProvider,
                anonymousAuthenticationProvider ) );
    }

    /**
     * Session registry consumed by the {@code <s:concurrency-control>} element in
     * {@code gemma-web}'s {@code applicationContext-security.xml}. Single-session enforcement
     * is wired on the web side; this bean is the shared store.
     *
     * <p>Works in conjunction with the {@code HttpSessionEventPublisher} configured in
     * {@code web.xml}.
     */
    @Bean(name = "sessionRegistry")
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Stock Spring Security {@link LoggerListener} that logs authentication events at INFO.
     */
    @Bean(name = "authenticationLoggerListener")
    public LoggerListener authenticationLoggerListener() {
        return new LoggerListener();
    }

    // -----------------------------------------------------------------------------------
    // After-invocation providers (Gemma-specific filters layered on top of gsec's set)
    //
    // These four beans were defined in the XML's tail section. They are looked up by name
    // from MethodSecurityConfig.AFTER_INVOCATION_PROVIDER_BEAN_NAMES (only three are
    // currently registered into the AfterInvocationProviderManager: the two collection
    // filters; the single-CS filter and the diff-expr collection filter are present-but-
    // not-wired, retained for parity with the XML). Bean ids match the XML ids exactly.
    // -----------------------------------------------------------------------------------

    private static final List<Permission> ADMIN_OR_READ = Arrays.asList(
            BasePermission.ADMINISTRATION,
            BasePermission.READ );

    @Bean(name = "afterAclCompositeSequenceCollectionRead")
    public AclEntryAfterInvocationCompositeSequenceCollectionByArrayDesignFilteringProvider afterAclCompositeSequenceCollectionRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationCompositeSequenceCollectionByArrayDesignFilteringProvider p =
                new AclEntryAfterInvocationCompositeSequenceCollectionByArrayDesignFilteringProvider(
                        aclService,
                        "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ",
                        ADMIN_OR_READ );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    @Bean(name = "afterAclCompositeSequenceRead")
    public AclEntryAfterInvocationCompositeSequenceByArrayDesignFilteringProvider afterAclCompositeSequenceRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationCompositeSequenceByArrayDesignFilteringProvider p =
                new AclEntryAfterInvocationCompositeSequenceByArrayDesignFilteringProvider(
                        aclService,
                        "AFTER_ACL_COMPOSITE_SEQUENCE_READ",
                        ADMIN_OR_READ );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    @Bean(name = "afterAclDataVectorCollectionRead")
    public AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider afterAclDataVectorCollectionRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider p =
                new AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider(
                        aclService,
                        "AFTER_ACL_DATA_VECTOR_COLLECTION_READ",
                        ADMIN_OR_READ );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    @Bean(name = "afterAclDifferentialExpressionAnalysisResultCollectionRead")
    public AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider afterAclDifferentialExpressionAnalysisResultCollectionRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider p =
                new AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider(
                        aclService,
                        "AFTER_ACL_DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_COLLECTION_READ",
                        ADMIN_OR_READ );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    // -----------------------------------------------------------------------------------
    // Phase 3 AfterInvocation Phase B: Gemma-owned replacements for gsec providers.
    // These were defined in the XML's tail section (phase2-acl-migrate added them); they
    // are looked up by name from MethodSecurityConfig.AFTER_INVOCATION_PROVIDER_BEAN_NAMES.
    // Same semantics as the gsec beans they replaced, but live in gemma-core so we can
    // drop the gsec class dependency. Bean ids match the XML ids exactly.
    //
    // Each provider needs processDomainObjectClass set so the AfterInvocationProviderManager
    // chain can correctly dispatch on the return type (Securable for entity providers,
    // SecureValueObject for the value-object providers).
    // -----------------------------------------------------------------------------------

    private static final List<Permission> READ_ONLY = Arrays.asList(
            BasePermission.READ );

    private static final List<Permission> ADMIN_OR_WRITE = Arrays.asList(
            BasePermission.ADMINISTRATION,
            BasePermission.WRITE );

    private static final List<Permission> ADMIN_OR_WRITE_OR_READ = Arrays.asList(
            BasePermission.ADMINISTRATION,
            BasePermission.WRITE,
            BasePermission.READ );

    /**
     * Gemma-owned replacement for gsec's {@code afterAclReadQuiet}. Same semantics
     * (READ-or-ADMIN check, null on denial) but lives in gemma-core. Used by 17
     * {@code @Secured({..., "AFTER_ACL_READ_QUIET"})} call sites. Cannot be expressed
     * as {@code @PostAuthorize}: that annotation has no "return null on denial" mode.
     */
    @Bean(name = "gemmaAfterAclReadQuiet")
    public AclEntryAfterInvocationQuietReadProvider gemmaAfterAclReadQuiet(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationQuietReadProvider p =
                new AclEntryAfterInvocationQuietReadProvider( aclService, ADMIN_OR_READ );
        p.setProcessDomainObjectClass( gemma.gsec.model.Securable.class );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    /**
     * Gemma-owned replacement for gsec's {@code afterAclValueObject}. Single-object ACL READ
     * check that also populates the SecureValueObject security-metadata fields (isPublic /
     * isShared / userOwned / userCanWrite) as a side-effect of the ACL fetch. Cannot be
     * expressed as {@code @PostAuthorize}: the side-effect on the returned VO is load-bearing
     * (drives lock / share / edit UI affordances in the web layer).
     */
    @Bean(name = "gemmaAfterAclValueObjectRead")
    public AclEntryAfterInvocationValueObjectReadProvider gemmaAfterAclValueObjectRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationValueObjectReadProvider p =
                new AclEntryAfterInvocationValueObjectReadProvider( aclService, READ_ONLY );
        p.setProcessDomainObjectClass( gemma.gsec.model.SecureValueObject.class );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    /**
     * Gemma-owned replacement for gsec's {@code afterAclValueObjectCollection}. Bulk ACL READ
     * filter over a collection of SecureValueObjects, populating per-row security-metadata
     * fields on every retained VO. Cannot be expressed as {@code @PostFilter}: the per-row
     * side-effect is load-bearing.
     */
    @Bean(name = "gemmaAfterAclValueObjectCollectionRead")
    public AclEntryAfterInvocationValueObjectCollectionReadProvider gemmaAfterAclValueObjectCollectionRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationValueObjectCollectionReadProvider p =
                new AclEntryAfterInvocationValueObjectCollectionReadProvider( aclService, READ_ONLY );
        p.setProcessDomainObjectClass( gemma.gsec.model.SecureValueObject.class );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    /**
     * Gemma-owned replacement for gsec's {@code afterAclValueObjectMap}. Bulk ACL READ filter
     * over a Map whose keys are SecureValueObjects, populating per-key security-metadata
     * fields on every retained key VO. Map values are NOT checked.
     */
    @Bean(name = "gemmaAfterAclValueObjectMapRead")
    public AclEntryAfterInvocationValueObjectMapReadProvider gemmaAfterAclValueObjectMapRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationValueObjectMapReadProvider p =
                new AclEntryAfterInvocationValueObjectMapReadProvider( aclService, READ_ONLY );
        p.setProcessDomainObjectClass( gemma.gsec.model.SecureValueObject.class );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    /**
     * Gemma-owned replacement for gsec's {@code afterAclMyDataRead}. Owner-and-permission
     * filter (ADMIN-or-WRITE) over Securable collections.
     */
    @Bean(name = "gemmaAfterAclMyDataRead")
    public AclEntryAfterInvocationOwnedCollectionFilteringProvider gemmaAfterAclMyDataRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationOwnedCollectionFilteringProvider p =
                new AclEntryAfterInvocationOwnedCollectionFilteringProvider( aclService, ADMIN_OR_WRITE );
        p.setProcessDomainObjectClass( gemma.gsec.model.Securable.class );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    /**
     * Gemma-owned replacement for gsec's {@code afterAclMyPrivateDataRead}. Private-and-readable
     * filter (ADMIN-or-WRITE-or-READ) over Securable collections.
     */
    @Bean(name = "gemmaAfterAclMyPrivateDataRead")
    public AclEntryAfterInvocationPrivateCollectionFilteringProvider gemmaAfterAclMyPrivateDataRead(
            @Qualifier("aclService") AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationPrivateCollectionFilteringProvider p =
                new AclEntryAfterInvocationPrivateCollectionFilteringProvider( aclService, ADMIN_OR_WRITE_OR_READ );
        p.setProcessDomainObjectClass( gemma.gsec.model.Securable.class );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    /**
     * Gemma-owned replacement for gsec's {@code afterAclStreamRead}. Lazy READ-or-ADMIN
     * filter for {@code Stream<? extends Securable>} return types.
     */
    @Bean(name = "gemmaAfterAclStreamRead")
    public AclEntryAfterInvocationStreamFilteringProvider gemmaAfterAclStreamRead(
            // gsec's AclService (not Spring's) — the Stream filter constructor wants the
            // gsec interface specifically. The aclService bean from GemmaAclConfiguration
            // is a GsecAclServiceAdapter which implements both interfaces.
            @Qualifier("aclService") gemma.gsec.acl.domain.AclService aclService,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        AclEntryAfterInvocationStreamFilteringProvider p =
                new AclEntryAfterInvocationStreamFilteringProvider( aclService, ADMIN_OR_READ );
        p.setProcessDomainObjectClass( gemma.gsec.model.Securable.class );
        return configureRetrievalStrategies( p, objectIdentityRetrievalStrategy, sidRetrievalStrategy );
    }

    /**
     * Apply the shared retrieval-strategy properties that every Gemma after-invocation
     * provider needs. Mirrors the {@code <property name="objectIdentityRetrievalStrategy" .../>}
     * and {@code <property name="sidRetrievalStrategy" .../>} children in the XML beans.
     */
    private static <T extends AbstractAclProvider> T configureRetrievalStrategies(
            T provider,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            SidRetrievalStrategy sidRetrievalStrategy ) {
        provider.setObjectIdentityRetrievalStrategy( objectIdentityRetrievalStrategy );
        provider.setSidRetrievalStrategy( sidRetrievalStrategy );
        return provider;
    }
}

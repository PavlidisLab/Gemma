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
package ubic.gemma.rest.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.RoleHierarchyVoter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 6 configuration for the Gemma RESTful API ({@code /rest/v2/**}).
 *
 * <p><b>Wiring status (Phase 1 of GEMMA_REST_STANDALONE_ROADMAP.md):</b> this
 * {@code @Configuration} class is picked up by gemma-rest's existing
 * {@code <context:component-scan base-package="ubic.gemma.rest"/>}
 * ({@code gemma-rest/src/main/resources/ubic/gemma/applicationContext-component-scan.xml}).
 * It is therefore active in any Spring root context that loads gemma-rest's
 * classpath XML — which today means both gemma-web's WAR boot AND the
 * gemma-rest standalone WAR boot (the latter activated via
 * {@code mvn -pl gemma-rest -P gemma-rest-war package}; see
 * {@code gemma-rest/src/main/webapp/WEB-INF/web.xml}).
 *
 * <p>The legacy {@code <s:http pattern="/rest/v2/**">} block in
 * {@code gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml}
 * (lines 41-47) coexists with this Java config for now. Removing the XML block
 * is roadmap §8 row 2; until that lands, both definitions register a
 * {@code SecurityFilterChain} for {@code /rest/v2/**} and Spring Security
 * applies them in registration order. The gemma-rest standalone WAR
 * deliberately omits gemma-web's {@code applicationContext-security.xml} from
 * its classpath, so there only this {@code @Bean} contributes the REST chain.
 *
 * <h2>Filter chain summary (translated from the legacy XML)</h2>
 * The legacy XML block was:
 * <pre>{@code
 * <s:http access-decision-manager-ref="httpAccessDecisionManager" pattern="/rest/v2/**"
 *         entry-point-ref="restAuthEntryPoint" realm="Gemma RESTful API">
 *     <s:anonymous granted-authority="IS_AUTHENTICATED_ANONYMOUSLY"/>
 *     <s:http-basic entry-point-ref="restAuthEntryPoint"/>
 *     <s:intercept-url pattern="/rest/v2/users/**" access="GROUP_USER"/>
 * </s:http>
 * }</pre>
 *
 * Mapping to Spring Security 6 idioms:
 * <ul>
 *   <li>{@code pattern="/rest/v2/**"} &rarr; {@link HttpSecurity#securityMatcher(String)}.</li>
 *   <li>{@code <s:intercept-url pattern="/rest/v2/users/**" access="GROUP_USER"/>} &rarr;
 *       {@code .authorizeHttpRequests(auth -> auth.requestMatchers("/rest/v2/users/**").hasAuthority("GROUP_USER").anyRequest().permitAll())}.
 *       The default rule ({@code anyRequest().permitAll()}) corresponds to the legacy
 *       "no intercept-url match" behavior &mdash; the XML had no fallback {@code <s:intercept-url>}
 *       inside this chain, and the outer chain in gemma-web had
 *       {@code <s:intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/>}
 *       which is functionally equivalent to permitAll for our anonymous-authenticated users.</li>
 *   <li>{@code <s:http-basic entry-point-ref="restAuthEntryPoint"/>} &rarr;
 *       {@code .httpBasic(basic -> basic.authenticationEntryPoint(restAuthEntryPoint))}.</li>
 *   <li>{@code entry-point-ref="restAuthEntryPoint"} (chain-level entry point) &rarr;
 *       {@code .exceptionHandling(eh -> eh.authenticationEntryPoint(restAuthEntryPoint))}.</li>
 *   <li>{@code <s:anonymous granted-authority="IS_AUTHENTICATED_ANONYMOUSLY"/>} &rarr;
 *       {@code .anonymous(anon -> anon.authorities("IS_AUTHENTICATED_ANONYMOUSLY"))}.
 *       Spring Security 6's default anonymous principal is "anonymousUser" with role
 *       ROLE_ANONYMOUS; the legacy XML overrode the authority to the marker token
 *       {@code IS_AUTHENTICATED_ANONYMOUSLY} which is referenced by access expressions
 *       elsewhere in the codebase. We preserve that override here.</li>
 *   <li>{@code realm="Gemma RESTful API"} &rarr; {@code .httpBasic(basic -> basic.realmName("Gemma RESTful API"))}.</li>
 *   <li>{@code access-decision-manager-ref="httpAccessDecisionManager"} &rarr; see
 *       {@linkplain #httpAccessDecisionManager(RoleHierarchyVoter) httpAccessDecisionManager bean below}.
 *       In Spring Security 6, {@code authorizeHttpRequests} uses {@code AuthorizationManager}
 *       rather than {@code AccessDecisionManager}; the legacy {@code AccessDecisionManager}
 *       bean is still defined here because it is consumed by code in
 *       {@code gemma-rest} ({@code DatasetsWebService}, {@code PlatformsWebService},
 *       {@code CacheControlHeaderDecorator}) that calls
 *       {@code accessDecisionManager.decide(...)} directly. Once those call sites are
 *       migrated to {@code AuthorizationManager}, the bean can be removed.</li>
 * </ul>
 *
 * <h2>Spring Security 6 idiom choices</h2>
 * <ul>
 *   <li><b>CSRF disabled.</b> This is a stateless REST API authenticated via HTTP
 *       Basic. The legacy XML did not configure CSRF explicitly; Spring Security's
 *       default (CSRF enabled) was effectively bypassed for the REST chain because
 *       no form-bound state existed. Explicitly disable to match the de-facto behavior
 *       and conform to REST-API conventions.</li>
 *   <li><b>Session creation policy: STATELESS.</b> REST clients pass HTTP Basic
 *       credentials on every request; no server-side session is needed. The legacy
 *       XML did not set {@code create-session}, so the namespace default
 *       ({@code ifRequired}) applied &mdash; this was an oversight, and the
 *       conventional REST-API choice (stateless) is the right one. Note this differs
 *       from the gemma-web chain (which uses sessions for the form-login flow).</li>
 *   <li><b>No {@code WebSecurityConfigurerAdapter}, no {@code antMatchers},
 *       no {@code authorizeRequests}.</b> Spring Security 6 deprecates these in
 *       favor of {@code SecurityFilterChain} + {@code requestMatchers} +
 *       {@code authorizeHttpRequests}.</li>
 * </ul>
 *
 * <h2>Open items at cutover time</h2>
 * <ul>
 *   <li>The gemma-web outer chain ({@code <s:http pattern="/**">}, lines 51-85) is
 *       NOT migrated by this class. It remains in XML for the form-login web UI.
 *       Care must be taken at cutover so the {@code /rest/v2/**} chain is registered
 *       before the catch-all {@code /**} chain (Spring Security evaluates filter
 *       chains in registration order; the {@code SecurityFilterChain} bean with the
 *       {@code @Order(1)} annotation should be applied when both chains coexist).</li>
 *   <li>Direct {@code AccessDecisionManager.decide(...)} call sites in
 *       {@code DatasetsWebService.java:3082}, {@code PlatformsWebService.java:318},
 *       and {@code CacheControlHeaderDecorator.java} reference the bean by type
 *       ({@code @Autowired AccessDecisionManager}). Once this config is wired and the
 *       legacy XML bean is removed, those injections will resolve to the bean defined
 *       here. TODO: migrate those sites to {@code AuthorizationManager} (Spring 6 idiom).</li>
 * </ul>
 *
 * @see RestAuthEntryPoint
 */
@Configuration
@EnableWebSecurity
public class RestSecurityConfig {

    /**
     * REST filter chain. Matches {@code /rest/v2/**}; everything outside that pattern
     * falls through to the next-registered chain (currently the gemma-web XML
     * {@code <s:http pattern="/**">} block).
     *
     * <p>The legacy XML used {@code access-decision-manager-ref="httpAccessDecisionManager"}
     * to plug in a custom {@code AffirmativeBased} manager that knew about Gemma's role
     * hierarchy (via {@code roleHierarchyVoter}). In Spring Security 6's
     * {@code authorizeHttpRequests}, role hierarchy is applied automatically when a
     * {@code RoleHierarchy} bean is present in the context (which it is &mdash; defined
     * in {@code ubic/gemma/core/security/applicationContext-gsec.xml}). So {@code hasAuthority("GROUP_USER")}
     * here will correctly grant access to any authority that the role hierarchy resolves
     * as &ge; {@code GROUP_USER}.
     */
    @Bean
    public SecurityFilterChain restSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("restAuthEntryPoint") AuthenticationEntryPoint restAuthEntryPoint,
            TokenStore tokenStore
    ) throws Exception {
        return http
                .securityMatcher( "/rest/v2/**" )
                .authorizeHttpRequests( auth -> auth
                        // /login is the credential-trade endpoint and must be reachable without
                        // an existing credential. Everything else under /users/** still requires
                        // GROUP_USER, satisfied by either a Bearer token (resolved by the filter
                        // below) or an HTTP Basic header.
                        .requestMatchers( "/rest/v2/login" ).permitAll()
                        .requestMatchers( "/rest/v2/users/**" ).hasAuthority( "GROUP_USER" )
                        .anyRequest().permitAll() )
                // Bearer-token filter runs before the Basic-auth filter so a curator-UI request
                // bearing Authorization: Bearer <opaque> short-circuits before Spring's BasicAuth
                // tries to decode the same header. If the token is absent / unknown the filter is
                // a no-op and BasicAuth gets its normal shot — preserves the legacy CLI flow.
                // See AUTH_FOR_SPA_RECCE.md Option C.
                .addFilterBefore( new BearerTokenAuthenticationFilter( tokenStore ), BasicAuthenticationFilter.class )
                .httpBasic( basic -> basic
                        .realmName( "Gemma RESTful API" )
                        .authenticationEntryPoint( restAuthEntryPoint ) )
                .exceptionHandling( eh -> eh
                        .authenticationEntryPoint( restAuthEntryPoint ) )
                .anonymous( anon -> anon
                        .authorities( "IS_AUTHENTICATED_ANONYMOUSLY" ) )
                .sessionManagement( s -> s
                        .sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
                .csrf( csrf -> csrf.disable() )
                .build();
    }

    /**
     * Legacy {@link AccessDecisionManager} bean carried over from the gemma-web XML.
     *
     * <p>This is the {@code httpAccessDecisionManager} bean defined in
     * {@code gemma-web/applicationContext-security.xml} lines 9-18. It is still needed
     * because three call sites in gemma-rest inject it directly to call
     * {@code .decide(...)}:
     * <ul>
     *   <li>{@code DatasetsWebService.java:222} &rarr; used at line ~3082</li>
     *   <li>{@code PlatformsWebService.java:87} &rarr; used at line ~318</li>
     *   <li>{@code CacheControlHeaderDecorator.java:31}</li>
     * </ul>
     *
     * <p>The composition matches the XML exactly:
     * {@link AffirmativeBased} (allowIfAllAbstainDecisions=true) over
     * {@link WebExpressionVoter} + {@link RoleHierarchyVoter} + {@link AuthenticatedVoter}.
     * {@code AuthenticatedVoter} is what lets {@code IS_AUTHENTICATED_ANONYMOUSLY}
     * resolve as an access expression token.
     *
     * <p>TODO (post-cutover): migrate the three call sites to Spring Security 6's
     * {@code AuthorizationManager} API and remove this bean. {@code AccessDecisionManager}
     * is deprecated for removal in Spring Security 7.
     *
     * @param roleHierarchyVoter the {@code roleHierarchyVoter} bean from gsec
     *                           ({@code applicationContext-gsec.xml}). The
     *                           {@code @Qualifier} pins the lookup to the bean id rather
     *                           than relying on type-only resolution &mdash; gsec defines
     *                           several {@link AccessDecisionVoter} beans.
     */
    @Bean(name = "httpAccessDecisionManager")
    public AccessDecisionManager httpAccessDecisionManager(
            @Qualifier("roleHierarchyVoter") RoleHierarchyVoter roleHierarchyVoter
    ) {
        List<AccessDecisionVoter<?>> voters = Arrays.asList(
                new WebExpressionVoter(),
                roleHierarchyVoter,
                new AuthenticatedVoter()
        );
        AffirmativeBased manager = new AffirmativeBased( voters );
        manager.setAllowIfAllAbstainDecisions( true );
        return manager;
    }
}

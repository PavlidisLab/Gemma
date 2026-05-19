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
package ubic.gemma.rest.analytics.ga4;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.web.context.request.RequestAttributes;

import java.util.Arrays;

/**
 * Java replacement for {@code applicationContext-analytics.xml}, wiring the Google Analytics 4
 * provider used by the gemma-rest module's request/event listeners.
 * <p>
 * Active only under the {@code web} profile (matching the {@code profile="web"} attribute on the
 * original XML beans file) — the CLI / non-web profiles don't ship a GA provider.
 * <p>
 * The exposed bean implements {@link ubic.gemma.rest.analytics.AnalyticsProvider} and is autowired
 * by type into the request/event listeners (see {@code AnalyticsApplicationEventListener} and
 * {@code AnalyticsRequestEventListener}); the bean id is the Spring-default short class name
 * ({@code googleAnalytics4Provider}) which matches what the XML produced.
 */
@Configuration
@Profile("web")
public class AnalyticsConfig {

    @Value("${ga.tracker}")
    private String trackerId;

    @Value("${ga.secretKey}")
    private String secretKey;

    @Value("${ga.debug}")
    private boolean debug;

    @Bean
    public GoogleAnalytics4Provider googleAnalytics4Provider( AuthenticationTrustResolver authenticationTrustResolver ) {
        GoogleAnalytics4Provider provider = new GoogleAnalytics4Provider( trackerId, secretKey );
        provider.setClientIdRetrievalStrategy( clientIdRetrievalStrategy() );
        provider.setUserIdRetrievalStrategy( new AuthenticationBasedUserIdRetrievalStrategy( authenticationTrustResolver ) );
        provider.setDebug( debug );
        return provider;
    }

    /**
     * Composite client-id retrieval strategy ordered the same way as the original XML:
     * <ol>
     *   <li>request header (X-Client-Id / similar) — fastest path, no session needed</li>
     *   <li>session-scoped request attribute — sticky across a logged-in session</li>
     *   <li>request-scoped request attribute — falls back to per-request id</li>
     * </ol>
     */
    private CompositeClientIdRetrievalStrategy clientIdRetrievalStrategy() {
        RequestAttributesBasedClientIdRetrievalStrategy sessionScoped = new RequestAttributesBasedClientIdRetrievalStrategy();
        sessionScoped.setScope( RequestAttributes.SCOPE_SESSION );
        RequestAttributesBasedClientIdRetrievalStrategy requestScoped = new RequestAttributesBasedClientIdRetrievalStrategy();
        requestScoped.setScope( RequestAttributes.SCOPE_REQUEST );
        return new CompositeClientIdRetrievalStrategy( Arrays.asList(
                new RequestHeaderBasedClientIdRetrievalStrategy(),
                sessionScoped,
                requestScoped
        ) );
    }
}

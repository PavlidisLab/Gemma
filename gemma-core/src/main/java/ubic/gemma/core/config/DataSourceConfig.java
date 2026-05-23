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
package ubic.gemma.core.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.security.authentication.ManualAuthenticationServiceBasedSecurityContextFactory;
import ubic.gemma.core.util.DummyMailSender;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Renovations Phase 3: Java-config replacement for {@code applicationContext-dataSource.xml}.
 * <p>
 * Ports four concerns from the XML, preserving bean ids exactly (the {@code dataSource} id is
 * referenced by name from {@code applicationContext-hibernate.xml}'s {@code sessionFactory},
 * from {@code applicationContext-dataSourceInitializer.xml}'s test-profile
 * {@code DataSourceInitializer} beans, and from many @Autowired DataSource injections in
 * persistence code — breaking the name would break the world):
 * <ol>
 *   <li>Production / dev {@code dataSource} — Hikari, pointed at {@code gemma.db.*}.</li>
 *   <li>Test {@code dataSource} — Hikari, pointed at {@code gemma.testdb.*}.</li>
 *   <li>{@code groupAgentSecurityContext} — lazy factory bean used by the scheduler to run
 *       scheduled jobs under the GROUP_AGENT identity; the test profile uses different
 *       credentials.</li>
 *   <li>{@code mailSender} — real {@link JavaMailSenderImpl} in production, a
 *       {@link DummyMailSender} elsewhere.</li>
 * </ol>
 * The XML also declared two stub {@code java.lang.Object} beans named
 * {@code createDatabaseInitializer} and {@code dataSourceInitializer} under the
 * production/dev profile so that {@code applicationContext-hibernate.xml}'s
 * {@code depends-on="createDatabaseInitializer"} would resolve. In the test profile,
 * {@code applicationContext-dataSourceInitializer.xml} provides real
 * {@code DataSourceInitializer} beans under the same ids. We preserve that arrangement by
 * declaring the stubs here under {@code @Profile("!test & !testdb")}; the test-profile XML's
 * beans then live alongside (different profile) without collision.
 * <p>
 * The MySQL-specific connection properties bean ({@code dataSourceProps}) is preserved as a
 * private helper rather than a Spring bean — it was only ever referenced by the two
 * {@code dataSource} bean definitions in the same XML, never by name from outside.
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL Connector/J performance + correctness tuning, sourced from {@code gemma.db.hikari.*}
     * properties (see {@code default.properties}). See
     * https://dev.mysql.com/doc/connectors/en/connector-j-connp-props-performance-extensions.html
     * for the {@code useCursorFetch} / {@code rewriteBatchedStatements} pair, and the MySQL
     * server documentation for the {@code sessionVariables} sql_mode override (drops
     * ONLY_FULL_GROUP_BY because Gemma's HQL produces aggregates without listing every selected
     * non-aggregate in GROUP BY, a pattern strict mode forbids). Externalising these to
     * properties lets test / per-environment overrides land without a Java rebuild.
     */
    @Value("${gemma.db.hikari.useCursorFetch}")
    private String hikariUseCursorFetch;

    @Value("${gemma.db.hikari.rewriteBatchedStatements}")
    private String hikariRewriteBatchedStatements;

    @Value("${gemma.db.hikari.connectionTimeZone}")
    private String hikariConnectionTimeZone;

    @Value("${gemma.db.hikari.sessionVariables}")
    private String hikariSessionVariables;

    /**
     * Pool-level Hikari knobs (distinct from the {@code dataSourceProperties} driver-level knobs
     * above). All four default to {@code null} so that an unset key falls through to Hikari's own
     * built-in defaults rather than overriding them with zero. See {@code deploy/env.example} for
     * the per-environment guidance (tunneled prod wants {@code keepaliveTime=60000} and
     * {@code maxLifetime=1800000} to reap server- or tunnel-closed sockets before Hikari hands
     * them back out).
     */
    @Value("${gemma.db.hikari.keepaliveTime:#{null}}")
    private Long hikariKeepaliveTime;

    @Value("${gemma.db.hikari.maxLifetime:#{null}}")
    private Long hikariMaxLifetime;

    @Value("${gemma.db.hikari.connectionTimeout:#{null}}")
    private Long hikariConnectionTimeout;

    @Value("${gemma.db.hikari.idleTimeout:#{null}}")
    private Long hikariIdleTimeout;

    private Properties hikariDataSourceProperties() {
        Properties props = new Properties();
        // Enable server-side cursor fetching so large result sets stream instead of buffering.
        props.setProperty( "useCursorFetch", hikariUseCursorFetch );
        // Merge multiple insert/update statements into a single batch round-trip.
        props.setProperty( "rewriteBatchedStatements", hikariRewriteBatchedStatements );
        // Default timezone for storage of DATETIME mapped to java.util.Date.
        props.setProperty( "connectionTimeZone", hikariConnectionTimeZone );
        // Drop ONLY_FULL_GROUP_BY from sql_mode for the connection's session.
        props.setProperty( "sessionVariables", hikariSessionVariables );
        return props;
    }

    /**
     * Apply pool-level Hikari setters from {@code gemma.db.hikari.*} properties. Each setter
     * fires only if the corresponding property is set; unset keys preserve Hikari's own defaults
     * rather than coercing to zero. Shared between the prod / dev and test datasource beans so
     * tunneled-tunnel-survival tuning applies uniformly.
     */
    private void applyHikariPoolSettings( HikariDataSource ds ) {
        if ( hikariKeepaliveTime != null ) {
            ds.setKeepaliveTime( hikariKeepaliveTime );
        }
        if ( hikariMaxLifetime != null ) {
            ds.setMaxLifetime( hikariMaxLifetime );
        }
        if ( hikariConnectionTimeout != null ) {
            ds.setConnectionTimeout( hikariConnectionTimeout );
        }
        if ( hikariIdleTimeout != null ) {
            ds.setIdleTimeout( hikariIdleTimeout );
        }
    }

    // ---------------------------------------------------------------------------
    // dataSource — production / dev
    // ---------------------------------------------------------------------------

    /**
     * Production / dev datasource. Active under the {@code production} and {@code dev} profiles
     * (Gemma uses the same physical database in both — dev points at the prod schema for
     * local-against-prod debugging; only the connection pool sizes differ via
     * {@code gemma.db.*} property overrides).
     */
    @Bean(name = "dataSource", destroyMethod = "close")
    @Profile({ EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV })
    public DataSource dataSource(
            @Value("${gemma.db.user}") String user,
            @Value("${gemma.db.password}") String password,
            @Value("${gemma.db.url}") String url,
            @Value("${gemma.db.maximumPoolSize}") int maximumPoolSize,
            @Value("${gemma.db.minimumIdle}") int minimumIdle ) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName( "gemma" );
        ds.setDriverClassName( "com.mysql.cj.jdbc.Driver" );
        ds.setUsername( user );
        ds.setPassword( password );
        ds.setJdbcUrl( url );
        ds.setDataSourceProperties( hikariDataSourceProperties() );
        ds.setMaximumPoolSize( maximumPoolSize );
        ds.setMinimumIdle( minimumIdle );
        applyHikariPoolSettings( ds );
        return ds;
    }

    // ---------------------------------------------------------------------------
    // dataSource — test / testdb
    // ---------------------------------------------------------------------------

    /**
     * Test datasource. Active under {@code test} (unit + integration tests) and {@code testdb}
     * (Flyway baseline / migration verification against a disposable schema). Bean id is the
     * same as the prod datasource — only one of the two definitions is active at a time, so
     * downstream {@code @Autowired DataSource} injections work uniformly. The test-profile
     * {@code DataSourceInitializer} beans in
     * {@code applicationContext-dataSourceInitializer.xml} reference {@code dataSource} by name
     * and so consume whichever of these two is active.
     */
    @Bean(name = "dataSource", destroyMethod = "close")
    @Profile({ EnvironmentProfiles.TEST, "testdb" })
    public DataSource testDataSource(
            @Value("${gemma.testdb.user}") String user,
            @Value("${gemma.testdb.password}") String password,
            @Value("${gemma.testdb.url}") String url,
            @Value("${gemma.testdb.maximumPoolSize}") int maximumPoolSize,
            @Value("${gemma.testdb.minimumIdle}") int minimumIdle ) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName( "com.mysql.cj.jdbc.Driver" );
        ds.setUsername( user );
        ds.setPassword( password );
        ds.setJdbcUrl( url );
        ds.setDataSourceProperties( hikariDataSourceProperties() );
        ds.setMaximumPoolSize( maximumPoolSize );
        ds.setMinimumIdle( minimumIdle );
        applyHikariPoolSettings( ds );
        // Catch connection leaks in tests (60s threshold).
        ds.setLeakDetectionThreshold( 60_000L );
        return ds;
    }

    // ---------------------------------------------------------------------------
    // groupAgentSecurityContext — used by the scheduler to authenticate scheduled jobs
    // ---------------------------------------------------------------------------

    /**
     * Lazy factory for the {@code GROUP_AGENT} {@link
     * org.springframework.security.core.context.SecurityContext}. Only built when the scheduler
     * actually launches a job. Active under production / dev where {@code gemma.agent.*}
     * properties are populated.
     * <p>
     * Returned as the {@link ManualAuthenticationServiceBasedSecurityContextFactory} (a Spring
     * {@code FactoryBean}); Spring unwraps it for {@code @Autowired SecurityContext}
     * injections and exposes it as the raw factory for {@code BeanFactory#getBean("&...")}
     * callers.
     */
    @Bean(name = "groupAgentSecurityContext")
    @Lazy
    @Profile({ EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV })
    public ManualAuthenticationServiceBasedSecurityContextFactory groupAgentSecurityContext(
            ubic.gemma.core.security.authentication.ManualAuthenticationService manualAuthenticationService,
            @Value("${gemma.agent.userName}") String userName,
            @Value("${gemma.agent.password}") String password ) {
        ManualAuthenticationServiceBasedSecurityContextFactory factory =
                new ManualAuthenticationServiceBasedSecurityContextFactory();
        factory.setManualAuthenticationService( manualAuthenticationService );
        factory.setUserName( userName );
        factory.setPassword( password );
        return factory;
    }

    /**
     * Test-profile variant of {@link #groupAgentSecurityContext}: same bean id, different
     * credential keys ({@code gemma.testdb.agent.*}) so test runs don't authenticate against
     * production credentials.
     */
    @Bean(name = "groupAgentSecurityContext")
    @Lazy
    @Profile({ EnvironmentProfiles.TEST, "testdb" })
    public ManualAuthenticationServiceBasedSecurityContextFactory testGroupAgentSecurityContext(
            ubic.gemma.core.security.authentication.ManualAuthenticationService manualAuthenticationService,
            @Value("${gemma.testdb.agent.userName}") String userName,
            @Value("${gemma.testdb.agent.password}") String password ) {
        ManualAuthenticationServiceBasedSecurityContextFactory factory =
                new ManualAuthenticationServiceBasedSecurityContextFactory();
        factory.setManualAuthenticationService( manualAuthenticationService );
        factory.setUserName( userName );
        factory.setPassword( password );
        return factory;
    }

    // ---------------------------------------------------------------------------
    // mailSender — real in production, dummy elsewhere
    // ---------------------------------------------------------------------------

    @Bean(name = "mailSender")
    @Profile(EnvironmentProfiles.PRODUCTION)
    public MailSender mailSender(
            @Value("${mail.host}") String host,
            @Value("${mail.protocol}") String protocol,
            @Value("${mail.username}") String username,
            @Value("${mail.password}") String password ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost( host );
        sender.setProtocol( protocol );
        sender.setUsername( username );
        sender.setPassword( password );
        return sender;
    }

    @Bean(name = "mailSender")
    @Profile("!" + EnvironmentProfiles.PRODUCTION)
    public MailSender dummyMailSender() {
        return new DummyMailSender();
    }

    // ---------------------------------------------------------------------------
    // Stub beans for sessionFactory's depends-on chain (production / dev only)
    // ---------------------------------------------------------------------------

    /**
     * Dummy {@code createDatabaseInitializer} stub. {@code applicationContext-hibernate.xml}'s
     * {@code sessionFactory} declares {@code depends-on="createDatabaseInitializer"} so that
     * in test runs the empty gemdtest DB exists before Hibernate's hbm2ddl=create fires; the
     * real {@code DataSourceInitializer} bean by this id is provided by
     * {@code applicationContext-dataSourceInitializer.xml} under {@code @Profile("test")}.
     * Outside the test profile that bean isn't loaded, so {@code depends-on} would fail
     * resolution — hence the stub here under the complementary profile.
     */
    @Bean(name = "createDatabaseInitializer")
    @Profile({ EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV })
    public Object createDatabaseInitializerStub() {
        return new Object();
    }

    /**
     * Dummy {@code dataSourceInitializer} stub. Mirrors {@link #createDatabaseInitializerStub()};
     * other beans (test-only) may declare {@code depends-on="dataSourceInitializer"} and the
     * legacy XML kept a stub for symmetry. Preserved here verbatim.
     */
    @Bean(name = "dataSourceInitializer")
    @Profile({ EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV })
    public Object dataSourceInitializerStub() {
        return new Object();
    }
}

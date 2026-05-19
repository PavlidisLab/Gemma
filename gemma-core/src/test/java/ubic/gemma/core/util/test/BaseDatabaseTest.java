package ubic.gemma.core.util.test;

import ubic.gemma.core.security.gsec.acl.AclAuthorizationStrategyImpl;
import ubic.gemma.core.security.gsec.acl.AclSidRetrievalStrategyImpl;
import ubic.gemma.core.security.gsec.acl.ObjectIdentityRetrievalStrategyImpl;
import ubic.gemma.core.security.gsec.acl.domain.AclDao;
import ubic.gemma.core.security.gsec.acl.domain.AclDaoImpl;
import ubic.gemma.core.security.gsec.acl.domain.AclService;
import ubic.gemma.core.security.gsec.acl.domain.AclServiceImpl;
import org.flywaydb.core.Flyway;
import org.h2.Driver;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.hibernate.HibernateSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Minimalist test setup with an in-memory database and transactional test cases.
 * <p>
 * Renovations Phase 2: SessionFactory is built natively from hibernate.cfg.xml via
 * {@link HibernateSessionFactoryBean} (Spring 6's {@code org.springframework.orm.hibernate5.LocalSessionFactoryBean}
 * no longer works with Hibernate 6 due to a removed {@code ReflectionManager.reset()} call). We use
 * {@link HibernateTransactionManager} + {@code SpringSessionContext} so DAOs that call
 * {@code sessionFactory.getCurrentSession()} see the transactional session.
 * <p>
 * Phase 3 first-wave: schema and seed data come from versioned Flyway migrations under
 * {@code db/migration/h2/} (V1__hibernate_baseline.sql, V2__schema_extras.sql, V3__seed_data.sql).
 * The Flyway bean materializes the schema before the SessionFactory is built; Hibernate runs in
 * {@code hbm2ddl.auto=none}. The legacy {@code DataSourceInitializer} + {@code DatabaseSchemaPopulator}
 * + {@code InitialDataPopulator} chain is gone from the H2 path (the populators stay on disk for
 * the MySQL integration test wiring that still uses them).
 *
 * @author poirigui
 */
@ActiveProfiles(EnvironmentProfiles.TEST)
public abstract class BaseDatabaseTest extends AbstractTransactionalJUnit4SpringContextTests {

    protected abstract static class BaseDatabaseTestContextConfiguration {
        @Bean
        public DataSource dataSource() {
            DataSource ds = new SimpleDriverDataSource( new Driver(), "jdbc:h2:mem:gemdtest;MODE=MYSQL;DB_CLOSE_DELAY=-1" );
            new JdbcTemplate( ds ).execute( "drop all objects" );
            return ds;
        }

        /**
         * Flyway migration runner. Bean name {@code flyway} is a Spring Boot convention; bean order
         * matters here because {@link #sessionFactory(DataSource, Flyway)} declares a method-level
         * dependency so Hibernate boots only after the schema + seed data are applied.
         * <p>
         * We pin the migration location to {@code db/migration/h2/} so the H2 path is isolated from
         * the production MySQL migrations that will land in a sibling directory in the follow-on
         * session. {@code baselineOnMigrate=true} lets the same Flyway bean adopt an existing
         * (pre-Flyway) schema for any future external test fixture; in the in-memory H2 case the
         * schema is always empty at boot so it's a no-op.
         */
        @Bean(initMethod = "migrate")
        public Flyway flyway( DataSource dataSource ) {
            return Flyway.configure()
                    .dataSource( dataSource )
                    .locations( "classpath:db/migration/h2" )
                    .baselineOnMigrate( true )
                    .load();
        }

        @Bean
        public HibernateSessionFactoryBean sessionFactory( DataSource dataSource, Flyway flyway ) {
            HibernateSessionFactoryBean factory = new HibernateSessionFactoryBean();
            factory.setDataSource( dataSource );
            factory.setConfigLocation( HibernateSessionFactoryBean.defaultConfigLocation() );
            Properties props = new Properties();
            props.setProperty( "hibernate.dialect", H2Dialect.class.getName() );
            props.setProperty( "hibernate.current_session_context_class", "org.springframework.orm.hibernate5.SpringSessionContext" );
            props.setProperty( "hibernate.cache.use_second_level_cache", "false" );
            props.setProperty( "hibernate.cache.use_query_cache", "false" );
            props.setProperty( "hibernate.max_fetch_depth", "3" );
            props.setProperty( "hibernate.default_batch_fetch_size", "128" );
            props.setProperty( "hibernate.jdbc.batch_size", "32" );
            props.setProperty( "hibernate.jdbc.batch_versioned_data", "true" );
            props.setProperty( "hibernate.order_inserts", "true" );
            props.setProperty( "hibernate.order_updates", "true" );
            props.setProperty( "hibernate.show_sql", Settings.getString( "gemma.hibernate.show_sql" ) );
            props.setProperty( "hibernate.format_sql", Settings.getString( "gemma.hibernate.format_sql" ) );
            // Phase 3: Flyway is the schema source of truth. We tried `validate` first; it fails
            // with "wrong column type encountered in column [principal] in table [acl_sid]: found
            // [boolean (Types#BOOLEAN)], but expecting [bit (Types#INTEGER)]". H2 under MODE=MYSQL
            // implements BIT as BOOLEAN at the JDBC level, but Hibernate's validate expects
            // BIT->INTEGER (TINYINT). The Hibernate-generated DDL itself emitted "BIT not null" --
            // i.e. Hibernate creates the column, H2 maps it to BOOLEAN, then Hibernate's own
            // validator rejects it. There's no way to make the round-trip clean without changing
            // either the gsec HBM mappings (out of scope; gsec is external) or the H2 mode. So we
            // run `none` for now -- Flyway materialises the schema, Hibernate trusts it. The
            // production-MySQL session will revisit `validate` against the real prod schema dump
            // where the BIT->BOOLEAN H2 quirk doesn't apply. See FLYWAY_PROD_FOLLOWUP.md.
            props.setProperty( "hibernate.hbm2ddl.auto", "none" );
            factory.setHibernateProperties( props );
            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager( SessionFactory sessionFactory ) {
            return new HibernateTransactionManager( sessionFactory );
        }

        @Bean
        public AclDao aclDao( SessionFactory sessionFactory, SidRetrievalStrategy sidRetrievalStrategy ) {
            // Match the authority Gemma tests actually carry (@WithMockUser(authorities="GROUP_ADMIN")
            // or runAsAdmin()) — the prior "ADMIN" string was a stale relic.
            AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
                    new GrantedAuthority[] { new SimpleGrantedAuthority( "GROUP_ADMIN" ), new SimpleGrantedAuthority( "GROUP_ADMIN" ), new SimpleGrantedAuthority( "GROUP_ADMIN" ) },
                    sidRetrievalStrategy );
            return new AclDaoImpl( sessionFactory,
                    aclAuthorizationStrategy,
                    new SpringCacheBasedAclCache( new ConcurrentMapCache( "acl" ),
                            new DefaultPermissionGrantingStrategy( new ConsoleAuditLogger() ),
                            aclAuthorizationStrategy ) );
        }

        @Bean
        public AclService aclService( AclDao aclDao ) {
            return new AclServiceImpl( aclDao );
        }

        @Bean
        public ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy() {
            return new ObjectIdentityRetrievalStrategyImpl();
        }

        @Bean
        public SidRetrievalStrategy sidRetrievalStrategy() {
            return new AclSidRetrievalStrategyImpl( new NullRoleHierarchy() );
        }
    }

    @Autowired
    protected SessionFactory sessionFactory;

    /**
     * Flush and clear the session after each test.
     * <p>
     * This ensures that any error in the test are caught and each test starts with a clean slate.
     */
    @After
    public final void flushAndClearSession() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }
}

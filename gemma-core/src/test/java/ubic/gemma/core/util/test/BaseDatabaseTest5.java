package ubic.gemma.core.util.test;

import org.flywaydb.core.Flyway;
import org.h2.Driver;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.security.acl.AclAuthorizationStrategyImpl;
import ubic.gemma.core.security.acl.AclSidRetrievalStrategyImpl;
import ubic.gemma.core.security.acl.ObjectIdentityRetrievalStrategyImpl;
import ubic.gemma.core.security.acl.domain.AclDao;
import ubic.gemma.core.security.acl.domain.AclDaoImpl;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.core.security.acl.domain.AclServiceImpl;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.hibernate.HibernateSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * JUnit 5 (Jupiter) counterpart of {@link BaseDatabaseTest}.
 * <p>
 * Where {@link BaseDatabaseTest} inherits {@code AbstractTransactionalJUnit4SpringContextTests}
 * to get JUnit 4-style per-test transaction wrapping, this base uses Spring's
 * {@link Transactional} annotation directly. {@code @Transactional} at the class
 * level — combined with the {@code SpringExtension} inherited from
 * {@link BaseTest5} — gives every test method a per-method transaction that is
 * rolled back at end-of-test by default (Spring Test's standard
 * {@code TransactionalTestExecutionListener} behaviour). This reproduces the
 * JUnit 4 base class behaviour without inheriting from it.
 * <p>
 * The {@code @ContextConfiguration} location is intentionally NOT declared on
 * this base; subclasses provide their own inner {@code @Configuration} class
 * (extending {@link BaseDatabaseTestContextConfiguration}) the same way they
 * did under the JUnit 4 base.
 */
@ActiveProfiles(EnvironmentProfiles.TEST)
@Transactional
public abstract class BaseDatabaseTest5 extends BaseTest5 {

    protected abstract static class BaseDatabaseTestContextConfiguration {
        @Bean
        public DataSource dataSource() {
            DataSource ds = new SimpleDriverDataSource( new Driver(), "jdbc:h2:mem:gemdtest;MODE=MYSQL;DB_CLOSE_DELAY=-1" );
            new JdbcTemplate( ds ).execute( "drop all objects" );
            return ds;
        }

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
    @AfterEach
    public final void flushAndClearSession() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }
}

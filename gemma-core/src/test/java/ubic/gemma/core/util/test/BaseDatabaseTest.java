package ubic.gemma.core.util.test;

import gemma.gsec.acl.AclAuthorizationStrategyImpl;
import gemma.gsec.acl.AclSidRetrievalStrategyImpl;
import gemma.gsec.acl.ObjectIdentityRetrievalStrategyImpl;
import gemma.gsec.acl.domain.AclDao;
import gemma.gsec.acl.domain.AclDaoImpl;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.acl.domain.AclServiceImpl;
import org.h2.Driver;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
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
import ubic.gemma.persistence.initialization.DatabaseSchemaPopulator;
import ubic.gemma.persistence.initialization.InitialDataPopulator;

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

        @Bean
        public HibernateSessionFactoryBean sessionFactory( DataSource dataSource ) {
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
            // Tests rely on Hibernate-generated DDL; the DatabaseSchemaPopulator Hibernate branch is a no-op.
            props.setProperty( "hibernate.hbm2ddl.auto", "create" );
            factory.setHibernateProperties( props );
            return factory;
        }

        @Bean
        public DataSourceInitializer dataSourceInitializer( DataSource dataSource ) {
            DataSourceInitializer di = new DataSourceInitializer();
            di.setDataSource( dataSource );
            CompositeDatabasePopulator cdp = new CompositeDatabasePopulator();
            cdp.addPopulators(
                    new DatabaseSchemaPopulator( "h2" ),
                    new InitialDataPopulator( true ) );
            di.setDatabasePopulator( cdp );
            di.setEnabled( true );
            return di;
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

package ubic.gemma.core.util.test;

import gemma.gsec.acl.AclAuthorizationStrategyImpl;
import gemma.gsec.acl.AclSidRetrievalStrategyImpl;
import gemma.gsec.acl.ObjectIdentityRetrievalStrategyImpl;
import gemma.gsec.acl.domain.AclDao;
import gemma.gsec.acl.domain.AclDaoImpl;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.acl.domain.AclServiceImpl;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
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
import ubic.gemma.persistence.initialization.DatabaseSchemaPopulator;
import ubic.gemma.persistence.initialization.InitialDataPopulator;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimalist test setup with an in-memory database and transactional test cases.
 * <p>
 * Renovations Phase 2: SessionFactory is now unwrapped from a JPA EntityManagerFactory built from
 * META-INF/persistence.xml (Spring 6 + Hibernate 6 pattern; the legacy
 * {@code org.springframework.orm.hibernate5.LocalSessionFactoryBean} no longer works with
 * Hibernate 6 because {@code ReflectionManager.reset()} is gone).
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
        public LocalContainerEntityManagerFactoryBean entityManagerFactory( DataSource dataSource ) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource( dataSource );
            emf.setPersistenceUnitName( "gemma" );
            emf.setJpaVendorAdapter( new HibernateJpaVendorAdapter() );
            Map<String, Object> props = new HashMap<>();
            props.put( "hibernate.dialect", H2Dialect.class.getName() );
            props.put( "hibernate.cache.use_second_level_cache", "false" );
            props.put( "hibernate.cache.use_query_cache", "false" );
            props.put( "hibernate.max_fetch_depth", "3" );
            props.put( "hibernate.default_batch_fetch_size", "128" );
            props.put( "hibernate.jdbc.batch_size", "32" );
            props.put( "hibernate.jdbc.batch_versioned_data", "true" );
            props.put( "hibernate.order_inserts", "true" );
            props.put( "hibernate.order_updates", "true" );
            props.put( "hibernate.show_sql", Settings.getString( "gemma.hibernate.show_sql" ) );
            props.put( "hibernate.format_sql", Settings.getString( "gemma.hibernate.format_sql" ) );
            // Hibernate-managed schema creation; DatabaseSchemaPopulator's Hibernate branch is a no-op on Phase 2.
            props.put( "hibernate.hbm2ddl.auto", "create" );
            emf.setJpaPropertyMap( props );
            return emf;
        }

        @Bean
        public SessionFactory sessionFactory( EntityManagerFactory entityManagerFactory ) {
            return entityManagerFactory.unwrap( SessionFactory.class );
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
        public PlatformTransactionManager platformTransactionManager( EntityManagerFactory entityManagerFactory ) {
            return new JpaTransactionManager( entityManagerFactory );
        }

        @Bean
        public AclDao aclDao( SessionFactory sessionFactory, SidRetrievalStrategy sidRetrievalStrategy ) {
            AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
                    new GrantedAuthority[] { new SimpleGrantedAuthority( "ADMIN" ), new SimpleGrantedAuthority( "ADMIN" ), new SimpleGrantedAuthority( "ADMIN" ) },
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

package ubic.gemma.core.util.test;

import gemma.gsec.acl.AclAuthorizationStrategyImpl;
import gemma.gsec.acl.AclSidRetrievalStrategyImpl;
import gemma.gsec.acl.domain.AclDao;
import gemma.gsec.acl.domain.AclDaoImpl;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.acl.domain.AclServiceImpl;
import org.apache.lucene.util.Version;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.util.Settings;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Minimalist test setup with an in-memory database and transactional test cases.
 *
 * @author poirigui
 */
public abstract class BaseDatabaseTest extends AbstractTransactionalJUnit4SpringContextTests {

    protected abstract static class BaseDatabaseTestContextConfiguration {
        @Bean
        public DataSource dataSource() {
            DataSource ds = new SimpleDriverDataSource( new org.h2.Driver(), "jdbc:h2:mem:gemdtest;MODE=MYSQL;DB_CLOSE_DELAY=-1" );
            new JdbcTemplate( ds ).execute( "drop all objects" );
            return ds;
        }

        @Bean
        public FactoryBean<SessionFactory> sessionFactory( DataSource dataSource ) {
            LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
            factory.setDataSource( dataSource );
            factory.setConfigLocations(
                    new ClassPathResource( "/hibernate.cfg.xml" ) );
            Properties props = new Properties();
            props.setProperty( "hibernate.hbm2ddl.auto", "create" );
            props.setProperty( "hibernate.dialect", H2Dialect.class.getName() );
            props.setProperty( "hibernate.cache.use_second_level_cache", "false" );
            props.setProperty( "hibernate.max_fetch_depth", "3" );
            props.setProperty( "hibernate.default_batch_fetch_size", "100" );
            props.setProperty( "hibernate.jdbc.fetch_size", "128" );
            props.setProperty( "hibernate.jdbc.batch_size", "32" );
            props.setProperty( "hibernate.jdbc.batch_versioned_data", "true" );
            props.setProperty( "hibernate.order_inserts", "true" );
            props.setProperty( "hibernate.order_updates", "true" );
            props.setProperty( "hibernate.show_sql", Settings.getString( "gemma.hibernate.show_sql" ) );
            props.setProperty( "hibernate.format_sql", Settings.getString( "gemma.hibernate.format_sql" ) );
            props.setProperty( "hibernate.search.lucene_version", Version.LUCENE_36.name() );
            // use an in-memory search index for testing
            props.setProperty( "hibernate.search.default.directory_provider", "ram" );
            factory.setHibernateProperties( props );
            return factory;
        }

        @Bean
        @DependsOn("sessionFactory")
        public DataSourceInitializer dataSourceInitializer( DataSource dataSource ) {
            return new DataSourceInitializer( dataSource );
        }

        @Bean
        public PlatformTransactionManager platformTransactionManager( SessionFactory sessionFactory ) {
            return new HibernateTransactionManager( sessionFactory );
        }

        @Bean
        public AclDao aclDao( SessionFactory sessionFactory ) {
            AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
                    new GrantedAuthority[] { new SimpleGrantedAuthority( "ADMIN" ), new SimpleGrantedAuthority( "ADMIN" ), new SimpleGrantedAuthority( "ADMIN" ) },
                    new AclSidRetrievalStrategyImpl( new NullRoleHierarchy() ) );
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
    }

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected AclService aclService;

    protected static class DataSourceInitializer implements InitializingBean {

        private final JdbcTemplate template;

        @Autowired
        private ApplicationContext applicationContext;

        public DataSourceInitializer( DataSource dataSource ) {
            this.template = new JdbcTemplate( dataSource );
        }

        @Override
        public void afterPropertiesSet() {
            JdbcTestUtils.executeSqlScript( template, applicationContext.getResource( "/sql/init-acls.sql" ), false );
            JdbcTestUtils.executeSqlScript( template, applicationContext.getResource( "/sql/init-entities.sql" ), false );
            JdbcTestUtils.executeSqlScript( template, applicationContext.getResource( "/sql/init-indices.sql" ), false );
        }
    }
}

package ubic.gemma.core.util.test;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import ubic.gemma.persistence.util.Settings;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Minimalist test setup with an in-memory database and transactional test cases.
 *
 * @author poirigui
 */
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public abstract class BaseDatabaseTest extends AbstractTransactionalJUnit4SpringContextTests {

    protected abstract static class BaseDatabaseTestContextConfiguration {
        @Bean
        public DataSource dataSource() {
            return new SimpleDriverDataSource( new org.h2.Driver(), "jdbc:h2:mem:gemdtest;MODE=MYSQL;DB_CLOSE_DELAY=-1" );
        }

        @Bean
        public FactoryBean<SessionFactory> sessionFactory( DataSource dataSource ) {
            LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
            factory.setDataSource( dataSource );
            factory.setConfigLocations(
                    new ClassPathResource( "/hibernate.cfg.xml" ) );
            Properties props = new Properties();
            props.setProperty( "hibernate.hbm2ddl.auto", "create" );
            props.setProperty( "hibernate.dialect", org.hibernate.dialect.H2Dialect.class.getName() );
            props.setProperty( "hibernate.cache.use_second_level_cache", "false" );
            props.setProperty( "hibernate.show_sql", Settings.getString( "gemma.hibernate.show_sql" ) );
            props.setProperty( "hibernate.format_sql", Settings.getString( "gemma.hibernate.format_sql" ) );
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
    }

    @Autowired
    protected SessionFactory sessionFactory;

    protected static class DataSourceInitializer implements InitializingBean {

        private final DataSource dataSource;

        @Autowired
        private ApplicationContext applicationContext;

        public DataSourceInitializer( DataSource dataSource ) {
            this.dataSource = dataSource;
        }

        @Override
        public void afterPropertiesSet() {
            JdbcTemplate template = new JdbcTemplate( dataSource );
            JdbcTestUtils.executeSqlScript( template, applicationContext.getResource( "/sql/init-acls.sql" ), false );
            JdbcTestUtils.executeSqlScript( template, applicationContext.getResource( "/sql/init-entities.sql" ), false );
            JdbcTestUtils.executeSqlScript( template, applicationContext.getResource( "/sql/init-indices.sql" ), false );
        }
    }
}

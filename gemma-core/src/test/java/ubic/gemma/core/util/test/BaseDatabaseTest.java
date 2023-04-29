package ubic.gemma.core.util.test;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
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
        public PlatformTransactionManager platformTransactionManager( SessionFactory sessionFactory ) {
            return new HibernateTransactionManager( sessionFactory );
        }
    }

    @Autowired
    protected SessionFactory sessionFactory;
}

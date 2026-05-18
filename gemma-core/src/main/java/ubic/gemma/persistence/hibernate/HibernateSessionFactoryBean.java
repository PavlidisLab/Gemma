package ubic.gemma.persistence.hibernate;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import ubic.gemma.persistence.initialization.TestBootstrapState;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

/**
 * Spring {@link FactoryBean} that builds a Hibernate {@link SessionFactory} using Hibernate's native
 * {@link Configuration} bootstrap (not JPA).
 * <p>
 * Phase 2 background: Spring 6's {@code org.springframework.orm.hibernate5.LocalSessionFactoryBean} no
 * longer works with Hibernate 6 because its bootstrap path calls
 * {@code hibernate-commons-annotations}' {@code ReflectionManager.reset()}, which was removed in
 * Hibernate 6. The JPA-bootstrap alternative
 * ({@code LocalContainerEntityManagerFactoryBean} + {@code HibernateJpaVendorAdapter} +
 * {@code entityManagerFactory.unwrap(SessionFactory.class)}) compiles and bootstraps fine, but
 * Hibernate's JPA bootstrap hard-codes {@code hibernate.current_session_context_class=jpa} regardless
 * of what's in {@code jpaPropertyMap}, breaking {@code SessionFactory.getCurrentSession()} for code
 * that relies on Spring's {@code SpringSessionContext} bridge to {@code HibernateTransactionManager}.
 * <p>
 * Gemma's DAOs use {@code SessionFactory.getCurrentSession()} throughout, so we need native Hibernate
 * bootstrap. This bean does the minimum {@code Configuration}-based setup the codebase needs:
 * load {@code hibernate.cfg.xml}, apply a {@link Properties} bag (dialect, batching, cache config,
 * etc.), and produce a {@link SessionFactory}.
 *
 * @author poirigui
 */
@CommonsLog
public class HibernateSessionFactoryBean
        implements FactoryBean<SessionFactory>, InitializingBean, DisposableBean {

    private DataSource dataSource;
    private Resource configLocation;
    private Properties hibernateProperties = new Properties();
    private Class<?>[] annotatedClasses;
    private SessionFactory sessionFactory;

    public void setDataSource( DataSource dataSource ) {
        this.dataSource = dataSource;
    }

    public void setConfigLocation( Resource configLocation ) {
        this.configLocation = configLocation;
    }

    public void setHibernateProperties( Properties hibernateProperties ) {
        this.hibernateProperties = hibernateProperties;
    }

    public Properties getHibernateProperties() {
        return this.hibernateProperties;
    }

    /**
     * Register {@code @Entity}-annotated classes (typically test fixtures defined inline in a JUnit
     * class). Equivalent to {@link Configuration#addAnnotatedClass(Class)} on the underlying
     * Hibernate configuration.
     */
    public void setAnnotatedClasses( Class<?>... annotatedClasses ) {
        this.annotatedClasses = annotatedClasses;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        Configuration cfg = new Configuration();
        // Wire Spring-managed DataSource into Hibernate via DatasourceConnectionProviderImpl so that
        // Hibernate doesn't try to build its own pool from JDBC URL/user/password properties.
        cfg.getProperties().put( org.hibernate.cfg.AvailableSettings.DATASOURCE, this.dataSource );
        if ( this.configLocation != null ) {
            cfg.configure( this.configLocation.getURL() );
        }
        if ( this.annotatedClasses != null ) {
            for ( Class<?> c : this.annotatedClasses ) {
                cfg.addAnnotatedClass( c );
            }
        }
        cfg.addProperties( this.hibernateProperties );
        // Phase 2 multi-context guard: only the FIRST SessionFactory built in this JVM is
        // allowed to materialize the schema via hbm2ddl.auto=create/create-drop/update. Later
        // SessionFactories (which Spring's TestContext cache builds whenever @ContextConfiguration
        // shape differs) would otherwise drop the schema out from under the first
        // SessionFactory's still-cached entity persisters, manifesting as
        // SQLGrammarException("Table 'gemdtest.X' doesn't exist") on the original context once
        // the cache rotates. We force hbm2ddl=none on the second-and-later boot so the test DB,
        // schema, and seed data all stay intact.
        String requestedHbm2ddl = cfg.getProperty( "hibernate.hbm2ddl.auto" );
        boolean wantsCreate = requestedHbm2ddl != null
                && ( requestedHbm2ddl.startsWith( "create" ) || "update".equals( requestedHbm2ddl ) );
        if ( wantsCreate && !TestBootstrapState.claimSchemaMaterialization() ) {
            log.info( "Schema already materialized by an earlier ApplicationContext in this JVM; "
                    + "downgrading hibernate.hbm2ddl.auto from '" + requestedHbm2ddl + "' to 'none' "
                    + "for this SessionFactory to avoid wiping the live schema." );
            cfg.setProperty( "hibernate.hbm2ddl.auto", "none" );
        }
        this.sessionFactory = cfg.buildSessionFactory();
    }

    @Override
    public SessionFactory getObject() {
        return this.sessionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return ( this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class );
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() {
        if ( this.sessionFactory != null ) {
            this.sessionFactory.close();
        }
    }

    /**
     * Convenience for tests that want the canonical Gemma mapping list without committing to a
     * particular {@link DataSource} or property set: returns a {@link ClassPathResource} pointing
     * at the production {@code hibernate.cfg.xml}.
     */
    public static Resource defaultConfigLocation() {
        return new ClassPathResource( "/hibernate.cfg.xml" );
    }
}

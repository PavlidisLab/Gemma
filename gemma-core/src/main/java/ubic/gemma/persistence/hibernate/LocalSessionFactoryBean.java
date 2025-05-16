package ubic.gemma.persistence.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

/**
 * Reimplementation of {@link org.springframework.orm.hibernate4.LocalSessionFactoryBean} that supports resolving XSD
 * schemas in the classpath.
 * <p>
 * It's been slimmed down to what we actually use in Gemma.
 *
 * @author poirigui
 * @see XSDEntityResolver
 * @see org.springframework.orm.hibernate4.LocalSessionFactoryBean
 */
public class LocalSessionFactoryBean
        implements FactoryBean<SessionFactory>, ResourceLoaderAware, InitializingBean, DisposableBean {

    private DataSource dataSource;

    private Resource[] configLocations;

    private Properties hibernateProperties;

    private Class<?>[] annotatedClasses;

    private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private Configuration configuration;

    private SessionFactory sessionFactory;

    public void setDataSource( DataSource dataSource ) {
        this.dataSource = dataSource;
    }

    public void setConfigLocation( Resource configLocation ) {
        this.configLocations = new Resource[] { configLocation };
    }

    public void setHibernateProperties( Properties hibernateProperties ) {
        this.hibernateProperties = hibernateProperties;
    }

    /**
     * Return the Hibernate properties, if any. Mainly available for
     * configuration through property paths that specify individual keys.
     */
    public Properties getHibernateProperties() {
        if ( this.hibernateProperties == null ) {
            this.hibernateProperties = new Properties();
        }
        return this.hibernateProperties;
    }

    public void setAnnotatedClasses( Class<?>... annotatedClasses ) {
        this.annotatedClasses = annotatedClasses;
    }

    public void setResourceLoader( ResourceLoader resourceLoader ) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver( resourceLoader );
    }

    public void afterPropertiesSet() throws IOException {
        LocalSessionFactoryBuilder sfb = new LocalSessionFactoryBuilder( this.dataSource, this.resourcePatternResolver );

        // this is the main difference with the original implementation
        sfb.setEntityResolver( new XSDEntityResolver() );

        if ( this.configLocations != null ) {
            for ( Resource resource : this.configLocations ) {
                // Load Hibernate configuration from given location.
                sfb.configure( resource.getURL() );
            }
        }

        if ( this.hibernateProperties != null ) {
            sfb.addProperties( this.hibernateProperties );
        }

        if ( this.annotatedClasses != null ) {
            sfb.addAnnotatedClasses( this.annotatedClasses );
        }

        // Build SessionFactory instance.
        this.configuration = sfb;
        this.sessionFactory = buildSessionFactory( sfb );
    }

    protected SessionFactory buildSessionFactory( LocalSessionFactoryBuilder sfb ) {
        return sfb.buildSessionFactory();
    }

    public final Configuration getConfiguration() {
        if ( this.configuration == null ) {
            throw new IllegalStateException( "Configuration not initialized yet" );
        }
        return this.configuration;
    }


    public SessionFactory getObject() {
        return this.sessionFactory;
    }

    public Class<?> getObjectType() {
        return ( this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class );
    }

    public boolean isSingleton() {
        return true;
    }

    public void destroy() {
        this.sessionFactory.close();
    }
}

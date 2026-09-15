package ubic.gemma.rest.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * Deregister JDBC drivers loaded by the webapp ClassLoader on context shutdown.
 *
 * <p>{@link DriverManager} lives in the JVM/system ClassLoader and pins a reference to every
 * registered {@link Driver}. Drivers shipped inside the webapp (notably {@code mysql-connector-j}
 * in {@code WEB-INF/lib}) are loaded by the webapp ClassLoader; without explicit deregistration
 * on undeploy the webapp ClassLoader cannot be GC'd, causing a memory leak across Tomcat
 * redeploys.
 *
 * <p>Tomcat 10.1's {@code WebappClassLoaderBase.clearReferencesJdbc()} mitigates this
 * automatically, but emits a noisy WARN ("The web application registered the JDBC driver ...
 * but failed to unregister it ... has been forcibly unregistered") on every redeploy. Doing
 * the deregistration ourselves keeps {@code catalina.out} clean and removes the dependence on
 * container-specific cleanup behaviour.
 *
 * <p>Implements GitHub issue #534.
 */
public class JdbcDriverDeregistrationListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger( JdbcDriverDeregistrationListener.class );

    @Override
    public void contextDestroyed( ServletContextEvent sce ) {
        ClassLoader webappClassLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while ( drivers.hasMoreElements() ) {
            Driver driver = drivers.nextElement();
            // Only deregister drivers loaded by THIS webapp's ClassLoader. Drivers loaded by
            // the parent (container / system) ClassLoader are shared across webapps and must
            // not be touched.
            if ( driver.getClass().getClassLoader() == webappClassLoader ) {
                try {
                    DriverManager.deregisterDriver( driver );
                    log.info( "Deregistered JDBC driver on shutdown: {}", driver.getClass().getName() );
                } catch ( SQLException e ) {
                    log.warn( "Failed to deregister JDBC driver {}: {}", driver.getClass().getName(), e.getMessage() );
                }
            }
        }
    }
}

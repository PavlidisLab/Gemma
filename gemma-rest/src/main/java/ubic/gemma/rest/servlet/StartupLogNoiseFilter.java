package ubic.gemma.rest.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Suppresses a small whitelist of known-benign noisy log records emitted by third-party
 * libraries at startup (and during normal request handling) that aren't actionable from our
 * code. Installed on the root JUL logger's handlers so records are filtered regardless of
 * which child logger emitted them.
 *
 * <p>Currently filters:
 * <ul>
 *   <li><b>Jersey "is not resolvable to a concrete type"</b> — JAX-RS complains about
 *       parameter types like {@code TaxonArg<?>} where the wildcard reflects a real runtime
 *       choice (the static {@code valueOf(String)} returns one of several concrete
 *       subclasses depending on the input). Jersey resolves the converter via that factory
 *       method correctly; the warning is informational and there's no fix without flattening
 *       138 generic abstract args to raw types and eating the corresponding compile-time
 *       unchecked warnings. ~138 occurrences across the REST surface, each emitted once per
 *       parameter at app init.</li>
 *   <li><b>Lucene Vector API on JDK 23+</b> — {@code VectorizationProvider} logs a WARNING
 *       suggesting we upgrade Lucene to use the JDK 23+ Vector API. Lucene 9.x still works
 *       fine on JDK 25; the optimization is unrealized but nothing functional is lost.
 *       Resolved when Hibernate Search bumps to Lucene 10.x.</li>
 * </ul>
 *
 * <p><b>Not filtered (intentionally):</b>
 * <ul>
 *   <li>{@code sun.misc.Unsafe::objectFieldOffset} called by ehcache 3.10.9-jakarta — this
 *       is a JVM-internal stderr warning that doesn't go through JUL, so a Java-side filter
 *       can't suppress it. Only fix is upgrading ehcache when a non-Unsafe build ships.</li>
 *   <li>Tomcat's APR {@code System.load} restricted-method warning — handled at the JVM
 *       flag level via {@code --enable-native-access=ALL-UNNAMED} in CATALINA_OPTS.</li>
 * </ul>
 *
 * <p>Companion to {@link JerseyClientAbortLogFilter}, which targets a single logger by name
 * with a thrown-cause check. This filter takes the broader "wrap the root handlers" approach
 * because the offending Jersey introspection logger isn't a stable name across Jersey
 * versions, but the message text is.
 */
public class StartupLogNoiseFilter implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger( StartupLogNoiseFilter.class );

    private final Map<Handler, Filter> previousFilters = new HashMap<>();

    @Override
    public void contextInitialized( ServletContextEvent sce ) {
        java.util.logging.Logger root = java.util.logging.Logger.getLogger( "" );
        int wrapped = 0;
        for ( Handler h : root.getHandlers() ) {
            Filter prev = h.getFilter();
            previousFilters.put( h, prev );
            h.setFilter( new NoiseSuppressionFilter( prev ) );
            wrapped++;
        }
        log.info( "Installed startup log-noise filter on {} root handler(s)", wrapped );
    }

    @Override
    public void contextDestroyed( ServletContextEvent sce ) {
        for ( Map.Entry<Handler, Filter> e : previousFilters.entrySet() ) {
            try {
                e.getKey().setFilter( e.getValue() );
            } catch ( Exception ex ) {
                log.warn( "Failed to restore prior filter on handler {}: {}",
                        e.getKey().getClass().getName(), ex.getMessage() );
            }
        }
        previousFilters.clear();
    }

    /** Drops records matching any of the known-benign patterns; everything else passes through. */
    private static final class NoiseSuppressionFilter implements Filter {
        private final Filter delegate;

        NoiseSuppressionFilter( Filter delegate ) {
            this.delegate = delegate;
        }

        @Override
        public boolean isLoggable( LogRecord record ) {
            String msg = record.getMessage();
            if ( msg != null ) {
                // Jersey: "Parameter N of type X from method Y is not resolvable to a concrete type."
                if ( msg.contains( "is not resolvable to a concrete type" ) ) {
                    return false;
                }
                // Lucene: "You are running with Java 23 or later. To make full use of the Vector API ..."
                if ( msg.contains( "To make full use of the Vector API" ) ) {
                    return false;
                }
            }
            return delegate == null || delegate.isLoggable( record );
        }
    }
}

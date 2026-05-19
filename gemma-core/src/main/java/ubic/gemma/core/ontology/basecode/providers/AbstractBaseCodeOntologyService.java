/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.providers;

import ubic.gemma.core.ontology.basecode.jena.UrlOntologyService;
import ubic.basecode.util.Configuration;

import static java.util.Objects.requireNonNull;

/**
 * Base class for all ontologies built-in to the baseCode project.
 * <p>
 * The ontologies that subclass this will honor settings in the {@code basecode.properties} file for loading and
 * locating the ontology.
 *
 * @author poirigui
 */
public abstract class AbstractBaseCodeOntologyService extends AbstractDelegatingOntologyService {

    /**
     * Intentionally package-private constructor.
     */
    protected AbstractBaseCodeOntologyService( String name, String cacheName ) {
        this( name, requireNonNull( Configuration.getString( "url." + cacheName ) ),
            Boolean.TRUE.equals( Configuration.getBoolean( "load." + cacheName ) ), cacheName );
    }

    public AbstractBaseCodeOntologyService( String name, String url, boolean isEnabled, String cacheName ) {
        super( new UrlOntologyService( name, url, isEnabled, cacheName ) );
    }
}

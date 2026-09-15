package ubic.gemma.core.ontology.providers;

import ubic.gemma.core.ontology.jena.UrlOntologyService;
import ubic.gemma.core.config.Configuration;

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

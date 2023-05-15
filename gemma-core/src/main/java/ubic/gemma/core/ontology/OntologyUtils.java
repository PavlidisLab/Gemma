package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;

import java.util.List;

/**
 * Utilities for working with ontologies.
 * @author poirigui
 */
@CommonsLog
public class OntologyUtils {

    /**
     * Ensure that a given ontology is initialized, force-loading it via {@link OntologyService#initialize(boolean, boolean)}
     * if necessary.
     * <p>
     * If the ontology was started via {@link OntologyService#startInitializationThread(boolean, boolean)}, this method
     * will patiently wait until it completes.
     * @throws InterruptedException in case the ontology initialization thread is started, we will wait which implies a
     * possible interrupt
     */
    public static void ensureInitialized( OntologyService service ) throws InterruptedException {
        if ( service.isOntologyLoaded() )
            return;
        if ( service.isInitializationThreadAlive() ) {
            log.info( String.format( "Waiting for %s to load...", service ) );
            service.waitForInitializationThread();
        } else {
            service.initialize( true, false );
        }
    }

    /**
     * Extract a label for a term URI as per {@link OntologyTerm#getLabel()}.
     */
    public static String getLabelFromTermUri( String termUri ) {
        UriComponents components = UriComponentsBuilder.fromUriString( termUri ).build();
        List<String> segments = components.getPathSegments();
        // use the fragment
        if ( !StringUtils.isEmpty( components.getFragment() ) ) {
            return partToTerm( components.getFragment() );
        }
        // pick the last non-empty segment
        for ( int i = segments.size() - 1; i >= 0; i-- ) {
            if ( !StringUtils.isEmpty( segments.get( i ) ) ) {
                return partToTerm( segments.get( i ) );
            }
        }
        // as a last resort, return the parsed URI (this will remove excessive trailing slashes)
        return components.toUriString();
    }

    private static String partToTerm( String part ) {
        return part.replaceFirst( "_", ":" ).toUpperCase();
    }
}

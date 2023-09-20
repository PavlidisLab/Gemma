package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import ubic.basecode.ontology.providers.OntologyService;

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
            log.info( String.format( "Force-loading %s...", service ) );
            service.initialize( true, false );
        }
    }
}

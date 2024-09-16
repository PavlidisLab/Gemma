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

    /**
     * Ensure that a given ontology is initialized, force-loading it via {@link OntologyService#initialize(boolean, boolean)}, but setting the language level to LITE,
     * Inferencing to NONE, processImports to false, and enable search to false, if the ontology isn't already loaded.
     */
    public static void ensureInitializedLite( OntologyService service ) throws InterruptedException {
        if ( service.isOntologyLoaded() )
            return;
        if ( service.isInitializationThreadAlive() ) {
            log.info( String.format( "Waiting for %s to load...", service ) );
            service.waitForInitializationThread();
        } else {
            ensureInitialized( service, OntologyService.InferenceMode.NONE, OntologyService.LanguageLevel.LITE, false, false );
        }
    }

    /**
     * Ensure that a given ontology is initialized, force-loading it via {@link OntologyService#initialize(boolean, boolean)},
     * but first setting how we load it. However, those parameters are ignored if the ontology is already loaded or in progress.
     * @throws InterruptedException  in case the ontology initialization thread is started, we will wait which implies a
     *      possible interrupt
     */
    public static void ensureInitialized( OntologyService service, OntologyService.InferenceMode mode, OntologyService.LanguageLevel level, Boolean searchEnabled, Boolean processImports ) throws InterruptedException {

        if ( service.isOntologyLoaded() )
            return;
        if ( service.isInitializationThreadAlive() ) {
            log.info( String.format( "Waiting for %s to load...", service ) );
            service.waitForInitializationThread();
        } else {
            service.setInferenceMode( mode );
            service.setSearchEnabled( searchEnabled );
            service.setLanguageLevel( level );
            service.setProcessImports( processImports );
            log.info( String.format( "Force-loading %s ", service ) );

            service.initialize( true, false );
        }
    }
}

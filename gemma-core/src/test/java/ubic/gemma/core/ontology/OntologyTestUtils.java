package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import ubic.basecode.ontology.providers.OntologyService;

import java.io.InputStream;

/**
 * Utilities for testing ontologies.
 */
@CommonsLog
public class OntologyTestUtils {

    /**
     * Initialize an ontology, cancelling any pending initialization.
     */
    public static void initialize( OntologyService os, InputStream stream ) throws InterruptedException {
        if ( os.isInitializationThreadAlive() ) {
            log.warn( String.format( "Cancelling pending initialization of %s...", os ) );
            os.cancelInitializationThread();
            os.waitForInitializationThread();
        }
        os.initialize( stream, false );
    }
}

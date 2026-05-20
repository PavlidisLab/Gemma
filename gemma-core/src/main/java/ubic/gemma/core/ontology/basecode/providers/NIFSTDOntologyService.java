/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.providers;

import ubic.gemma.core.ontology.basecode.jena.ClasspathOntologyService;
import ubic.gemma.core.config.Configuration;

/**
 * @author paul
 */
@Deprecated
public class NIFSTDOntologyService extends AbstractDelegatingOntologyService {

    private static final String NIFSTD_ONTOLOGY_FILE = "/data/loader/ontology/nif-gemma.owl.gz";

    public NIFSTDOntologyService() {
        super( new ClasspathOntologyService( "NIFSTD", NIFSTD_ONTOLOGY_FILE,
            Boolean.TRUE.equals( Configuration.getBoolean( "load.nifstdOntology" ) ), "nifstdOntology" ) );
        setProcessImports( false );
    }
}

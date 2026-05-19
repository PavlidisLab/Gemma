/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.search;

import ubic.gemma.core.ontology.basecode.providers.OntologyService;

/**
 * Base class for exceptions raised by {@link OntologyService#findTerm(String)} and others.
 * @author poirigui
 */
public class OntologySearchException extends Exception {
    private final String query;

    public OntologySearchException( String message, String query, Throwable cause ) {
        super( message, cause );
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}

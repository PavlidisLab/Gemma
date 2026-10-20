package ubic.gemma.core.ontology.search;

import ubic.gemma.core.ontology.providers.OntologyService;

/**
 * Base class for exceptions raised by {@link OntologyService#findTerm(String, int)} and others.
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

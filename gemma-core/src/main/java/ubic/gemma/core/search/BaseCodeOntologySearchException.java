package ubic.gemma.core.search;

import ubic.basecode.ontology.search.OntologySearchException;

/**
 * Exception that wraps a baseCode {@link ubic.basecode.ontology.search.OntologySearchException}.
 */
public class BaseCodeOntologySearchException extends SearchException {

    private final ubic.basecode.ontology.search.OntologySearchException cause;

    public BaseCodeOntologySearchException( OntologySearchException cause ) {
        super( cause.getMessage(), cause );
        this.cause = cause;
    }

    @Override
    public ubic.basecode.ontology.search.OntologySearchException getCause() {
        return cause;
    }
}

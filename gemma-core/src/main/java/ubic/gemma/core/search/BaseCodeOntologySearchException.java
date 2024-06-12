package ubic.gemma.core.search;

import ubic.basecode.ontology.search.OntologySearchException;

/**
 * Exception that wraps a baseCode {@link OntologySearchException}.
 */
public class BaseCodeOntologySearchException extends SearchException {

    private final OntologySearchException cause;

    public BaseCodeOntologySearchException( OntologySearchException cause ) {
        super( cause.getMessage(), cause );
        this.cause = cause;
    }

    @Override
    public OntologySearchException getCause() {
        return cause;
    }
}

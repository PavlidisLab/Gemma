package ubic.gemma.core.search;

import ubic.basecode.ontology.search.OntologySearchException;

/**
 * Exception that wraps a baseCode {@link OntologySearchException}.
 *
 * <p>Restored from the pre-strip Gemma search code (ed93c2f023^^) during the
 * Phase 3 search restoration; the original was deleted in the
 * "stub/delete search subsystem cascade" commit.
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

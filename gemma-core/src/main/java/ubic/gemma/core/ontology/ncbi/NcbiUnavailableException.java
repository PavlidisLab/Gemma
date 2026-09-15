package ubic.gemma.core.ontology.ncbi;

/**
 * Thrown when NCBI cannot be reached or answers in a shape we cannot read — unverified, which is not the
 * same as "no such gene". Mirrors {@code OlsUnavailableException}: the caller decides whether an
 * unverifiable id blocks a write.
 *
 * @author gemma
 */
public class NcbiUnavailableException extends Exception {

    public NcbiUnavailableException( String message ) {
        super( message );
    }

    public NcbiUnavailableException( String message, Throwable cause ) {
        super( message, cause );
    }
}

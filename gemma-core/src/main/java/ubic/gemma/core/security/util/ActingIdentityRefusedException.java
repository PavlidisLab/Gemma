package ubic.gemma.core.security.util;

/**
 * An {@code onBehalfOf} claim the server refuses to record: an agent naming its own account, or an agent recording a
 * ruling without naming the person who directed it. gemma-rest answers it with 400.
 */
public class ActingIdentityRefusedException extends IllegalArgumentException {

    public ActingIdentityRefusedException( String message ) {
        super( message );
    }
}

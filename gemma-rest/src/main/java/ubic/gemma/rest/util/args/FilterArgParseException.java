package ubic.gemma.rest.util.args;

import java.util.Optional;

/**
 * Represents a parsing exception to a {@link FilterArg}.
 *
 * @author poirigui
 */
public class FilterArgParseException extends Exception {

    private final Integer part;

    public FilterArgParseException( String message, int part ) {
        super( message );
        this.part = part;
    }

    @SuppressWarnings("unused")
    public FilterArgParseException( String message ) {
        super( message );
        this.part = null;
    }

    /**
     * Obtain the index of the part at fault here.
     *
     * @return the index of the part at fault, or {@link Optional#empty()} if not available
     */
    public Optional<Integer> getPart() {
        return Optional.ofNullable( part );
    }
}

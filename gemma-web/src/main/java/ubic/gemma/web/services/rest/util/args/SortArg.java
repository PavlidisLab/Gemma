package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

/**
 * Class representing an API argument that should be an integer.
 *
 * @author tesarst
 */
public class SortArg extends MalformableArg {
    private static final String ERROR_MSG =
            "Value '%s' can not be interpreted as a sort argument. Correct syntax is: [+,-][field]. E.g: '-id' means 'order by ID descending. "
                    + "Make sure you URL encode the arguments, for example '+' has to be encoded to '%%2B'.";

    private String field;
    private boolean asc;

    private SortArg( String field, boolean asc ) {
        this.field = field;
        this.asc = asc;
    }

    /**
     * Constructor used to create an instance that instead of returning the sort values, informs that the received
     * string was not well-formed.
     *
     * @param errorMessage the malformed original string argument.
     */
    private SortArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request taxon argument
     * @return a new SortArg object representing the sort options in the given string, or a malformed SortArg that will
     * throw a {@link ubic.gemma.web.services.rest.util.GemmaApiException}, if the given string was not well-formed.
     */
    @SuppressWarnings("unused")
    public static SortArg valueOf( final String s ) {
        try {
            //noinspection ConstantConditions // Handled by the try catch
            return new SortArg( s.substring( 1 ), parseBoolean( s.charAt( 0 ) ) );
        } catch ( NullPointerException | StringIndexOutOfBoundsException e ) {
            return new SortArg( String.format( ERROR_MSG, s ), e );
        }
    }

    /**
     * Decides whether the given char represents a true or false.
     *
     * @param c '+' or '-' character.
     * @return true if character was '+', false if it was '-'. Null in any other case.
     */
    private static Boolean parseBoolean( char c ) {
        if ( c == '+' ) {
            return true;
        }
        if ( c == '-' ) {
            return false;
        }
        return null;
    }

    /**
     * @return the field to sort by. If the original argument was not well-composed, will produce a {@link GemmaApiException} instead.
     */
    public String getField() {
        this.checkMalformed();
        return field;
    }

    /**
     * @return the direction of sort. If the original argument was not well-composed, will produce a {@link GemmaApiException} instead.
     */
    public boolean isAsc() {
        this.checkMalformed();
        return asc;
    }

}

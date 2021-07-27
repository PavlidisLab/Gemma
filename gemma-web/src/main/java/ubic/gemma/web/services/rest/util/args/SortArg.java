package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.BadRequestException;

/**
 * Class representing an API argument that should be an integer.
 *
 * @author tesarst
 */
@Schema(implementation = String.class, type = "string", example = "+id")
public class SortArg extends AbstractArg<SortArg.FieldWithDirection> {
    private static final String ERROR_MSG =
            "Value '%s' can not be interpreted as a sort argument. Correct syntax is: [+,-][field]. E.g: '-id' means 'order by ID descending. "
                    + "Make sure you URL encode the arguments, for example '+' has to be encoded to '%%2B'.";

    private SortArg( String field, boolean asc ) {
        super( new FieldWithDirection( field, asc ) );
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
     * throw a {@link javax.ws.rs.BadRequestException}, if the given string was not well-formed.
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
     * @return the field to sort by.
     * @throws BadRequestException if the original argument was not well-composed
     */
    public String getField() throws BadRequestException {
        return getValue().field;
    }

    /**
     * @return the direction of sort.
     * @throws BadRequestException if the original argument was not well-composed
     */
    public boolean isAsc() throws BadRequestException {
        return getValue().isAsc;
    }

    static class FieldWithDirection {
        public String field;
        public boolean isAsc;

        public FieldWithDirection( String field, boolean asc ) {
            this.field = field;
            this.isAsc = asc;
        }
    }

}

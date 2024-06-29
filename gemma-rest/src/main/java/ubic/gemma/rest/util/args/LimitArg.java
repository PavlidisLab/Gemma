package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Argument used to represent a limit.
 */
@Schema(type = "integer", minimum = "1", maximum = "100", description = "Limit the number of results retrieved.")
public class LimitArg extends AbstractArg<Integer> {

    /**
     * This is the default maximum used for {@link #getValue()}.
     */
    public static int MAXIMUM = 100;

    private LimitArg( int value ) {
        super( value );
    }

    /**
     * Obtain the value of the limit ensuring that it is smaller than {@link #MAXIMUM}
     */
    @Override
    public Integer getValue() {
        return this.getValue( MAXIMUM );
    }

    /**
     * Obtain the value of the limit and ensure it is smaller than a given maximum.
     *
     * Use {@link #getValue()} to accept any limit.
     *
     * @param maximum a maximum the limit must not exceeed, otherwise a {@link MalformedArgException} will be raised
     * @throws MalformedArgException of the limit is exceeded, or the argument was malformed in the first place.
     */
    public Integer getValue( Integer maximum ) throws MalformedArgException {
        Integer value = super.getValue();
        if ( value > maximum ) {
            throw new MalformedArgException( "The provided limit cannot exceed " + maximum + ".", null );
        }
        return value;
    }

    /**
     * Obtain the value of the limit, explicitly disregarding the maximum defined by {@link #MAXIMUM}.
     *
     */
    public Integer getValueNoMaximum() {
        return super.getValue();
    }

    public static LimitArg valueOf( String s ) throws MalformedArgException {
        int limit;
        try {
            limit = Integer.parseInt( s );
        } catch ( NumberFormatException e ) {
            throw new MalformedArgException( "The provided limit is not a valid number.", e );
        }
        if ( limit < 1 ) {
            throw new MalformedArgException( "The provided limit must be greater than zero.", null );
        }
        return new LimitArg( limit );
    }
}

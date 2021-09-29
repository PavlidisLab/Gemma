package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import javax.ws.rs.BadRequestException;

/**
 * Argument used to represent a limit.
 */
@Schema(implementation = Integer.class, minimum = "1", maximum = "100")
public class LimitArg extends AbstractArg<Integer> {

    /**
     * This is the default maximum used for {@link #getValue()}.
     */
    public static int MAXIMUM = 100;

    private LimitArg( @NonNull Integer value ) {
        super( value );
    }

    public LimitArg( String errorMessage, Throwable cause ) {
        super( errorMessage, cause );
    }

    /**
     * Obtain the value of the limit ensuring that it is smaller than {@link #MAXIMUM}
     * @return
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
     * @return
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
     * @return
     */
    public Integer getValueNoMaximum() {
        return super.getValue();
    }

    public static LimitArg valueOf( String s ) {
        int limit;
        try {
            limit = Integer.parseInt( s );
        } catch ( NumberFormatException e ) {
            return new LimitArg( "The provided limit is not a valid number.", e );
        }
        if ( limit < 1 ) {
            return new LimitArg( "The provided limit must be greater than one.", null );
        }
        return new LimitArg( limit );
    }
}
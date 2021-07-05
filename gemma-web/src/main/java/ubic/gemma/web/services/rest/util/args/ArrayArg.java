package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.List;

/**
 * Class representing an API argument that should be an array.
 *
 * @author tesarst
 */
public abstract class ArrayArg<T> extends AbstractArg<List<T>> {
    static final String ERROR_MSG = "Value '%s' can not converted to an array of ";

    ArrayArg( List<T> values ) {
        super( values );
    }

    protected ArrayArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Splits string by the ',' comma character and trims the resulting strings.
     *
     * @param  arg the string to process
     * @return trimmed strings exploded from the input.
     */
    protected static String[] splitString( String arg ) {
        String[] array = arg.split( "," );
        for ( int i = 0; i < array.length; i++ )
            array[i] = array[i].trim();
        return array;
    }
}

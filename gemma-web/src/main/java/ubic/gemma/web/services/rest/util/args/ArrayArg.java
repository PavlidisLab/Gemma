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
}

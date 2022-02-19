package ubic.gemma.web.services.rest.util.args;

import java.util.List;

/**
 * Class representing an API argument that should be an array.
 *
 * @author tesarst
 */
public abstract class AbstractArrayArg<T> extends AbstractArg<List<T>> {

    protected static final String ERROR_MSG = "Value '%s' can not converted to an array of ";

    protected AbstractArrayArg( List<T> values ) {
        super( values );
    }
}

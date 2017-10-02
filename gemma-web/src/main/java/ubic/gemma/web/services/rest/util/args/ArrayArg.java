package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.List;

/**
 * Class representing an API argument that should be an array.
 *
 * @author tesarst
 */
public abstract class ArrayArg<T> extends MalformableArg {
    static final String ERROR_MSG = "Value '%s' can not converted to an array of ";

    private List<T> values;

    ArrayArg( List<T> values ) {
        this.values = values;
    }

    ArrayArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * @return the list of values parsed from the original String argument. If the original argument could not be
     * parsed into a list, will produce a {@link GemmaApiException} instead.
     */
    public List<T> getValue() {
        this.checkMalformed();
        return values;
    }

}

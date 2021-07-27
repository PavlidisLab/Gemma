package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.MalformedArgException;

public interface Arg<T> {

    /**
     * Verify if the argument is malformed.
     * @return
     */
    boolean isMalformed();

    /**
     * Obtain the value, or exception represented by this argument.
     *
     * Checks whether the instance of this object was created as a malformed argument, and if true, throws an
     * exception using the information provided in the constructor.
     *
     * Even though the internal value can be null, it is only the case when it is malformed and this method will produce
     * a {@link MalformedArgException}, thus guaranteeing non-nullity.
     */
    T getValue() throws MalformedArgException;
}

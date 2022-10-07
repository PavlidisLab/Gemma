package ubic.gemma.web.services.rest.util.args;

public interface Arg<T> {

    /**
     * Obtain the value represented by this argument.
     */
    T getValue();
}

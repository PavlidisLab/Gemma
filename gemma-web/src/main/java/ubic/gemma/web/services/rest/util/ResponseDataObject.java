package ubic.gemma.web.services.rest.util;

/**
 * Wrapper for a non-error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?showone=error#error">Google JSON style-guide</a>
 *
 * @author tesarst
 */
public class ResponseDataObject<T> {

    private final T data;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    ResponseDataObject( T payload ) {
        this.data = payload;
    }

    public T getData() {
        return data;
    }
}

package ubic.gemma.web.services.rest.util;

/**
 * Created by tesarst on 17/05/17.
 * Wrapper for a non-error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?showone=error#error">Google JSON style-guide</a>
 */
public class ResponseDataObject {

    @SuppressWarnings({ "WeakerAccess", "unused" }) // Used by RS serializer
    public final Object data;

    /**
     * @param payload the data to be serialised and returned as the response payload.
     */
    ResponseDataObject( Object payload ) {
        this.data = payload;
    }
}

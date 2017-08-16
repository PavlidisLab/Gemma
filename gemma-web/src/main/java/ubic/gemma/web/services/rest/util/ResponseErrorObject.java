package ubic.gemma.web.services.rest.util;

/**
 * Wrapper for an error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?showone=error#error">Google JSON style-guide</a>
 *
 * @author tesarst
 */
@SuppressWarnings("unused")
// Some properties might show as unused, but they are still serialised to JSON and published through API for client consumption.
public class ResponseErrorObject {
    /**
     * Adds the apiVersion property to the final JSON object
     */
    public final String apiVersion = WebService.API_VERSION;

    private WellComposedErrorBody error;

    /**
     * @param payload payload containing the error details.
     */
    ResponseErrorObject( WellComposedErrorBody payload ) {
        this.error = payload;
    }

    /**
     * @return the payload with error details.
     */
    public WellComposedErrorBody getError() {
        return error;
    }
}

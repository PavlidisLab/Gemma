package ubic.gemma.web.services.rest.util;

/**
 * Created by tesarst on 17/05/17.
 * Wrapper for an error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?showone=error#error">Google JSON styleguide</a>
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

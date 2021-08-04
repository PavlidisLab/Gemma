package ubic.gemma.web.services.rest.util;

import ubic.gemma.web.services.rest.RootWebService;

/**
 * Wrapper for an error response payload compliant with the
 * <a href="https://google.github.io/styleguide/jsoncstyleguide.xml?showone=error#error">Google JSON style-guide</a>
 *
 * @author tesarst
 */
@SuppressWarnings("unused")
// Some properties might show as unused, but they are still serialised to JSON and published through API for client consumption.
public class ResponseErrorObject {

    private final String apiVersion = RootWebService.API_VERSION; //OpenApiUtils.getOpenApi().getInfo().getVersion();

    private final WellComposedErrorBody error;

    /**
     * @param payload payload containing the error details.
     */
    public ResponseErrorObject( WellComposedErrorBody payload ) {
        this.error = payload;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * @return the payload with error details.
     */
    public WellComposedErrorBody getError() {
        return error;
    }
}

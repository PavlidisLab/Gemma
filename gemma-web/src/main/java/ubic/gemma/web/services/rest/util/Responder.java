package ubic.gemma.web.services.rest.util;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.lang.reflect.Array;
import java.util.Collection;

/**
 * Handles setting of the response status code and composing a proper payload structure.
 *
 * @author tesarst
 */
public class Responder {

    private static final String DEFAULT_ERR_MSG_NULL_OBJECT = "Requested resource was not found in our database.";

    /**
     * @param code            the http status code that you want the API response to show. This code must be a valid code that the
     *                        HttpServletResponse can handle. See {@link HttpServletResponse} for list of accepted values.
     * @param toReturn        an object to be wrapped in a ResponseObject to be published to the API.
     * @param servletResponse the object to set the appropriate response code on.
     * @return an instance of {@link ResponseDataObject} with the given toReturn object as its payload.
     * @throws GemmaApiException If the passed code was denoting a problem (codes 4xx/5xx), If the given toReturn object
     *                           is an instance of {@link WellComposedErrorBody}, it will be passed to the exception to act as a payload.
     *                           ToReturn arguments of different type will be discarded.
     */
    public static ResponseDataObject code( Response.Status code, Object toReturn,
            HttpServletResponse servletResponse ) {

        // Handle error codes
        if ( Responder.isCodeAnError( code ) ) {
            if ( toReturn instanceof WellComposedErrorBody ) {
                throw new GemmaApiException( ( WellComposedErrorBody ) toReturn );
            } else {
                throw new GemmaApiException( code );
            }
        }

        // non-error codes
        Responder.sendHeaders( code, servletResponse );
        return new ResponseDataObject( toReturn );
    }

    /**
     * Creates a new 200 response object. Use this method when everything is shiny, and you are returning the object
     * that the client expects. This includes empty collections or arrays
     *
     * @param servletResponse the object to set the appropriate response code on
     * @return always null, as the response payload for code 204 has to be empty.
     */
    public static ResponseDataObject code200( Object toReturn, HttpServletResponse servletResponse ) {
        return Responder.code( Response.Status.OK, toReturn, servletResponse );
    }

    /**
     * <p>
     * Creates a new 204 response object. Use this method when client submitted a request that was successfully processed,
     * and no data needs to be returned (e.g. DELETE requests). This response status requires that no payload is returned,
     * as it otherwise defaults to 200 in the javax.ws.rs layer.
     * </p>
     * <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP RFC</a>:
     * The 204 (No Content) status code indicates that the server has successfully fulfilled the request and that there
     * is no additional content to send in the response payload body.
     *
     * @param servletResponse the object to set the appropriate response code on
     * @return always null, as the response payload for code 204 has to be empty.
     */
    @SuppressWarnings({ "SameReturnValue", "WeakerAccess", "unused" })
    // Same return value: Intentional behavior - has to be consistent with other methods.
    // Weaker access, unused - we currently only have GET methods, none of which uses 204. Keeping the method for future use.
    public static ResponseDataObject code204( HttpServletResponse servletResponse ) {
        Responder.sendHeaders( Response.Status.NO_CONTENT, servletResponse );
        return null;
    }

    /**
     * <p>
     * Creates a new 400 response object. Use this method when there is an apparent error in the client request.
     * </p>
     * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP RFC</a>:
     * <pre>The 400 (Bad Request) status code indicates that the server cannot or will not process the request due to
     * something that is perceived to be a client error (e.g., malformed request syntax, invalid request message
     * framing, or deceptive request routing)</pre>
     *
     * @param message         A String that will be used in the ResponseErrorObject as a message describing the problem.
     * @param servletResponse the object to set the appropriate response code on
     * @return response data object
     */
    public static ResponseDataObject code400( String message, HttpServletResponse servletResponse ) {
        Response.Status code = Response.Status.BAD_REQUEST;
        return Responder.code( code, new WellComposedErrorBody( code, message ), servletResponse );
    }

    /**
     * <p>
     * Creates a new 404 response object. Use this method when you are certain that the resource the client requested
     * does not exist. To denote an empty collection or an array, it is preferred to use this#code204(HttpServletResponse).
     * </p>
     * <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP RFC</a>:
     * <pre>The 404 (Not Found) status code indicates that the origin server did not find a current representation for
     * the target resource or is not willing to disclose that one exists.</pre>
     *
     * @param message         A String that will be used in the ResponseErrorObject as a message describing the problem.
     *                        For automatic null object detection, use this#autoCode(Object, HttpServletResponse).
     * @param servletResponse the object to set the appropriate response code on
     * @return response data object
     */
    public static ResponseDataObject code404( String message, HttpServletResponse servletResponse ) {
        Response.Status code = Response.Status.NOT_FOUND;
        return Responder.code( code, new WellComposedErrorBody( code, message ), servletResponse );
    }

    /**
     * Creates a 503 response object. Use this when you want to signal that this particular service is temporarily unavailable.
     *
     * @param message         A String that will be used in the ResponseErrorObject as a message describing the problem.
     * @param servletResponse the object to set the appropriate response code on
     * @return response data object
     */
    @SuppressWarnings("unused") // Keeping the method in case we need it handy on short notice.
    public static ResponseDataObject code503( String message, HttpServletResponse servletResponse ) {
        Response.Status code = Response.Status.SERVICE_UNAVAILABLE;
        return Responder.code( code, new WellComposedErrorBody( code, message ), servletResponse );
    }

    /**
     * <p>
     * Tries to guess the code of the response based on the given arguments, and creates a ResponseErrorObject if necessary.
     * Code defaults to '200 - OK', even for empty collections and arrays.
     * If toReturn is null: '404 - Not found'. Use this#code204(HttpServletResponse) if you wish to set '204 - No content' instead.
     * If toReturn is an instance of WellComposedErrorBody: reads the code from the object
     * </p>
     *
     * @param toReturn        an object to be wrapped in a ResponseObject to be published to the API
     * @param servletResponse the object to set the appropriate response code on
     * @return response data object
     */
    public static ResponseDataObject autoCode( Object toReturn, HttpServletResponse servletResponse ) {
        if ( toReturn == null ) { // object is null.
            return Responder.code404( Responder.DEFAULT_ERR_MSG_NULL_OBJECT, servletResponse );

        } else if (  // empty collection/array.
                ( toReturn instanceof Collection<?> && ( ( Collection<?> ) toReturn ).isEmpty() ) //
                        || ( toReturn.getClass().isArray() && Array.getLength( toReturn ) == 0 )  //
                ) {
            // Keeping this as a special case, in case we decide to handle it differently.
            return Responder.code200( toReturn, servletResponse );
        } else if ( toReturn instanceof WellComposedErrorBody ) { // pre-prepared error body.
            return Responder.code( ( ( WellComposedErrorBody ) toReturn ).getStatus(), toReturn, servletResponse );
        }

        return Responder.code200( toReturn, servletResponse );
    }

    /**
     * Checks whether the given code is an error.
     *
     * @param code the code to check.
     * @return true, if the given code is from the client or server error family.
     */
    private static boolean isCodeAnError( Response.Status code ) {
        return code.getFamily() == Response.Status.Family.CLIENT_ERROR
                || code.getFamily() == Response.Status.Family.SERVER_ERROR;
    }

    /**
     * Sets a response code and flushes the servlet response buffer.
     *
     * @param code            response code
     * @param servletResponse servlet response
     */
    private static void sendHeaders( Response.Status code, HttpServletResponse servletResponse ) {
        servletResponse.setStatus( code.getStatusCode() );
        try {
            servletResponse.flushBuffer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}

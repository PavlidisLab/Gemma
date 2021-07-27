package ubic.gemma.web.services.rest.util;

import javax.ws.rs.NotFoundException;

/**
 * Handles setting of the response status code and composing a proper payload structure.
 *
 * @author tesarst
 */
public class Responder {

    private static final String DEFAULT_ERR_MSG_NULL_OBJECT = "Requested resource was not found in our database.";

    /**
     * Produce a {@link ResponseDataObject} that wraps the given argument.
     *
     * @param toReturn           an object to be wrapped and published to the API
     * @throws NotFoundException if the argument is null, a suitable {@link ResponseErrorObject} will be subsequently
     *                           produced by {@link ubic.gemma.web.services.rest.providers.NotFoundExceptionMapper}
     * @return a {@link ResponseDataObject} containing the argument
     */
    public static <T> ResponseDataObject<T> respond( T toReturn ) throws NotFoundException {
        if ( toReturn == null ) { // object is null.
            throw new NotFoundException( Responder.DEFAULT_ERR_MSG_NULL_OBJECT );
        } else {
            return new ResponseDataObject<>( toReturn );
        }
    }

}

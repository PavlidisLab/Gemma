package ubic.gemma.web.services.rest.util;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

public class ExceptionMapperUtils {

    public static boolean acceptsJson( HttpHeaders headers ) {
        return headers.getAcceptableMediaTypes().stream().anyMatch( mediaType -> mediaType.isCompatible( MediaType.APPLICATION_JSON_TYPE ) );
    }
}

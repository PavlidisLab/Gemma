package ubic.gemma.web.services.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.ServletUtils;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static ubic.gemma.web.services.rest.util.ExceptionMapperUtils.acceptsJson;

@CommonsLog
public abstract class AbstractExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    @Context
    private HttpServletRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private ServletConfig servletConfig;

    /**
     * Translate the exception to an HTTP {@link Response.Status}.
     */
    protected abstract Response.Status getStatus( E exception );

    /**
     * Obtain a {@link WellComposedErrorBody} for the exception.
     */
    protected WellComposedErrorBody getWellComposedErrorBody( E exception ) {
        // for security reasons, we don't include the error object in the response entity
        return new WellComposedErrorBody( getStatus( exception ), exception.getMessage() );
    }

    /**
     * Indicate if the exception should be logged.
     */
    protected boolean logException() {
        return false;
    }


    @Override
    public final Response toResponse( E exception ) {
        if ( logException() ) {
            log.error( "Unhandled exception was raised for " + ServletUtils.summarizeRequest( request ) + ".", exception );
        }
        Response.Status code = getStatus( exception );
        MediaType mediaType;
        Object entity;
        if ( acceptsJson( headers ) ) {
            mediaType = MediaType.APPLICATION_JSON_TYPE;
            entity = new ResponseErrorObject( getWellComposedErrorBody( exception ), OpenApiUtils.getOpenApi( servletConfig ) );
        } else {
            mediaType = MediaType.TEXT_PLAIN_TYPE;
            WellComposedErrorBody body = getWellComposedErrorBody( exception );
            StringBuilder builder = new StringBuilder();
            builder.append( "Message: " ).append( body.getMessage() );
            if ( body.getErrors() != null ) {
                body.getErrors().forEach( ( k, v ) -> {
                    builder.append( '\n' ).append( k ).append( ": " ).append( v );
                } );
            }
            entity = builder.toString();
        }
        return Response.status( code ).type( mediaType ).entity( entity ).build();
    }
}

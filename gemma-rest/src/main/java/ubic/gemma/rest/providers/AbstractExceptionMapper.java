package ubic.gemma.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.rest.util.OpenApiUtils;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.ServletUtils;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static ubic.gemma.rest.util.ExceptionMapperUtils.acceptsJson;

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
     * Indicate if the given exception should be logged.
     */
    protected boolean logException( E exception ) {
        return false;
    }

    protected Response.ResponseBuilder getResponseBuilder( E exception ) {
        // FIXME: request is null in tests
        return Response.status( request != null && isXmlHttpRequest( request ) ? Response.Status.OK : getStatus( exception ) );
    }

    @Override
    public final Response toResponse( E exception ) {
        if ( logException( exception ) ) {
            log.error( String.format( "Unhandled exception was raised%s.",
                    request != null ? " for " + ServletUtils.summarizeRequest( request ).replaceAll( "[\r\n]", "" ) : "" ), exception );
        }
        Response.ResponseBuilder responseBuilder = getResponseBuilder( exception );
        if ( acceptsJson( headers ) ) {
            return responseBuilder
                    .type( MediaType.APPLICATION_JSON_TYPE )
                    .entity( new ResponseErrorObject( getWellComposedErrorBody( exception ), OpenApiUtils.getOpenApi( servletConfig ) ) )
                    .build();
        } else {
            WellComposedErrorBody body = getWellComposedErrorBody( exception );
            StringBuilder builder = new StringBuilder();
            builder.append( "Message: " ).append( body.getMessage() );
            if ( body.getErrors() != null ) {
                body.getErrors().forEach( ( k, v ) -> {
                    builder.append( '\n' ).append( k ).append( ": " ).append( v );
                } );
            }
            return responseBuilder
                    .type( MediaType.TEXT_PLAIN_TYPE )
                    .entity( builder.toString() )
                    .build();
        }
    }

    private boolean isXmlHttpRequest( HttpServletRequest request ) {
        return "XMLHttpRequest".equalsIgnoreCase( request.getHeader( "X-Requested-With" ) );
    }
}

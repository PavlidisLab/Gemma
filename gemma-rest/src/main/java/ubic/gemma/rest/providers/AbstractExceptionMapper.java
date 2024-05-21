package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.apachecommons.CommonsLog;
import org.glassfish.jersey.server.ContainerRequest;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.BuildInfoValueObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@CommonsLog
public abstract class AbstractExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    private final OpenAPI spec;
    private final BuildInfo buildInfo;

    @Context
    private ResourceContext ctx;

    protected AbstractExceptionMapper( OpenAPI spec, BuildInfo buildInfo ) {
        this.spec = spec;
        this.buildInfo = buildInfo;
    }

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

    protected Response.ResponseBuilder getResponseBuilder( ContainerRequest request, E exception ) {
        return Response.status( isXmlHttpRequest( request ) ? Response.Status.OK : getStatus( exception ) );
    }

    @Override
    public final Response toResponse( E exception ) {
        ContainerRequest request = ctx.getResource( ContainerRequest.class );
        if ( logException( exception ) ) {
            String m;
            if ( request != null ) {
                m = String.format( "Unhandled exception was raised for %s %s", request.getMethod(), request.getRequestUri() );
            } else {
                m = "Unhandled exception was raised, but there is no current request.";
            }
            log.error( m, exception );
        }
        String version;
        if ( spec.getInfo() != null ) {
            version = spec.getInfo().getVersion();
        } else {
            log.warn( "The 'info' field in the OpenAPI spec is null, will not include version in the error response." );
            version = null;
        }
        Response.ResponseBuilder responseBuilder = getResponseBuilder( request, exception );
        if ( request == null || acceptsJson( request ) ) {
            return responseBuilder
                    .type( MediaType.APPLICATION_JSON_TYPE )
                    .entity( new ResponseErrorObject( getWellComposedErrorBody( exception ), version, new BuildInfoValueObject( buildInfo ) ) )
                    .build();
        } else {
            WellComposedErrorBody body = getWellComposedErrorBody( exception );
            StringBuilder builder = new StringBuilder();
            builder.append( "Version: " ).append( version != null ? version : "?" ).append( '\n' );
            builder.append( "Build info: " ).append( buildInfo ).append( '\n' );
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

    private boolean acceptsJson( ContainerRequest request ) {
        return request.getAcceptableMediaTypes().stream().anyMatch( mediaType -> mediaType.isCompatible( MediaType.APPLICATION_JSON_TYPE ) );
    }

    private boolean isXmlHttpRequest( ContainerRequest request ) {
        return "XMLHttpRequest".equalsIgnoreCase( request.getHeaderString( "X-Requested-With" ) );
    }
}

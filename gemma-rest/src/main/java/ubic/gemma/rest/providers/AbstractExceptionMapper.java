package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.ContainerRequest;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.rest.util.BuildInfoValueObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedError;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.annotation.Nullable;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.List;
import java.util.concurrent.Future;

public abstract class AbstractExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    protected final Log log = LogFactory.getLog( getClass() );

    private final String hostUrl;
    private final Future<OpenAPI> spec;
    private final BuildInfo buildInfo;

    @Context
    private ResourceContext ctx;

    protected AbstractExceptionMapper( String hostUrl, Future<OpenAPI> spec, BuildInfo buildInfo ) {
        this.hostUrl = hostUrl;
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
        return WellComposedErrorBody.builder().code( getStatus( exception ).getStatusCode() ).message( exception.getMessage() ).build();
    }

    protected Response.ResponseBuilder getResponseBuilder( ContainerRequest request, E exception ) {
        return Response.status( isXmlHttpRequest( request ) ? Response.Status.OK : getStatus( exception ) );
    }

    @Override
    public final Response toResponse( E exception ) {
        ContainerRequest request = ctx.getResource( ContainerRequest.class );
        String requestMethod, requestUri;
        if ( request != null ) {
            requestMethod = request.getMethod();
            // make the request URI relative to the public-facing host URL
            requestUri = UriComponentsBuilder.fromHttpUrl( hostUrl )
                    .path( request.getRequestUri().getPath() )
                    .query( request.getRequestUri().getQuery() )
                    .fragment( request.getRequestUri().getFragment() )
                    .build()
                    .toUriString();
        } else {
            requestMethod = null;
            requestUri = null;
        }
        String version;
        if ( FutureUtils.get( spec ).getInfo() != null ) {
            version = FutureUtils.get( spec ).getInfo().getVersion();
        } else {
            log.warn( "The 'info' field in the OpenAPI spec is null, will not include version in the error response." );
            version = null;
        }
        Response.ResponseBuilder responseBuilder = getResponseBuilder( request, exception );
        WellComposedErrorBody body = getWellComposedErrorBody( exception );
        if ( request == null || acceptsJson( request ) ) {
            return responseBuilder
                    .type( MediaType.APPLICATION_JSON_TYPE )
                    .entity( ResponseErrorObject.builder().apiVersion( version ).buildInfo( BuildInfoValueObject.from( buildInfo ) ).error( body ).build() )
                    .build();
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append( "Request method: " ).append( requestMethod != null ? requestMethod : "?" ).append( '\n' );
            builder.append( "Request URI: " ).append( requestUri != null ? requestUri : "?" ).append( '\n' );
            builder.append( "Version: " ).append( version != null ? version : "?" ).append( '\n' );
            builder.append( "Build info: " ).append( buildInfo ).append( '\n' );
            builder.append( "Message: " ).append( body.getMessage() );
            List<WellComposedError> errors = body.getErrors();
            for ( int i = 0; i < errors.size(); i++ ) {
                WellComposedError e = errors.get( i );
                builder.append( '\n' ).append( '\n' )
                        .append( "Error #" ).append( i + 1 ).append( '\n' )
                        .append( "Reason: " ).append( e.getReason() ).append( '\n' )
                        .append( "Message: " ).append( e.getMessage() );
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

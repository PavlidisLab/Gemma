package ubic.gemma.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ubic.gemma.core.util.concurrent.FutureUtils;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.Future;

/**
 * Serve the OpenAPI specification held by the {@code openApi} bean.
 * <p>
 * This replaces {@code io.swagger.v3.jaxrs2.integration.resources.OpenApiResource}, which used to be registered
 * through {@code jersey.config.server.provider.packages} in {@code web.xml}. That resource re-read the spec from
 * Swagger's own {@code OpenApiContext} instead of returning the instance
 * {@link ubic.gemma.rest.util.OpenApiFactory} decorates, and the two could diverge permanently:
 * {@code GenericOpenApiContext.read()} caches with {@code cacheTTL = -1} but does an unsynchronized
 * check-then-act, so a request that landed while the factory's asynchronous build was still running produced a
 * second {@code OpenAPI} instance and pinned it in the cache for the life of the JVM. The served document then
 * lacked everything the factory applies after reading — the {@code servers} list built from {@code gemma.hosturl},
 * the {@code FilterArg}/{@code SortArg} examples and the {@code ${...}} placeholder resolution — while the Spring
 * bean kept the decorated copy. Resolving the {@link Future} here means there is exactly one instance and nothing
 * left to race over.
 * <p>
 * Requests that arrive before the build finishes now block on the future rather than racing it, so the first
 * caller after a restart waits for a complete spec instead of receiving a partial one.
 *
 * @author poirigui
 */
@Service
@Path("/openapi.{type:json|yaml}")
@Hidden
public class OpenApiWebService {

    private static final String APPLICATION_YAML = "application/yaml";

    @Autowired
    @Qualifier("openApi")
    private Future<OpenAPI> openApi;

    /**
     * Retrieve the specification as JSON or YAML, depending on the extension used in the request path.
     * <p>
     * The payload is large (~600 kB of JSON), so it is gzipped whenever the client advertises support. Setting
     * {@code Content-Encoding} here is what triggers Jersey's {@code GZipEncoder}: as a {@code ContentEncoder} it
     * compresses based on that response header rather than on {@code Accept-Encoding}. The previous
     * {@code OpenApiGzipHeaderDecorator} set the same header from a {@code WriterInterceptor} that recognised the
     * spec by its leading <code>{"openapi"</code> characters, and did so unconditionally; now that the endpoint is
     * ours, the header is set where the entity is built and only when the client asked for it.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, APPLICATION_YAML })
    public Response getOpenApi( @PathParam("type") String type, @HeaderParam(HttpHeaders.ACCEPT_ENCODING) String acceptEncoding ) throws JsonProcessingException {
        OpenAPI spec = FutureUtils.get( openApi );
        boolean yaml = "yaml".equals( type );
        String entity = yaml ? Yaml.mapper().writeValueAsString( spec ) : Json.mapper().writeValueAsString( spec );
        Response.ResponseBuilder builder = Response.ok( entity, yaml ? APPLICATION_YAML : MediaType.APPLICATION_JSON );
        if ( acceptEncoding != null && acceptEncoding.contains( "gzip" ) ) {
            builder.header( HttpHeaders.CONTENT_ENCODING, "gzip" );
        }
        return builder.build();
    }
}
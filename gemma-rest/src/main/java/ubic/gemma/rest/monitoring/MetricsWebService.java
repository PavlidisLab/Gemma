package ubic.gemma.rest.monitoring;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * Prometheus-format metrics scrape endpoint at {@code /rest/v2/metrics}. Token-gated: callers
 * must present the value of {@code gemma.metrics.scrapeToken} in the {@code X-Scrape-Token}
 * header. When the property is unset (empty), the endpoint returns 404 (metrics disabled)
 * so production cannot accidentally leak per-route timings.
 * <p>
 * The Prometheus registry bean is optional: if the {@code metrics} Spring profile is not
 * active there is no registry, and this resource returns 503 with a short text body.
 *
 * @author Phase 3 actuator wiring
 */
@Service
@Path("/metrics")
@CommonsLog
public class MetricsWebService {

    private static final String PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final String TOKEN_HEADER = "X-Scrape-Token";

    @Nullable
    @Autowired(required = false)
    private PrometheusMeterRegistry prometheusMeterRegistry;

    @Value("${gemma.metrics.scrapeToken:}")
    private String scrapeToken;

    @GET
    @Produces(PROMETHEUS_CONTENT_TYPE)
    @Tag(name = "Observability")
    @Operation(summary = "Prometheus exposition of process metrics",
            description = "Returns the Micrometer Prometheus exposition format (text/plain version 0.0.4). Requires the X-Scrape-Token header to match the gemma.metrics.scrapeToken property. When that property is unset, the endpoint is disabled (404).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Prometheus exposition body"),
                    @ApiResponse(responseCode = "401", description = "Missing or wrong X-Scrape-Token header"),
                    @ApiResponse(responseCode = "404", description = "Metrics endpoint disabled (no scrape token configured)"),
                    @ApiResponse(responseCode = "503", description = "Metrics Spring profile not active; no Prometheus registry available")
            })
    public Response scrape( @HeaderParam(TOKEN_HEADER) String presentedToken ) {
        if ( scrapeToken == null || scrapeToken.isEmpty() ) {
            return Response.status( Response.Status.NOT_FOUND )
                    .type( "text/plain; charset=utf-8" )
                    .entity( "metrics endpoint disabled (gemma.metrics.scrapeToken not set)\n" )
                    .build();
        }
        if ( presentedToken == null || !constantTimeEquals( scrapeToken, presentedToken ) ) {
            return Response.status( Response.Status.UNAUTHORIZED )
                    .type( "text/plain; charset=utf-8" )
                    .entity( "missing or invalid " + TOKEN_HEADER + " header\n" )
                    .build();
        }
        if ( prometheusMeterRegistry == null ) {
            return Response.status( Response.Status.SERVICE_UNAVAILABLE )
                    .type( "text/plain; charset=utf-8" )
                    .entity( "metrics profile not active\n" )
                    .build();
        }
        return Response.ok( prometheusMeterRegistry.scrape() )
                .type( PROMETHEUS_CONTENT_TYPE )
                .build();
    }

    /**
     * Constant-time string compare. Avoids early-exit timing leaks when validating the
     * scrape token against attacker-controlled input.
     */
    private static boolean constantTimeEquals( String a, String b ) {
        if ( a.length() != b.length() ) {
            return false;
        }
        int diff = 0;
        for ( int i = 0; i < a.length(); i++ ) {
            diff |= a.charAt( i ) ^ b.charAt( i );
        }
        return diff == 0;
    }
}

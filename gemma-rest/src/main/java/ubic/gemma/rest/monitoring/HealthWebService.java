package ubic.gemma.rest.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.rest.monitoring.health.HealthIndicator;
import ubic.gemma.rest.monitoring.health.HealthResult;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated process-health endpoint at {@code /rest/v2/health}. Returns the union of every
 * {@link HealthIndicator} bean in the application context; status is UP only if every
 * component is UP. Returns HTTP 503 when any component is DOWN so external uptime tools can
 * react without parsing the JSON.
 * <p>
 * Anonymous by default; the {@code /rest/v2/**} security chain in
 * {@code applicationContext-security.xml} permits anonymous access.
 *
 * @author Phase 3 actuator wiring
 */
@Service
@Path("/health")
@CommonsLog
public class HealthWebService {

    private final List<HealthIndicator> indicators;

    @Autowired
    public HealthWebService( List<HealthIndicator> indicators ) {
        this.indicators = indicators;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Observability")
    @Operation(summary = "Aggregated process health",
            description = "Returns UP only if every registered HealthIndicator (db, cache, disk space, ...) reports UP. Returns HTTP 503 with the same JSON shape when any component is DOWN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "All components UP", useReturnTypeSchema = true),
                    @ApiResponse(responseCode = "503", description = "At least one component reported DOWN", content = @Content(schema = @Schema(implementation = HealthValueObject.class)))
            })
    public Response getHealth() {
        Map<String, ComponentValueObject> components = new LinkedHashMap<>();
        boolean allUp = true;
        for ( HealthIndicator ind : indicators ) {
            HealthResult res;
            try {
                res = ind.check();
            } catch ( RuntimeException e ) {
                log.warn( "Health indicator " + ind.getName() + " threw", e );
                res = HealthResult.down( "indicator threw: " + e.getMessage() );
            }
            components.put( ind.getName(), new ComponentValueObject( res.getStatus().name(), res.getDetails() ) );
            if ( res.getStatus() != HealthResult.Status.UP ) {
                allUp = false;
            }
        }
        HealthValueObject body = new HealthValueObject( allUp ? "UP" : "DOWN", components );
        return Response.status( allUp ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE )
                .entity( body )
                .build();
    }

    @Value
    public static class HealthValueObject {
        String status;
        Map<String, ComponentValueObject> components;
    }

    @Value
    public static class ComponentValueObject {
        String status;
        Map<String, Object> details;
    }
}

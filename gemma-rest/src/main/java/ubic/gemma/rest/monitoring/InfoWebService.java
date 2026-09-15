package ubic.gemma.rest.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.util.BuildInfo;

import javax.annotation.Nullable;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Discrete process-info endpoint at {@code /rest/v2/info}. Wraps {@link BuildInfo} (already
 * populated from the gemma-core manifest by the {@code git-commit-id-maven-plugin}) plus
 * JVM/OS/uptime details. Anonymous; matches the {@code /rest/v2/**} security default.
 *
 * @author Phase 3 actuator wiring
 */
@Service
@Path("/info")
@CommonsLog
public class InfoWebService {

    private final BuildInfo buildInfo;

    @Autowired
    public InfoWebService( BuildInfo buildInfo ) {
        this.buildInfo = buildInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Observability")
    @Operation(summary = "Process build / JVM / OS info",
            description = "Stable URL for polling the running build version, git hash, JVM, OS, and uptime. Use this instead of parsing the larger root API payload when only the build info is needed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Info payload", useReturnTypeSchema = true)
            })
    public InfoValueObject getInfo() {
        return new InfoValueObject(
                new BuildBlock( buildInfo.getVersion(), formatTimestamp( buildInfo.getTimestamp() ), buildInfo.getGitHash() ),
                new JavaBlock(
                        System.getProperty( "java.version" ),
                        System.getProperty( "java.vendor" ),
                        System.getProperty( "java.vm.name" ) ),
                new OsBlock(
                        System.getProperty( "os.name" ),
                        System.getProperty( "os.version" ),
                        System.getProperty( "os.arch" ) ),
                getUptimeBlock() );
    }

    @Nullable
    private static String formatTimestamp( @Nullable Date d ) {
        if ( d == null ) {
            return null;
        }
        DateFormat iso = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH );
        iso.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return iso.format( d );
    }

    private static UptimeBlock getUptimeBlock() {
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        return new UptimeBlock( rt.getStartTime(), rt.getUptime() );
    }

    @Value
    public static class InfoValueObject {
        BuildBlock build;
        JavaBlock java;
        OsBlock os;
        UptimeBlock uptime;
    }

    @Value
    public static class BuildBlock {
        @Nullable String version;
        @Nullable String timestamp;
        @Nullable String gitHash;
    }

    @Value
    public static class JavaBlock {
        String version;
        String vendor;
        String vm;
    }

    @Value
    public static class OsBlock {
        String name;
        String version;
        String arch;
    }

    @Value
    public static class UptimeBlock {
        long startTimeMillis;
        long uptimeMillis;
    }
}

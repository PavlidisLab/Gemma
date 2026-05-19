package ubic.gemma.rest.monitoring.health;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Disk-space health probe. Checks the configured {@code gemma.appdata.home} directory and
 * reports DOWN if usable free space falls below {@code gemma.health.diskSpace.thresholdBytes}
 * (default 100 MB).
 *
 * @author Phase 3 actuator wiring
 */
@Component
@CommonsLog
public class DiskSpaceHealthIndicator implements HealthIndicator {

    private static final long DEFAULT_THRESHOLD_BYTES = 100L * 1024L * 1024L; // 100 MB

    @Value("${gemma.appdata.home:}")
    private String appdataHome;

    @Value("${gemma.health.diskSpace.thresholdBytes:" + DEFAULT_THRESHOLD_BYTES + "}")
    private long thresholdBytes;

    @Override
    public String getName() {
        return "diskSpace";
    }

    @Override
    public HealthResult check() {
        if ( appdataHome == null || appdataHome.isEmpty() ) {
            return HealthResult.down( "gemma.appdata.home is not configured" );
        }
        File path = new File( appdataHome );
        if ( !path.exists() ) {
            return HealthResult.down( "appdata home does not exist: " + appdataHome );
        }
        long total = path.getTotalSpace();
        long free = path.getUsableSpace();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put( "path", appdataHome );
        details.put( "totalBytes", total );
        details.put( "freeBytes", free );
        details.put( "thresholdBytes", thresholdBytes );
        if ( free < thresholdBytes ) {
            details.put( "error", "free space below threshold" );
            return HealthResult.down( details );
        }
        return HealthResult.up( details );
    }
}

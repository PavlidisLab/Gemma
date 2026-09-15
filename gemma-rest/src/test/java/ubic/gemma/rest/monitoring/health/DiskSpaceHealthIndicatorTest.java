package ubic.gemma.rest.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskSpaceHealthIndicatorTest {

    private DiskSpaceHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new DiskSpaceHealthIndicator();
    }

    @Test
    void getName_isDiskSpace() {
        assertEquals( "diskSpace", indicator.getName() );
    }

    @Test
    void appdataHomeBlank_yieldsDown() {
        ReflectionTestUtils.setField( indicator, "appdataHome", "" );
        ReflectionTestUtils.setField( indicator, "thresholdBytes", 1L );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertEquals( "gemma.appdata.home is not configured", r.getDetails().get( "error" ) );
    }

    @Test
    void appdataHomeNull_yieldsDown() {
        ReflectionTestUtils.setField( indicator, "appdataHome", null );
        ReflectionTestUtils.setField( indicator, "thresholdBytes", 1L );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertEquals( "gemma.appdata.home is not configured", r.getDetails().get( "error" ) );
    }

    @Test
    void pathMissing_yieldsDown( @TempDir Path tmp ) {
        File missing = tmp.resolve( "does-not-exist" ).toFile();
        ReflectionTestUtils.setField( indicator, "appdataHome", missing.getAbsolutePath() );
        ReflectionTestUtils.setField( indicator, "thresholdBytes", 1L );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertTrue( ( ( String ) r.getDetails().get( "error" ) ).contains( "appdata home does not exist" ) );
    }

    @Test
    void freeSpaceAboveThreshold_yieldsUp( @TempDir Path tmp ) {
        ReflectionTestUtils.setField( indicator, "appdataHome", tmp.toFile().getAbsolutePath() );
        // Threshold of 1 byte — any real tmpdir will exceed this.
        ReflectionTestUtils.setField( indicator, "thresholdBytes", 1L );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.UP, r.getStatus() );
        assertEquals( tmp.toFile().getAbsolutePath(), r.getDetails().get( "path" ) );
        assertEquals( 1L, r.getDetails().get( "thresholdBytes" ) );
        assertTrue( r.getDetails().containsKey( "freeBytes" ) );
        assertTrue( r.getDetails().containsKey( "totalBytes" ) );
    }

    @Test
    void freeSpaceBelowThreshold_yieldsDown( @TempDir Path tmp ) {
        ReflectionTestUtils.setField( indicator, "appdataHome", tmp.toFile().getAbsolutePath() );
        // Threshold of Long.MAX_VALUE — no real disk has this much free.
        ReflectionTestUtils.setField( indicator, "thresholdBytes", Long.MAX_VALUE );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertEquals( "free space below threshold", r.getDetails().get( "error" ) );
        assertTrue( r.getDetails().containsKey( "freeBytes" ) );
        assertEquals( Long.MAX_VALUE, r.getDetails().get( "thresholdBytes" ) );
    }
}

package ubic.gemma.rest.monitoring.health;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthResultTest {

    @Test
    void upNoArgs_yieldsUpWithEmptyDetails() {
        HealthResult r = HealthResult.up();
        assertEquals( HealthResult.Status.UP, r.getStatus() );
        assertTrue( r.getDetails().isEmpty() );
    }

    @Test
    void upWithDetails_copiesAndExposesEntries() {
        Map<String, Object> src = new LinkedHashMap<>();
        src.put( "database", "MySQL" );
        src.put( "elapsedMs", 12L );
        HealthResult r = HealthResult.up( src );
        assertEquals( HealthResult.Status.UP, r.getStatus() );
        assertEquals( "MySQL", r.getDetails().get( "database" ) );
        assertEquals( 12L, r.getDetails().get( "elapsedMs" ) );
        // mutate the source map after construction; result must not reflect mutation
        src.put( "added", "later" );
        assertEquals( 2, r.getDetails().size() );
    }

    @Test
    void downWithMessage_storesErrorEntry() {
        HealthResult r = HealthResult.down( "boom" );
        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertEquals( "boom", r.getDetails().get( "error" ) );
    }

    @Test
    void downWithDetailsMap_copiesEntries() {
        Map<String, Object> src = new LinkedHashMap<>();
        src.put( "error", "free space below threshold" );
        src.put( "freeBytes", 42L );
        HealthResult r = HealthResult.down( src );
        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertEquals( "free space below threshold", r.getDetails().get( "error" ) );
        assertEquals( 42L, r.getDetails().get( "freeBytes" ) );
    }

    @Test
    void detailsMap_isUnmodifiable() {
        HealthResult r = HealthResult.up();
        assertThrows( UnsupportedOperationException.class, () -> r.getDetails().put( "k", "v" ) );
    }

    @Test
    void status_enumHasUpAndDown() {
        // sanity guard against accidental enum reordering / removal
        assertNotNull( HealthResult.Status.valueOf( "UP" ) );
        assertNotNull( HealthResult.Status.valueOf( "DOWN" ) );
    }
}

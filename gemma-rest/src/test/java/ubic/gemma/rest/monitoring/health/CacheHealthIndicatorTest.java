package ubic.gemma.rest.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheHealthIndicatorTest {

    @Mock
    private CacheManager cacheManager;

    private CacheHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new CacheHealthIndicator( cacheManager );
    }

    @Test
    void getName_isCache() {
        assertEquals( "cache", indicator.getName() );
    }

    @Test
    void happyPath_reportsCacheCount() {
        when( cacheManager.getCacheNames() ).thenReturn( Arrays.asList( "a", "b", "c" ) );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.UP, r.getStatus() );
        assertEquals( 3, r.getDetails().get( "caches" ) );
        // cacheManager class name should be the mock's simple name (non-null/non-empty)
        assertTrue( r.getDetails().containsKey( "cacheManager" ) );
    }

    @Test
    void emptyCacheManager_reportsZeroAndStaysUp() {
        when( cacheManager.getCacheNames() ).thenReturn( Collections.emptyList() );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.UP, r.getStatus() );
        assertEquals( 0, r.getDetails().get( "caches" ) );
    }

    @Test
    void nullCacheNames_reportsZeroAndStaysUp() {
        when( cacheManager.getCacheNames() ).thenReturn( null );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.UP, r.getStatus() );
        assertEquals( 0, r.getDetails().get( "caches" ) );
    }

    @Test
    void cacheManagerThrows_yieldsDown() {
        when( cacheManager.getCacheNames() ).thenThrow( new IllegalStateException( "manager kaput" ) );

        HealthResult r = indicator.check();

        assertEquals( HealthResult.Status.DOWN, r.getStatus() );
        assertTrue( ( ( String ) r.getDetails().get( "error" ) ).contains( "manager kaput" ) );
    }
}

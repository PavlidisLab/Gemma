package ubic.gemma.rest.monitoring;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import ubic.gemma.rest.monitoring.health.HealthIndicator;
import ubic.gemma.rest.monitoring.health.HealthResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito tests for {@link HealthWebService}. No Spring context: indicators are constructed
 * as plain mocks and passed to the constructor.
 */
class HealthWebServiceTest {

    private static HealthIndicator indicator( String name, HealthResult result ) {
        HealthIndicator ind = mock( HealthIndicator.class );
        when( ind.getName() ).thenReturn( name );
        when( ind.check() ).thenReturn( result );
        return ind;
    }

    @Test
    void allIndicatorsUp_returns200AndUpBody() {
        HealthWebService svc = new HealthWebService( Arrays.asList(
                indicator( "db", HealthResult.up() ),
                indicator( "cache", HealthResult.up() ) ) );

        Response r = svc.getHealth();

        assertThat( r.getStatus() ).isEqualTo( 200 );
        HealthWebService.HealthValueObject body = (HealthWebService.HealthValueObject) r.getEntity();
        assertThat( body.getStatus() ).isEqualTo( "UP" );
        assertThat( body.getComponents() ).containsOnlyKeys( "db", "cache" );
        assertThat( body.getComponents().get( "db" ).getStatus() ).isEqualTo( "UP" );
        assertThat( body.getComponents().get( "cache" ).getStatus() ).isEqualTo( "UP" );
    }

    @Test
    void anyIndicatorDown_returns503AndDownBody() {
        HealthWebService svc = new HealthWebService( Arrays.asList(
                indicator( "db", HealthResult.up() ),
                indicator( "cache", HealthResult.down( "cache offline" ) ) ) );

        Response r = svc.getHealth();

        assertThat( r.getStatus() ).isEqualTo( 503 );
        HealthWebService.HealthValueObject body = (HealthWebService.HealthValueObject) r.getEntity();
        assertThat( body.getStatus() ).isEqualTo( "DOWN" );
        assertThat( body.getComponents().get( "db" ).getStatus() ).isEqualTo( "UP" );
        assertThat( body.getComponents().get( "cache" ).getStatus() ).isEqualTo( "DOWN" );
        assertThat( body.getComponents().get( "cache" ).getDetails() ).containsEntry( "error", "cache offline" );
    }

    @Test
    void indicatorThrows_isCaughtAndReportedAsDown() {
        HealthIndicator boom = mock( HealthIndicator.class );
        when( boom.getName() ).thenReturn( "boom" );
        when( boom.check() ).thenThrow( new RuntimeException( "kaboom" ) );

        HealthWebService svc = new HealthWebService( Arrays.asList(
                indicator( "db", HealthResult.up() ),
                boom ) );

        Response r = svc.getHealth();

        assertThat( r.getStatus() ).isEqualTo( 503 );
        HealthWebService.HealthValueObject body = (HealthWebService.HealthValueObject) r.getEntity();
        assertThat( body.getStatus() ).isEqualTo( "DOWN" );
        assertThat( body.getComponents().get( "boom" ).getStatus() ).isEqualTo( "DOWN" );
        assertThat( body.getComponents().get( "boom" ).getDetails().get( "error" ).toString() )
                .contains( "indicator threw" )
                .contains( "kaboom" );
    }

    @Test
    void emptyIndicatorList_returns200AndUpBodyWithNoComponents() {
        HealthWebService svc = new HealthWebService( Collections.emptyList() );

        Response r = svc.getHealth();

        assertThat( r.getStatus() ).isEqualTo( 200 );
        HealthWebService.HealthValueObject body = (HealthWebService.HealthValueObject) r.getEntity();
        assertThat( body.getStatus() ).isEqualTo( "UP" );
        assertThat( body.getComponents() ).isEmpty();
    }

    @Test
    void detailsPreservedOnUpIndicators() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put( "database", "MySQL" );
        details.put( "freeBytes", 1234L );
        HealthIndicator ind = mock( HealthIndicator.class );
        when( ind.getName() ).thenReturn( "db" );
        when( ind.check() ).thenReturn( HealthResult.up( details ) );

        HealthWebService svc = new HealthWebService( Collections.singletonList( ind ) );

        Response r = svc.getHealth();

        assertThat( r.getStatus() ).isEqualTo( 200 );
        HealthWebService.HealthValueObject body = (HealthWebService.HealthValueObject) r.getEntity();
        assertThat( body.getComponents().get( "db" ).getDetails() )
                .containsEntry( "database", "MySQL" )
                .containsEntry( "freeBytes", 1234L );
    }
}

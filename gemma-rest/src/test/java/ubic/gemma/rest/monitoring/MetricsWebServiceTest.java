package ubic.gemma.rest.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-Mockito tests for {@link MetricsWebService}. Uses a real {@link PrometheusMeterRegistry}
 * (cheap to construct, no Spring) and injects field values via {@link ReflectionTestUtils} since
 * the production code uses {@code @Value} + {@code @Autowired(required = false)}.
 */
class MetricsWebServiceTest {

    private static final String TOKEN_HEADER_VALUE = "the-correct-token";

    private MetricsWebService svc;

    @BeforeEach
    void setUp() {
        svc = new MetricsWebService();
    }

    @Test
    void blankScrapeToken_returns404() {
        ReflectionTestUtils.setField( svc, "scrapeToken", "" );
        ReflectionTestUtils.setField( svc, "prometheusMeterRegistry", null );

        Response r = svc.scrape( "anything" );

        assertThat( r.getStatus() ).isEqualTo( 404 );
        assertThat( r.getEntity().toString() ).contains( "metrics endpoint disabled" );
    }

    @Test
    void nullScrapeTokenProperty_returns404() {
        // @Value("${gemma.metrics.scrapeToken:}") would resolve to "" in real Spring, but guard
        // the null path explicitly — the production code checks both.
        ReflectionTestUtils.setField( svc, "scrapeToken", null );
        ReflectionTestUtils.setField( svc, "prometheusMeterRegistry", null );

        Response r = svc.scrape( "anything" );

        assertThat( r.getStatus() ).isEqualTo( 404 );
    }

    @Test
    void missingTokenHeader_returns401() {
        ReflectionTestUtils.setField( svc, "scrapeToken", TOKEN_HEADER_VALUE );
        ReflectionTestUtils.setField( svc, "prometheusMeterRegistry", null );

        Response r = svc.scrape( null );

        assertThat( r.getStatus() ).isEqualTo( 401 );
        assertThat( r.getEntity().toString() ).contains( "X-Scrape-Token" );
    }

    @Test
    void wrongTokenHeader_returns401() {
        ReflectionTestUtils.setField( svc, "scrapeToken", TOKEN_HEADER_VALUE );
        ReflectionTestUtils.setField( svc, "prometheusMeterRegistry", null );

        Response r = svc.scrape( "wrong-token" );

        assertThat( r.getStatus() ).isEqualTo( 401 );
    }

    @Test
    void wrongLengthTokenHeader_returns401() {
        // exercises the length-mismatch short-circuit inside constantTimeEquals
        ReflectionTestUtils.setField( svc, "scrapeToken", TOKEN_HEADER_VALUE );
        ReflectionTestUtils.setField( svc, "prometheusMeterRegistry", null );

        Response r = svc.scrape( "short" );

        assertThat( r.getStatus() ).isEqualTo( 401 );
    }

    @Test
    void correctTokenButRegistryNull_returns503() {
        ReflectionTestUtils.setField( svc, "scrapeToken", TOKEN_HEADER_VALUE );
        ReflectionTestUtils.setField( svc, "prometheusMeterRegistry", null );

        Response r = svc.scrape( TOKEN_HEADER_VALUE );

        assertThat( r.getStatus() ).isEqualTo( 503 );
        assertThat( r.getEntity().toString() ).contains( "metrics profile not active" );
    }

    @Test
    void correctTokenAndRegistryPresent_returns200WithPrometheusBody() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );
        Counter.builder( "gemma_test_counter_total" ).register( registry ).increment( 3 );

        ReflectionTestUtils.setField( svc, "scrapeToken", TOKEN_HEADER_VALUE );
        ReflectionTestUtils.setField( svc, "prometheusMeterRegistry", registry );

        Response r = svc.scrape( TOKEN_HEADER_VALUE );

        assertThat( r.getStatus() ).isEqualTo( 200 );
        // JAX-RS may reorder MediaType parameters when stringifying; assert structurally
        assertThat( r.getMediaType().getType() ).isEqualTo( "text" );
        assertThat( r.getMediaType().getSubtype() ).isEqualTo( "plain" );
        assertThat( r.getMediaType().getParameters() )
                .containsEntry( "version", "0.0.4" )
                .containsEntry( "charset", "utf-8" );
        assertThat( r.getEntity().toString() ).contains( "gemma_test_counter" );
    }
}

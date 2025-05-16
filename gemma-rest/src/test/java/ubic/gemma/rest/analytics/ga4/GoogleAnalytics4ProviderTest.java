package ubic.gemma.rest.analytics.ga4;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.web.client.RestTemplate;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.rest.analytics.AnalyticsProvider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ubic.gemma.rest.util.Assertions.assertThat;

@CommonsLog
public class GoogleAnalytics4ProviderTest {

    private GoogleAnalytics4Provider provider;

    @Before
    public void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add( ( request, body, execution ) -> {
            log.info( String.format( "\nURL: %s\nPayload: %s", request.getURI(), new String( body, StandardCharsets.UTF_8 ) ) );
            return execution.execute( request, body );
        } );
        provider = new GoogleAnalytics4Provider( restTemplate, "test", "test" );
        provider.afterPropertiesSet();
        provider.setDebug( true );
        ThreadLocal<String> clientId = ThreadLocal.withInitial( () -> RandomStringUtils.randomNumeric( 10 ) + "." + RandomStringUtils.randomNumeric( 10 ) );
        provider.setClientIdRetrievalStrategy( clientId::get );
        provider.setPollingIntervalMillis( 1000 );
    }

    @After
    public void tearDown() throws Exception {
        provider.destroy();
    }

    @Test
    public void testBlankMeasurementId() {
        AnalyticsProvider provider = new GoogleAnalytics4Provider( "", "" );
        provider.sendEvent( "page_view" );
        assertThatThrownBy( () -> new GoogleAnalytics4Provider( "test", "" ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    @Category(SlowTest.class)
    public void test() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool( 16 );
        Collection<Future<?>> futures = new ArrayList<>();
        for ( int i = 0; i < 500; i++ ) {
            futures.add( executor.submit( () -> {
                provider.sendEvent( "page_view", new Date(), "Key", "Value" );
            } ) );
        }
        long maxWait = 15L * 1000L * 1000L * 1000L;
        long initialTime = System.nanoTime();
        assertThat( futures ).allSatisfy( f -> {
            long elapsed = System.nanoTime() - initialTime;
            assertThat( f ).succeedsWithin( Math.max( 0, maxWait - elapsed ), TimeUnit.NANOSECONDS );
        } );
    }

    @Test
    public void testInvalidEvent() {
        assertThatThrownBy( () -> provider.sendEvent( RandomStringUtils.randomAlphabetic( 41 ) ) )
                .asInstanceOf( InstanceOfAssertFactories.throwable( InvalidEventException.class ) )
                .extracting( InvalidEventException::getErrors )
                .satisfies( errors -> {
                    assertThat( errors ).hasFieldError( "event", "name", "size" );
                } );

        assertThatThrownBy( () -> provider.sendEvent( RandomStringUtils.randomAlphabetic( 30 ),
                "test", RandomStringUtils.randomAlphabetic( 101 ) ) )
                .asInstanceOf( InstanceOfAssertFactories.throwable( InvalidEventException.class ) )
                .extracting( InvalidEventException::getErrors )
                .satisfies( errors -> {
                    assertThat( errors ).hasFieldError( "event", "params", "sizeOfValue" );
                } );
    }

    @Test
    public void testReservedEventName() {
        assertThatThrownBy( () -> provider.sendEvent( "first_open" ) )
                .asInstanceOf( InstanceOfAssertFactories.throwable( InvalidEventException.class ) )
                .extracting( InvalidEventException::getErrors )
                .satisfies( errors -> {
                    assertThat( errors ).hasFieldError( "event", "name", "reservedEventName", "first_open" );
                } );
    }

    @Test
    public void testFutureEvent() {
        assertThatThrownBy( () -> provider.sendEvent( RandomStringUtils.randomAlphabetic( 30 ), new Date( System.currentTimeMillis() + 10000 ) ) )
                .isInstanceOf( InvalidEventException.class )
                .asInstanceOf( InstanceOfAssertFactories.throwable( InvalidEventException.class ) )
                .extracting( InvalidEventException::getErrors )
                .satisfies( errors -> {
                    assertThat( errors ).hasFieldError( "event", "date", "dateMustBeInPast" );
                } );
    }
}
package ubic.gemma.rest.analytics.ga4;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.web.client.RestTemplate;
import ubic.gemma.core.util.test.category.SlowTest;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;

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
        provider = new GoogleAnalytics4Provider( restTemplate, new ScheduledThreadPoolExecutor( 1 ), "test", "test" );
        provider.afterPropertiesSet();
        provider.setDebug( true );
        ThreadLocal<String> clientId = ThreadLocal.withInitial( () -> UUID.randomUUID().toString() );
        provider.setClientIdRetrievalStrategy( clientId::get );
        provider.setPollingIntervalMillis( 1000 );
    }

    @After
    public void tearDown() throws Exception {
        provider.destroy();
    }

    @Test
    @Category(SlowTest.class)
    public void test() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool( 16 );
        for ( int i = 0; i < 500; i++ ) {
            executor.submit( () -> {
                provider.sendEvent( "page_view", new Date(), "Key", "Value" );
            } );
        }
        executor.shutdown();
        assertTrue( executor.awaitTermination( Integer.MAX_VALUE, TimeUnit.SECONDS ) );
    }

    @Test
    public void testInvalidEvent() {
        assertThatThrownBy( () -> {
            provider.sendEvent( RandomStringUtils.randomAlphabetic( 41 ) );
        } ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> {
            provider.sendEvent( RandomStringUtils.randomAlphabetic( 30 ),
                    "test", RandomStringUtils.randomAlphabetic( 101 ) );
        } ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> {
            provider.sendEvent( "first_open" );
        } ).isInstanceOf( IllegalArgumentException.class );
    }
}
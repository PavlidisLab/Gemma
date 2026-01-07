package ubic.gemma.core.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;
import ubic.gemma.core.util.test.category.IntegrationTest;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

@Category(IntegrationTest.class)
@NetworkAvailable(url = "https://gemma.msl.ubc.ca/rest/v2")
public class GemmaRestApiClientTest {

    private static final String GEMMA_HOST_URL = "https://gemma.msl.ubc.ca";

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    private final GemmaRestApiClient client = new GemmaRestApiClientImpl( GEMMA_HOST_URL );

    @Test
    public void test() throws IOException {
        assertThat( client.perform( "/" ) )
                .asInstanceOf( type( GemmaRestApiClient.DataResponse.class ) )
                .extracting( GemmaRestApiClient.DataResponse::getData )
                .hasFieldOrProperty( "welcome" )
                .hasFieldOrProperty( "version" )
                .hasFieldOrProperty( "externalDatabases" )
                .hasFieldOrProperty( "buildInfo" );
    }

    @Test
    public void testEndpointWithParameters() throws IOException {
        assertThat( client.perform( "/datasets", "query", "BRCA1" ) )
                .isInstanceOf( GemmaRestApiClient.DataResponse.class );
    }

    @Test
    public void testEndpointThatRequiresCredentials() throws IOException {
        assertThat( client.perform( "/users/me" ) )
                .asInstanceOf( type( GemmaRestApiClient.ErrorResponse.class ) )
                .satisfies( r -> {
                    assertThat( r.getError().getCode() ).isEqualTo( 401 );
                } );
    }

    @Test
    public void testEndpointWithIncorrectCredentials() throws IOException {
        try {
            Authentication auth = new UsernamePasswordAuthenticationToken( "foo", "1234", Collections.emptyList() );
            client.setAuthentication( auth );
            assertThat( client.perform( "/" ) )
                    .asInstanceOf( type( GemmaRestApiClient.ErrorResponse.class ) )
                    .satisfies( r -> {
                        assertThat( r.getError().getCode() ).isEqualTo( 401 );
                    } );
        } finally {
            client.clearAuthentication();
        }
    }

    @Test
    public void testEndpointWithRedirection() throws IOException {
        assertThat( client.perform( "/datasets/1/analyses/differential/resultSets" ) )
                .asInstanceOf( type( GemmaRestApiClient.DataResponse.class ) )
                .extracting( GemmaRestApiClient.DataResponse::getData )
                .asInstanceOf( list( Object.class ) )
                .isNotEmpty()
                .allSatisfy( o -> assertThat( o ).hasFieldOrPropertyWithValue( "analysis.bioAssaySetId", 1 ) );
    }

    @Test
    public void testNotFoundEndpoint() throws IOException {
        assertThat( client.perform( "/bleh" ) )
                .asInstanceOf( type( GemmaRestApiClient.ErrorResponse.class ) )
                .satisfies( r -> {
                    assertThat( r.getError().getCode() ).isEqualTo( 404 );
                } );
    }

    @Test
    public void testCompressedEndpoint() throws IOException {
        assertThat( client.perform( "/datasets" ) )
                .isInstanceOf( GemmaRestApiClient.DataResponse.class );
    }
}
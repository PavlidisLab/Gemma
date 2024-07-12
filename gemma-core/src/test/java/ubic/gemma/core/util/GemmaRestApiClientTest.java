package ubic.gemma.core.util;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

public class GemmaRestApiClientTest {

    private static final String GEMMA_HOST_URL = "https://gemma.msl.ubc.ca";

    private final GemmaRestApiClient client = new GemmaRestApiClientImpl( GEMMA_HOST_URL );

    @BeforeClass
    public static void setUp() {
        assumeThatResourceIsAvailable( GEMMA_HOST_URL + "/rest/v2" );
    }

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
            client.setAuthentication( "foo", "1234" );
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
                .hasFieldOrPropertyWithValue( "id", 1 );
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
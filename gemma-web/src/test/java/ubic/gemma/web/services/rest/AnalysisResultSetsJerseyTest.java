package ubic.gemma.web.services.rest;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;
import ubic.gemma.web.util.BaseJerseyTest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the result sets endpoint.
 *
 * This also contains tests of other endpoints as using multiple classes would result in Spring context being reloaded
 * multiple times over.
 *
 * @author poirigui
 */
@CommonsLog
public class AnalysisResultSetsJerseyTest extends BaseJerseyTest {

    @Test
    public void testGetResultSets() {
        Response response = target( "/resultSets" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
        assertThat( response.getHeaderString( "Content-Type" ) ).isEqualTo( MediaType.APPLICATION_JSON );
    }

    @Test
    public void testGetDatasets() {
        Response response = target( "/datasets" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
        assertThat( response.getHeaderString( "Content-Type" ) ).isEqualTo( MediaType.APPLICATION_JSON );
    }

    @Test
    public void testGetPlatforms() {
        Response response = target( "/platforms" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
        assertThat( response.getHeaderString( "Content-Type" ) ).isEqualTo( MediaType.APPLICATION_JSON );
    }

    @Test
    public void testUnmappedRoute() {
        Response response = target( "/unmapped" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 404 );
        assertThat( response.getHeaderString( "Content-Type" ) ).isEqualTo( MediaType.APPLICATION_JSON );
    }

    @Test
    public void testOpenApiEndpoint() throws IOException {
        Response response = target( "/openapi.json" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
        assertThat( response.getHeaderString( "Content-Type" ) ).isEqualTo( MediaType.APPLICATION_JSON );
        InputStream reader = ( InputStream ) response.getEntity();
        OpenAPI openAPI = Json.mapper().readValue( reader, OpenAPI.class );
        assertThat( openAPI.getInfo().getTitle() ).isEqualTo( "Gemma RESTful API" );
    }
}

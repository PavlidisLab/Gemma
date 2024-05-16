package ubic.gemma.rest;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.Assertions;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
public class AnalysisResultSetsJerseyTest extends BaseJerseyIntegrationTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /* fixture */
    private ExpressionExperiment ee;

    @Before
    public void setUpMocks() {
        ee = ExpressionExperiment.Factory.newInstance();
        ee = expressionExperimentService.create( ee );
    }

    @After
    public void removeFixtures() {
        expressionExperimentService.remove( ee );
    }

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
    public void testOpenApiEndpoint() {
        Response response = target( "/openapi.json" ).request().get();
        Assertions.assertThat( response )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entityAs( String.class )
                .satisfies( payload -> {
                    OpenAPI openAPI = Json.mapper().readValue( payload, OpenAPI.class );
                    assertThat( openAPI.getInfo().getTitle() ).isEqualTo( "Gemma RESTful API" );
                } );
    }

    @Test
    public void testRedirection() {
        Response response = target( "/datasets/" + ee.getId() + "/analyses/differential/resultSets" ).request().get();
        // jax-rs performs a redirection for a 302, it's a good thing, but it makes testing quite difficult
        // TODO: inspect the JSON payload to make sure it matches the getResultSets() endpoint (which is also difficult because it uses a generic type as return value)
        assertThat( response.getStatus() ).isEqualTo( 200 );
    }
}

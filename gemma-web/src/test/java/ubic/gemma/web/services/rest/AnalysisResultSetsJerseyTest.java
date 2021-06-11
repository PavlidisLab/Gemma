package ubic.gemma.web.services.rest;

import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;
import ubic.gemma.web.util.BaseSpringWebJerseyTest;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the result sets endpoint.
 * @author poirigui
 */
@CommonsLog
public class AnalysisResultSetsJerseyTest extends BaseSpringWebJerseyTest {

    @Test
    public void testGetResultSets() {
        Response response = target( "/resultSets" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void testGetDatasets() {
        Response response = target( "/datasets" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void testGetPlatforms() {
        Response response = target( "/platforms" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void testUnmappedRoute() {
        Response response = target( "/unmapped" ).request().get();
        assertThat( response.getStatus() ).isEqualTo( 404 );
    }
}

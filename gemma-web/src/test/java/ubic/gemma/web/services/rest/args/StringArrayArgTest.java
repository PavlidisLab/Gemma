package ubic.gemma.web.services.rest.args;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;
import ubic.gemma.web.services.rest.util.args.StringArrayArg;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static org.assertj.core.api.Assertions.assertThat;

public class StringArrayArgTest {

    @Test
    public void testParametersAnnotatedWithExplode() {
        Reader reader = new Reader( new OpenAPI() );
        OpenAPI spec = reader.read( Api.class );
        assertThat( spec.getPaths().get( "/" ).getGet().getParameters().get( 0 ) )
                .hasFieldOrPropertyWithValue( "name", "query" )
                .hasFieldOrPropertyWithValue( "explode", false );
        ;
    }

    public static class Api {

        @GET
        @Path("/")
        @Parameter(name = "query", in = ParameterIn.QUERY, explode = Explode.FALSE, schema = @Schema(implementation = StringArrayArg.class))
        public String search( @QueryParam("query") StringArrayArg query ) {
            return "test";
        }
    }
}

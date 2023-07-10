package ubic.gemma.rest.util.args;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static org.assertj.core.api.Assertions.assertThat;

public class StringArrayArgTest {

    @Test
    public void testBase64EncodedFilter() {
        StringArrayArg arg = StringArrayArg.valueOf( "H4sIAAAAAAAAA0vUUVBI0knWUUgBAJsnMwkKAAAA" );
        assertThat( arg.getValue() ).containsExactly( "a", "b", "c", "d" );
    }

    @Test
    public void testEmptyBase64EncodedArray() {
        StringArrayArg arg = StringArrayArg.valueOf( "H4sIAAAAAAAAAwMAAAAAAAAAAAA=" );
        assertThat( arg.getValue() ).isEmpty();
    }

    @Test
    public void testEmptyArray() {
        StringArrayArg arg = StringArrayArg.valueOf( "" );
        assertThat( arg.getValue() ).isEmpty();
    }

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

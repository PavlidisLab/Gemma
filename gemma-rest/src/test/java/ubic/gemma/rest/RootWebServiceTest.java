package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.rest.util.Assertions;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

public class RootWebServiceTest extends BaseJerseyIntegrationTest {

    @Autowired
    @Qualifier("openApi")
    private Future<OpenAPI> openApi;

    @Value("${gemma.externalDatabases.featured}")
    private List<String> featuredExternalDatabases;

    @Test
    public void test() {
        String expectedVersion = FutureUtils.get( openApi ).getInfo().getVersion();
        assertThat( expectedVersion ).isNotBlank();
        assertThat( featuredExternalDatabases ).isNotEmpty();
        Assertions.assertThat( target( "/" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data" )
                .hasFieldOrPropertyWithValue( "version", expectedVersion )
                .hasFieldOrPropertyWithValue( "docs", "/resources/restapidocs/" )
                .extracting( "externalDatabases", list( Object.class ) )
                .extracting( "name" )
                .containsExactlyElementsOf( featuredExternalDatabases );
    }
}

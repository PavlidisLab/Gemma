package ubic.gemma.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.rest.util.OpenApiUtils;
import ubic.gemma.rest.util.ResponseDataObject;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class RootWebServiceTest extends BaseSpringContextTest {

    @Autowired
    private RootWebService rootWebService;

    @Test
    public void test() {
        ResponseDataObject<RootWebService.ApiInfoValueObject> response = rootWebService.getApiInfo( new MockHttpServletRequest(), new MockServletConfig() );
        String expectedVersion = OpenApiUtils.getOpenApi( null ).getInfo().getVersion();
        assertThat( expectedVersion ).isNotBlank();
        assertThat( response.getData().getVersion() ).isEqualTo( expectedVersion );
        assertThat( response.getData().getDocs() ).isEqualTo( URI.create( "http://localhost/resources/restapidocs/" ) );
        assertThat( response.getData().getExternalDatabases() )
                .extracting( "name" ).containsExactly( Settings.getStringArray( "gemma.externalDatabases.featured" ) );
    }
}

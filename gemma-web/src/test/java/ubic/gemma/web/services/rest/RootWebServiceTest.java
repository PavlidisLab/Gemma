package ubic.gemma.web.services.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.util.BaseSpringWebTest;

import static org.assertj.core.api.Assertions.assertThat;

public class RootWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private RootWebService rootWebService;

    @Test
    public void test() {
        ResponseDataObject<RootWebService.ApiInfoValueObject> response = rootWebService.getApiInfo( new MockHttpServletResponse(), new MockServletConfig() );
        String expectedVersion = OpenApiUtils.getOpenApi( null ).getInfo().getVersion();
        assertThat( expectedVersion ).isNotBlank();
        assertThat( response.getData().getVersion() ).isEqualTo( expectedVersion );
    }
}

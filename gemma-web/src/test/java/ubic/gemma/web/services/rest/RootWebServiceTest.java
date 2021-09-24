package ubic.gemma.web.services.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.util.BaseSpringWebTest;

import static org.assertj.core.api.Assertions.assertThat;

public class RootWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private RootWebService rootWebService;

    @Test
    public void test() {
        ResponseDataObject<RootWebService.ApiInfoValueObject> response = rootWebService.all( new MockHttpServletResponse(), new MockServletConfig() );
        assertThat( response.getData().getVersion() ).isEqualTo( "2.3.4" );
    }
}

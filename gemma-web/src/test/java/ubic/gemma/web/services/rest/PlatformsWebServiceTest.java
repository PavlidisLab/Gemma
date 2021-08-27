package ubic.gemma.web.services.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.PlatformFilterArg;
import ubic.gemma.web.services.rest.util.args.PlatformArg;
import ubic.gemma.web.services.rest.util.args.SortArg;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.List;

public class PlatformsWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private PlatformsWebService platformsWebService;

    @Test
    public void testAll() {
        ResponseDataObject<List<ArrayDesignValueObject>> response = platformsWebService.all(
                PlatformFilterArg.valueOf( "" ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
    }

    @Test
    public void testPlatformDatasets() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = platformsWebService.platformDatasets(
                PlatformArg.valueOf( "1" ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "20" ),
                new MockHttpServletResponse() );
    }
}

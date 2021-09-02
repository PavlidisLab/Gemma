package ubic.gemma.web.services.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.PlatformArg;
import ubic.gemma.web.services.rest.util.args.PlatformFilterArg;
import ubic.gemma.web.services.rest.util.args.SortArg;
import ubic.gemma.web.util.BaseSpringWebTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PlatformsWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private PlatformsWebService platformsWebService;

    @Test
    public void testAll() {
        PaginatedResponseDataObject<ArrayDesignValueObject> response = platformsWebService.all(
                PlatformFilterArg.valueOf( "" ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }

    @Test
    public void testPlatformDatasets() {
        PaginatedResponseDataObject<ExpressionExperimentValueObject> response = platformsWebService.platformDatasets(
                PlatformArg.valueOf( "1" ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "20" ),
                new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }

    @Test
    public void testPlatformElements() {
        PaginatedResponseDataObject<CompositeSequenceValueObject> response = platformsWebService.platformElements(
                PlatformArg.valueOf( "1" ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "20" ),
                new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }
}

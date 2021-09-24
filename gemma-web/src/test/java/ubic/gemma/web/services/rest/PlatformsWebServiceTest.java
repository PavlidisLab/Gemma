package ubic.gemma.web.services.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;
import ubic.gemma.web.util.BaseSpringWebTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PlatformsWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private PlatformsWebService platformsWebService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    /* fixtures */
    private ExpressionExperiment expressionExperiment;
    private ArrayDesign arrayDesign;

    @Before
    public void setUp() {
        expressionExperiment = getTestPersistentBasicExpressionExperiment();
        arrayDesign = expressionExperiment.getBioAssays().iterator().next().getArrayDesignUsed();
    }

    @After
    public void tearDown() {
        eeService.remove( expressionExperiment );
        arrayDesignService.remove( arrayDesign );
    }

    @Test
    public void testAll() {
        PaginatedResponseDataObject<ArrayDesignValueObject> response = platformsWebService.all(
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }

    @Test
    public void testPlatformDatasets() {
        PaginatedResponseDataObject<ExpressionExperimentValueObject> response = platformsWebService.platformDatasets(
                PlatformArg.valueOf( this.arrayDesign.getId().toString() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
        assertThat( response.getData() ).asList()
                .hasSize( 1 )
                .first().hasFieldOrPropertyWithValue( "id", expressionExperiment.getId() );
    }

    @Test
    public void testPlatformElements() {
        PaginatedResponseDataObject<CompositeSequenceValueObject> response = platformsWebService.platformElements(
                PlatformArg.valueOf( this.arrayDesign.getId().toString() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }
}

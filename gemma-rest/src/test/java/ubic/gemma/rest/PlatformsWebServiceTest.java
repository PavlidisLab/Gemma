package ubic.gemma.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.BlacklistedPlatform;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.FilteringAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("web")
@WebAppConfiguration
public class PlatformsWebServiceTest extends BaseSpringContextTest {

    @Autowired
    private PlatformsWebService platformsWebService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BlacklistedEntityService blacklistedEntityService;

    /* fixtures */
    private ExpressionExperiment expressionExperiment;
    private ArrayDesign arrayDesign;

    @Before
    public void setUp() throws Exception {
        expressionExperiment = getTestPersistentBasicExpressionExperiment();
        arrayDesign = expressionExperiment.getBioAssays().iterator().next().getArrayDesignUsed();
    }

    @After
    public void tearDown() {
        eeService.remove( expressionExperiment );
        arrayDesignService.remove( arrayDesign );
        blacklistedEntityService.removeAllInBatch();
    }

    @Test
    public void testAll() {
        FilteringAndPaginatedResponseDataObject<ArrayDesignValueObject> response = platformsWebService.getPlatforms(
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ) );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }

    @Test
    public void testPlatformDatasets() {
        PaginatedResponseDataObject<ExpressionExperimentValueObject> response = platformsWebService.getPlatformDatasets(
                PlatformArg.valueOf( this.arrayDesign.getId().toString() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ) );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
        assertThat( response.getData() ).asList()
                .hasSize( 1 )
                .first().hasFieldOrPropertyWithValue( "id", expressionExperiment.getId() );
    }

    @Test
    public void testPlatformElements() {
        PaginatedResponseDataObject<CompositeSequenceValueObject> response = platformsWebService.getPlatformElements(
                PlatformArg.valueOf( this.arrayDesign.getId().toString() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ) );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }

    @Test
    public void testGetBlacklistedPlatforms() {
        BlacklistedPlatform bp = blacklistedEntityService.blacklistPlatform( arrayDesign, "This is just a test, don't feel bad about it." );
        assertThat( blacklistedEntityService.isBlacklisted( arrayDesign ) ).isTrue();
        assertThat( bp.getShortName() ).isEqualTo( arrayDesign.getShortName() );
        FilteringAndPaginatedResponseDataObject<ArrayDesignValueObject> payload = platformsWebService.getBlacklistedPlatforms( FilterArg.valueOf( "" ), SortArg.valueOf( "+id" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) );
        assertThat( payload.getData() )
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "shortName", arrayDesign.getShortName() );
    }

    @Test
    public void testGetBlacklistedPlatformsAsNonAdmin() {
        BlacklistedPlatform bp = blacklistedEntityService.blacklistPlatform( arrayDesign, "This is just a test, don't feel bad about it." );
        assertThat( blacklistedEntityService.isBlacklisted( arrayDesign ) ).isTrue();
        assertThat( bp.getShortName() ).isEqualTo( arrayDesign.getShortName() );
        try {
            runAsUser( "bob" );
            assertThatThrownBy( () -> platformsWebService.getBlacklistedPlatforms( FilterArg.valueOf( "" ), SortArg.valueOf( "+id" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) ) )
                    .isInstanceOf( AccessDeniedException.class );
        } finally {
            runAsAdmin();
        }
    }
}

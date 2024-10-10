package ubic.gemma.rest;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.blacklist.BlacklistedPlatform;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PlatformsWebServiceTest extends BaseJerseyIntegrationTest {

    @Autowired
    private PlatformsWebService platformsWebService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BlacklistedEntityService blacklistedEntityService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    /* fixtures */
    private ExpressionExperiment expressionExperiment;
    private ArrayDesign arrayDesign;

    @Before
    public void setUpMocks() {
        expressionExperiment = testHelper.getTestPersistentBasicExpressionExperiment();
        arrayDesign = expressionExperiment.getBioAssays().iterator().next().getArrayDesignUsed();
    }

    @After
    public void removeFixtures() {
        eeService.remove( expressionExperiment );
        arrayDesignService.remove( arrayDesign );
        blacklistedEntityService.removeAll();
    }

    @Test
    public void testAll() {
        FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> response = platformsWebService.getPlatforms(
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
        assertThat( response.getData() )
                .asInstanceOf( InstanceOfAssertFactories.LIST )
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
        FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> payload = platformsWebService.getBlacklistedPlatforms( FilterArg.valueOf( "" ), SortArg.valueOf( "+id" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) );
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
            testAuthenticationUtils.runAsUser( "bob", true );
            assertThatThrownBy( () -> platformsWebService.getBlacklistedPlatforms( FilterArg.valueOf( "" ), SortArg.valueOf( "+id" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) ) )
                    .isInstanceOf( AccessDeniedException.class );
        } finally {
            testAuthenticationUtils.runAsAdmin();
        }
    }
}

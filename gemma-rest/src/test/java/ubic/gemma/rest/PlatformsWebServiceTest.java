package ubic.gemma.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.model.blacklist.BlacklistedPlatform;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PlatformsWebServiceTest extends BaseJerseyIntegrationTest5 {

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

    @BeforeEach
    public void setUpMocks() {
        expressionExperiment = testHelper.getTestPersistentBasicExpressionExperiment();
        arrayDesign = expressionExperiment.getBioAssays().iterator().next().getArrayDesignUsed();
    }

    @AfterEach
    public void removeFixtures() {
        eeService.remove( expressionExperiment );
        arrayDesignService.remove( arrayDesign );
        blacklistedEntityService.removeAll();
    }

    @Test
    public void testAll() {
        Object response = platformsWebService.getPlatforms(
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ),
                null /* cursor */ );
        assertThat( response )
                .isInstanceOf( FilteredAndPaginatedResponseDataObject.class )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }

    @Test
    public void testPlatformDatasets() {
        Object response = platformsWebService.getPlatformDatasets(
                PlatformArg.valueOf( this.arrayDesign.getId().toString() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                null /* cursor */ );
        assertThat( response )
                .isInstanceOf( PaginatedResponseDataObject.class )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( PaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() )
                .singleElement()
                .hasFieldOrPropertyWithValue( "id", expressionExperiment.getId() );
    }

    @Test
    public void testPlatformElements() {
        Object response = platformsWebService.getPlatformElements(
                PlatformArg.valueOf( this.arrayDesign.getId().toString() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                null /* cursor */,
                false /* withSequence */ );
        assertThat( response )
                .isInstanceOf( PaginatedResponseDataObject.class )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 );
    }

    /**
     * Verifies the {@code withSequence=true} opt-in populates {@code sequence}
     * on every returned probe VO (the helper seeds 40-char ATCG strings via
     * {@link PersistentDummyObjectHelper#getTestNonPersistentBioSequence}).
     * Asserts only on {@code sequence}, not {@code sequenceLength}: the helper
     * doesn't call {@code BioSequence.setLength()} on seeded probes so length
     * is legitimately null in the fixture — both fields are nullable on the VO
     * by design.
     */
    @Test
    public void testPlatformElementsWithSequence() {
        ArrayDesign adWithProbes = testHelper.getTestPersistentArrayDesign( 3, true, true );
        try {
            Object response = platformsWebService.getPlatformElements(
                    PlatformArg.valueOf( adWithProbes.getId().toString() ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "20" ),
                    null /* cursor */,
                    true /* withSequence */ );
            assertThat( response ).isInstanceOf( PaginatedResponseDataObject.class );
            @SuppressWarnings("unchecked")
            PaginatedResponseDataObject<ubic.gemma.model.expression.designElement.CompositeSequenceValueObject> page =
                    ( PaginatedResponseDataObject<ubic.gemma.model.expression.designElement.CompositeSequenceValueObject> ) response;
            assertThat( page.getData() ).hasSize( 3 )
                    .allSatisfy( vo ->
                            assertThat( vo.getSequence() ).isNotBlank().matches( "[ATCG]+" ) );
        } finally {
            arrayDesignService.remove( adWithProbes );
        }
    }

    @Test
    public void testGetBlacklistedPlatforms() {
        BlacklistedPlatform bp = blacklistedEntityService.blacklistPlatform( arrayDesign, "This is just a test, don't feel bad about it." );
        assertThat( blacklistedEntityService.isBlacklisted( arrayDesign ) ).isTrue();
        assertThat( bp.getShortName() ).isEqualTo( arrayDesign.getShortName() );
        Object responseObj = platformsWebService.getBlacklistedPlatforms( FilterArg.valueOf( "" ), SortArg.valueOf( "+id" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ), null /* cursor */ );
        assertThat( responseObj ).isInstanceOf( FilteredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> payload = ( FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> ) responseObj;
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
            assertThatThrownBy( () -> platformsWebService.getBlacklistedPlatforms( FilterArg.valueOf( "" ), SortArg.valueOf( "+id" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ), null /* cursor */ ) )
                    .isInstanceOf( AccessDeniedException.class );
        } finally {
            testAuthenticationUtils.runAsAdmin();
        }
    }
}

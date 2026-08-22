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
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
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
                null /* cursor */, false );
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
                false /* withSequence */,
                false /* withGenes */,
                null /* gene */,
                FilterArg.valueOf( "" ) );
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
    /**
     * Ask 3: a mergee must be able to NAME its merge target. {@code isMergee} said only THAT a
     * merge happened, and {@code mergees.id} is not a filterable property, so from the mergee's
     * side — the side a visitor stands on — the target was unreachable.
     */
    @Test
    public void testPlatformVoNamesItsMergeTargetAndItsMergees() {
        ArrayDesign target = testHelper.getTestPersistentArrayDesign( 1, true, true );
        ArrayDesign mergee = testHelper.getTestPersistentArrayDesign( 1, true, true );
        try {
            mergee.setMergedInto( target );
            arrayDesignService.update( mergee );

            Object mergeeResponse = platformsWebService.getPlatformsByIds(
                    PlatformArrayArg.valueOf( mergee.getId().toString() ),
                    FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ),
                    SortArg.valueOf( "+id" ), null /* cursor */, false /* withGeneCounts */ );
            @SuppressWarnings("unchecked")
            FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> mergeePage =
                    ( FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> ) mergeeResponse;
            ArrayDesignValueObject mergeeVo = mergeePage.getData().iterator().next();
            assertThat( mergeeVo.getIsMergee() ).isTrue();
            assertThat( mergeeVo.getMergedInto() ).isNotNull()
                    .hasFieldOrPropertyWithValue( "id", target.getId() )
                    .hasFieldOrPropertyWithValue( "shortName", target.getShortName() );

            // ...and the target names its mergees from the other side.
            Object targetResponse = platformsWebService.getPlatformsByIds(
                    PlatformArrayArg.valueOf( target.getId().toString() ),
                    FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ),
                    SortArg.valueOf( "+id" ), null /* cursor */, false /* withGeneCounts */ );
            @SuppressWarnings("unchecked")
            FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> targetPage =
                    ( FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> ) targetResponse;
            ArrayDesignValueObject targetVo = targetPage.getData().iterator().next();
            assertThat( targetVo.getMergedInto() ).isNull();
            assertThat( targetVo.getMergees() )
                    .singleElement()
                    .hasFieldOrPropertyWithValue( "id", mergee.getId() );
        } finally {
            mergee.setMergedInto( null );
            arrayDesignService.update( mergee );
            arrayDesignService.remove( mergee );
            arrayDesignService.remove( target );
        }
    }

    /**
     * Ask 4, microarray case. The counts come from the on-disk report, which is written by the
     * pipeline / the monthly job — never computed per request, because counting distinct genes for
     * one large platform measured ~1.7s against production. With no report on disk the fields stay
     * NULL rather than 0: "not computed yet" and "maps to no genes" are different claims, and
     * falling back to the slow query is exactly what this design refuses to do.
     */
    @Test
    public void testMicroarrayGeneCountsAreNullWithoutAReport() {
        // getTestPersistentArrayDesign seeds ONECOLOR, i.e. a microarray.
        ArrayDesign ad = testHelper.getTestPersistentArrayDesign( 2, true, true );
        try {
            ArrayDesignValueObject without = firstPlatformVo( ad, false );
            assertThat( without.getNumberOfGenes() ).isNull();
            assertThat( without.getNumberOfMappedElements() ).isNull();

            ArrayDesignValueObject with = firstPlatformVo( ad, true );
            assertThat( with.getNumberOfGenes() ).isNull();
            assertThat( with.getNumberOfMappedElements() ).isNull();
            assertThat( with.getGeneCountsLastUpdated() ).isNull();
        } finally {
            arrayDesignService.remove( ad );
        }
    }

    /**
     * Ask 4, RNA-seq case. A gene-list platform's elements ARE genes, so both counts equal the
     * element count and neither needs a report — which is what makes this path work on a deployment
     * where no reports have been generated yet.
     */
    @Test
    public void testGeneListPlatformGeneCountsComeFromTheElementCount() {
        ArrayDesign ad = testHelper.getTestPersistentArrayDesign( 3, true, true );
        try {
            ad.setTechnologyType( TechnologyType.GENELIST );
            arrayDesignService.update( ad );

            ArrayDesignValueObject with = firstPlatformVo( ad, true );
            assertThat( with.getNumberOfGenes() ).isEqualTo( 3L );
            assertThat( with.getNumberOfMappedElements() ).isEqualTo( 3L );
            // Derived live, so there is no report timestamp to report.
            assertThat( with.getGeneCountsLastUpdated() ).isNull();

            // Still gated behind the opt-in.
            assertThat( firstPlatformVo( ad, false ).getNumberOfGenes() ).isNull();
        } finally {
            arrayDesignService.remove( ad );
        }
    }

    private ArrayDesignValueObject firstPlatformVo( ArrayDesign ad, boolean withGeneCounts ) {
        Object response = platformsWebService.getPlatformsByIds(
                PlatformArrayArg.valueOf( ad.getId().toString() ),
                FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ), null /* cursor */, withGeneCounts );
        @SuppressWarnings("unchecked")
        FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> page =
                ( FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> ) response;
        return page.getData().iterator().next();
    }

    /**
     * Regression guard for the {@code {probes}} restriction being dropped.
     * <p>
     * {@link ubic.gemma.rest.util.args.CompositeSequenceArrayArg#getPlatformFilter()} encodes only
     * {@code arrayDesign.id = ?}; until 2026-08-22 the endpoint composed that alone, so asking for
     * ONE probe by name returned the platform's first page instead. The bug was invisible to the
     * existing tests because they all mock the arg-service — this one goes through real
     * persistence and asserts on identity, which is the only thing that can catch it.
     */
    @Test
    public void testPlatformElementRestrictsToTheRequestedProbe() {
        ArrayDesign adWithProbes = testHelper.getTestPersistentArrayDesign( 3, true, true );
        try {
            CompositeSequence wanted = adWithProbes.getCompositeSequences().iterator().next();
            Object response = platformsWebService.getPlatformElement(
                    PlatformArg.valueOf( adWithProbes.getId().toString() ),
                    CompositeSequenceArrayArg.valueOf( wanted.getName() ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "20" ),
                    null /* cursor */,
                    false /* withSequence */,
                    false /* withGenes */ );
            @SuppressWarnings("unchecked")
            FilteredAndPaginatedResponseDataObject<CompositeSequenceValueObject> page =
                    ( FilteredAndPaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
            assertThat( page.getData() )
                    .singleElement()
                    .hasFieldOrPropertyWithValue( "id", wanted.getId() );
        } finally {
            arrayDesignService.remove( adWithProbes );
        }
    }

    /**
     * {@code withGenes=true} must set the field on every row, and to an EMPTY list rather than null
     * for a probe with no gene mapping — otherwise {@code NON_NULL} elides it and the client cannot
     * tell "maps to nothing" from "not requested". The seeded fixture has no GENE2CS rows, so this
     * pins the empty-not-null half specifically.
     */
    @Test
    public void testPlatformElementsWithGenesYieldsEmptyListNotNull() {
        ArrayDesign adWithProbes = testHelper.getTestPersistentArrayDesign( 3, true, true );
        try {
            Object response = platformsWebService.getPlatformElements(
                    PlatformArg.valueOf( adWithProbes.getId().toString() ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "20" ),
                    null /* cursor */,
                    false /* withSequence */,
                    true /* withGenes */,
                    null /* gene */,
                    FilterArg.valueOf( "" ) );
            @SuppressWarnings("unchecked")
            PaginatedResponseDataObject<CompositeSequenceValueObject> page =
                    ( PaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
            assertThat( page.getData() ).hasSize( 3 )
                    .allSatisfy( vo -> assertThat( vo.getGenes() ).isNotNull().isEmpty() );
        } finally {
            arrayDesignService.remove( adWithProbes );
        }
    }

    /**
     * Without the opt-in the field stays null, so it elides from the wire entirely — the existing
     * default response shape is unchanged for callers that never ask.
     */
    @Test
    public void testPlatformElementsWithoutGenesLeavesFieldNull() {
        ArrayDesign adWithProbes = testHelper.getTestPersistentArrayDesign( 3, true, true );
        try {
            Object response = platformsWebService.getPlatformElements(
                    PlatformArg.valueOf( adWithProbes.getId().toString() ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "20" ),
                    null /* cursor */,
                    false /* withSequence */,
                    false /* withGenes */,
                    null /* gene */,
                    FilterArg.valueOf( "" ) );
            @SuppressWarnings("unchecked")
            PaginatedResponseDataObject<CompositeSequenceValueObject> page =
                    ( PaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
            assertThat( page.getData() ).hasSize( 3 )
                    .allSatisfy( vo -> assertThat( vo.getGenes() ).isNull() );
        } finally {
            arrayDesignService.remove( adWithProbes );
        }
    }

    /**
     * A user {@code filter=} must narrow the listing. Pinned because the endpoint accepted no
     * filter at all until 2026-08-22 — an unknown query param is silently ignored by JAX-RS, so
     * the UI's search box appeared to run and matched everything.
     */
    @Test
    public void testPlatformElementsHonoursNameFilter() {
        ArrayDesign adWithProbes = testHelper.getTestPersistentArrayDesign( 3, true, true );
        try {
            CompositeSequence wanted = adWithProbes.getCompositeSequences().iterator().next();
            Object response = platformsWebService.getPlatformElements(
                    PlatformArg.valueOf( adWithProbes.getId().toString() ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "20" ),
                    null /* cursor */,
                    false /* withSequence */,
                    false /* withGenes */,
                    null /* gene */,
                    FilterArg.valueOf( "name = " + wanted.getName() ) );
            @SuppressWarnings("unchecked")
            PaginatedResponseDataObject<CompositeSequenceValueObject> page =
                    ( PaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
            assertThat( page.getData() )
                    .singleElement()
                    .hasFieldOrPropertyWithValue( "id", wanted.getId() );
        } finally {
            arrayDesignService.remove( adWithProbes );
        }
    }

    @Test
    public void testPlatformElementsWithSequence() {
        ArrayDesign adWithProbes = testHelper.getTestPersistentArrayDesign( 3, true, true );
        try {
            Object response = platformsWebService.getPlatformElements(
                    PlatformArg.valueOf( adWithProbes.getId().toString() ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "20" ),
                    null /* cursor */,
                    true /* withSequence */,
                    false /* withGenes */,
                    null /* gene */,
                    FilterArg.valueOf( "" ) );
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

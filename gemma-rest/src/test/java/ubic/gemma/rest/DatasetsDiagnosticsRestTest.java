package ubic.gemma.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;
import ubic.gemma.rest.util.args.DatasetArg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * Integration tests for the diagnostics endpoints (mean-variance, sample-correlation, svd) on
 * {@link DatasetsWebService}. Each test exercises the real Hibernate session lifecycle that the
 * mock-based {@code DatasetsWebServiceTest} cannot reproduce.
 * <p>
 * Regression target #1: the mean-variance handler previously called
 * {@code ee.getMeanVarianceRelation().getMeans()} on the entity returned by
 * {@code DatasetArgService.getEntity()}, which carries a lazy MVR proxy. Outside the service-bounded
 * Hibernate session this threw {@code LazyInitializationException} (HTTP 500). The fix in commit
 * {@code a70e3dc8f6} switched to {@code expressionExperimentService.loadWithMeanVarianceRelation}.
 * Builds before that commit fail {@link #testGetDatasetMeanVarianceReturnsArrays()} with a 500.
 * <p>
 * Regression target #2: the {@link ubic.gemma.core.analysis.preprocess.svd.SVDResult} constructor
 * previously assigned {@code pca.getExperimentAnalyzed()} (returns a {@code BioAssaySet}-typed
 * Hibernate proxy) directly to its {@code ExpressionExperiment} field. The implicit checkcast
 * inserted by javac threw {@code ClassCastException}, caught + logged message-only by
 * {@code SVDServiceImpl.getSvd}, which returned {@code null} silently. The fix in
 * {@code dd63676978} calls {@link Hibernate#unproxy(Object)} in the constructor and switched the
 * catch-block to log the full stack. {@link #getDatasetSvd_returns200_andExperimentAnalyzedSurfacesUnderlyingEntity()}
 * locks this in: pre-fix, the call returned 404 instead of 200.
 */
@Tag("integration")
public class DatasetsDiagnosticsRestTest extends BaseJerseyIntegrationTest5 {

    @Autowired
    private DatasetsWebService datasetsWebService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    private ExpressionExperiment eeWithMvr;
    private ExpressionExperiment eeWithoutMvr;
    private ExpressionExperiment eeWithSvd;
    private ExpressionExperiment eeWithoutSvd;

    @BeforeEach
    public void setUpFixtures() {
        testHelper.resetSeed();
        eeWithMvr = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        eeWithoutMvr = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        eeWithSvd = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        eeWithoutSvd = testHelper.getTestExpressionExperimentWithAllDependencies( false );

        // Build the MVR fixture by hand and persist via the EE service. Fast: no preprocess pipeline.
        double[] means = { 1.0, 2.0, 3.0, 4.0 };
        double[] variances = { 0.1, 0.4, 0.9, 1.6 };
        MeanVarianceRelation mvr = MeanVarianceRelation.Factory.newInstance( means, variances );
        expressionExperimentService.updateMeanVarianceRelation( eeWithMvr, mvr );

        // Hand-build a PrincipalComponentAnalysis for eeWithSvd. The svd() method runs the full
        // preprocess pipeline — too slow for the unit-test bar. principalComponentAnalysisService
        // .create(ee, u, eigenvalues, v, bad, numComponentsToStore, numLoadingsToStore) persists
        // the PCA, eigenvalues, eigenvectors, and probeLoadings directly.
        persistSyntheticPcaFor( eeWithSvd );
    }

    /**
     * Build a minimal synthetic PCA for {@code ee} and persist it. Picks the BioAssays from the
     * EE's first ArrayDesign so the BAD's row count matches the V-matrix row count.
     */
    private void persistSyntheticPcaFor( ExpressionExperiment ee ) {
        ExpressionExperiment thawed = expressionExperimentService.thaw( ee );

        // Pick the bioAssays that share a single platform so the BAD is well-formed.
        Map<Long, List<BioAssay>> byPlatform = new HashMap<>();
        for ( BioAssay ba : thawed.getBioAssays() ) {
            Long adId = ba.getArrayDesignUsed().getId();
            byPlatform.computeIfAbsent( adId, k -> new ArrayList<>() ).add( ba );
        }
        List<BioAssay> assays = byPlatform.values().iterator().next();

        BioAssayDimension bad = BioAssayDimension.Factory.newInstance( assays );
        bad = bioAssayDimensionService.findOrCreate( bad );

        // Pull two CompositeSequences off the bioAssays' shared ArrayDesign for the U matrix rows.
        // getCompositeSequences runs in its own @Transactional read so the lazy collection on
        // the bioAssay-side ArrayDesign proxy doesn't have to be initialized in this thread.
        List<CompositeSequence> probes = new ArrayList<>(
                arrayDesignService.getCompositeSequences( assays.get( 0 ).getArrayDesignUsed() ) );
        // Take the first two — synthetic numbers, just need referential integrity.
        List<CompositeSequence> uRows = probes.subList( 0, Math.min( 2, probes.size() ) );

        // U: probes × components (2 × 2)
        DoubleMatrix<CompositeSequence, Integer> u = new DenseDoubleMatrix<>( uRows.size(), 2 );
        u.setRowNames( uRows );
        List<Integer> uCols = new ArrayList<>();
        uCols.add( 0 );
        uCols.add( 1 );
        u.setColumnNames( uCols );
        for ( int i = 0; i < uRows.size(); i++ ) {
            u.set( i, 0, 0.7 - 0.1 * i );
            u.set( i, 1, -0.4 + 0.1 * i );
        }

        // V: components × biomaterials (assays.size() × 2)
        List<BioMaterial> bioMaterials = new ArrayList<>();
        for ( BioAssay ba : assays ) {
            bioMaterials.add( ba.getSampleUsed() );
        }
        DoubleMatrix<Integer, BioMaterial> v = new DenseDoubleMatrix<>( bioMaterials.size(), 2 );
        List<Integer> vRows = new ArrayList<>();
        for ( int i = 0; i < bioMaterials.size(); i++ ) {
            vRows.add( i );
        }
        v.setRowNames( vRows );
        v.setColumnNames( bioMaterials );
        for ( int i = 0; i < bioMaterials.size(); i++ ) {
            v.set( i, 0, 0.1 * ( i + 1 ) );
            v.set( i, 1, 0.05 * ( i + 1 ) );
        }

        double[] eigenvalues = { 0.5, 0.3 };
        principalComponentAnalysisService.create( thawed, u, eigenvalues, v, bad, 2, 5 );
    }

    @AfterEach
    public void tearDownFixtures() {
        if ( eeWithSvd != null ) {
            try {
                principalComponentAnalysisService.removeForExperiment( eeWithSvd );
            } catch ( Exception ignored ) {
                // best-effort
            }
            expressionExperimentService.remove( eeWithSvd );
        }
        if ( eeWithoutSvd != null ) {
            expressionExperimentService.remove( eeWithoutSvd );
        }
        if ( eeWithMvr != null ) {
            expressionExperimentService.remove( eeWithMvr );
        }
        if ( eeWithoutMvr != null ) {
            expressionExperimentService.remove( eeWithoutMvr );
        }
    }

    /**
     * Regression guard: calling the handler must not throw {@code LazyInitializationException}.
     * Pre-{@code a70e3dc8f6} this returned HTTP 500.
     */
    @Test
    public void testGetDatasetMeanVarianceReturnsArrays() {
        DatasetsWebService.MeanVarianceValueObject vo = datasetsWebService
                .getDatasetMeanVariance( DatasetArg.valueOf( eeWithMvr.getId().toString() ) )
                .getData();

        assertThat( vo ).isNotNull();
        assertThat( vo.getMeans() )
                .isNotNull()
                .isNotEmpty()
                .doesNotContain( Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY )
                .containsExactly( 1.0, 2.0, 3.0, 4.0 );
        assertThat( vo.getVariances() )
                .isNotNull()
                .isNotEmpty()
                .hasSameSizeAs( vo.getMeans() )
                .doesNotContain( Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY )
                .containsExactly( 0.1, 0.4, 0.9, 1.6 );
    }

    /**
     * Wire-shape check: the HTTP response is 200 with a JSON body containing the means array.
     * Complements the direct-handler call above by exercising the Jersey serialization path.
     */
    @Test
    public void testGetDatasetMeanVarianceHttp200() {
        assertThat( target( "/datasets/" + eeWithMvr.getId() + "/mean-variance" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .hasFieldOrProperty( "data.means" )
                .hasFieldOrProperty( "data.variances" );
    }

    /**
     * Negative case: a dataset without an MVR returns 404 (NotFoundException from the handler).
     */
    @Test
    public void testGetDatasetMeanVarianceWhenNoneIs404() {
        NotFoundException nfe = catchThrowableOfType( NotFoundException.class,
                () -> datasetsWebService.getDatasetMeanVariance(
                        DatasetArg.valueOf( eeWithoutMvr.getId().toString() ) ) );
        assertThat( nfe ).isNotNull();

        // Wire-level confirmation.
        assertThat( target( "/datasets/" + eeWithoutMvr.getId() + "/mean-variance" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    @Test
    public void testGetDatasetMeanVarianceWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/9999999/mean-variance" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    // ---------------------------------------------------------------------
    // SVD endpoint regression tests (dd63676978: Hibernate.unproxy fix in
    // SVDResult constructor + stack-logging in SVDServiceImpl.getSvd).
    // ---------------------------------------------------------------------

    /**
     * Regression guard for {@code dd63676978}. Pre-fix, the SVDResult constructor's implicit
     * checkcast on {@code pca.getExperimentAnalyzed()} threw {@code ClassCastException} (because
     * the generic-erased Hibernate proxy is typed {@code BioAssaySet}, not
     * {@code ExpressionExperiment}). {@code SVDServiceImpl.getSvd} caught it message-only and
     * returned null, surfacing the call as a 404 on /svd. Post-fix:
     * <ul>
     *     <li>{@code Hibernate.unproxy(...)} lets the underlying EE surface,</li>
     *     <li>the catch logs the full stack (any future CCE-style regression is visible).</li>
     * </ul>
     * The assertion bar here is "no throw, HTTP 200, arrays non-empty" — that proves the
     * constructor succeeded against a real proxied EE.
     */
    @Test
    public void getDatasetSvd_returns200_andExperimentAnalyzedSurfacesUnderlyingEntity() {
        DatasetsWebService.SimpleSVDValueObject vo = datasetsWebService
                .getDatasetSvd( DatasetArg.valueOf( eeWithSvd.getId().toString() ) )
                .getData();

        assertThat( vo ).isNotNull();
        assertThat( vo.getBioAssayIds() ).isNotNull().isNotEmpty();
        assertThat( vo.getBioMaterialIds() ).isNotNull().isNotEmpty();
        assertThat( vo.getVariances() ).isNotNull();
        assertThat( vo.getVMatrix() ).isNotNull();

        // Wire-level confirmation: 200 with the expected JSON envelope.
        assertThat( target( "/datasets/" + eeWithSvd.getId() + "/svd" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .hasFieldOrProperty( "data.bioAssayIds" )
                .hasFieldOrProperty( "data.bioMaterialIds" );
    }

    /**
     * Companion: /svd/loadings on a valid PCA should return 200 with at most {@code top} rows
     * and the requested PC. Pre-fix, this surfaced as 500 because {@code getSvd()} returned
     * null after the SVDResult-ctor CCE while {@code hasSvd()} was still true.
     */
    @Test
    public void getDatasetSvdLoadings_returns200_forValidPcAndTop() {
        DatasetsWebService.PcLoadingsValueObject vo = datasetsWebService
                .getDatasetSvdLoadings( DatasetArg.valueOf( eeWithSvd.getId().toString() ),
                        1, 5, DatasetsWebService.PcLoadingDirection.both )
                .getData();

        assertThat( vo ).isNotNull();
        assertThat( vo.getPc() ).isEqualTo( 1 );
        assertThat( vo.getRows() ).isNotNull();
        assertThat( vo.getRows().size() ).isLessThanOrEqualTo( 5 );
        assertThat( vo.getBioAssayScores() ).isNotNull();
    }

    /**
     * Negative case: an EE without a PCA → 404. Locks in the
     * "{@code throw new NotFoundException}" branch in {@code getDatasetSvd}.
     */
    @Test
    public void getDatasetSvd_returns404_whenNoSvd() {
        NotFoundException nfe = catchThrowableOfType( NotFoundException.class,
                () -> datasetsWebService.getDatasetSvd(
                        DatasetArg.valueOf( eeWithoutSvd.getId().toString() ) ) );
        assertThat( nfe ).isNotNull();

        assertThat( target( "/datasets/" + eeWithoutSvd.getId() + "/svd" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    /**
     * Negative case: /svd/loadings on an EE without a PCA → 404. The
     * {@code hasSvd()} short-circuit covers this; we also exercise the wire path.
     */
    @Test
    public void getDatasetSvdLoadings_returns404_whenNoSvd() {
        NotFoundException nfe = catchThrowableOfType( NotFoundException.class,
                () -> datasetsWebService.getDatasetSvdLoadings(
                        DatasetArg.valueOf( eeWithoutSvd.getId().toString() ),
                        1, 5, DatasetsWebService.PcLoadingDirection.both ) );
        assertThat( nfe ).isNotNull();

        assertThat( target( "/datasets/" + eeWithoutSvd.getId() + "/svd/loadings?pc=1&top=5" )
                .request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    // -------------------------------------------------------------------------
    // sample-correlation + outlier endpoints
    //
    // The full SampleCoexpressionAnalysis fixture (matrix + actualOutlier /
    // predictedOutlier list assertions) is omitted: hand-building one requires
    // running the processed-vector pipeline + SampleCoexpressionAnalysisService
    // compute(), which the existing service-level test
    // (SampleCoexpressionAnalysisServiceTest) gates behind @Tag("slow"). That
    // doesn't fit the "each new @Test <2s" bar here. The GET tests covered
    // below are limited to the 404-no-analysis path; the outlier-list shape +
    // unmasked-matrix payload is covered transitively by the value-object
    // constructor unit assertions and the slow service-level integration test.
    // -------------------------------------------------------------------------

    /**
     * GET /sample-correlation returns 404 when the dataset has no
     * {@code SampleCoexpressionAnalysis}. Cheap fixture (no SCA computed).
     */
    @Test
    public void testGetDatasetSampleCorrelationWhenNoneIs404() {
        NotFoundException nfe = catchThrowableOfType( NotFoundException.class,
                () -> datasetsWebService.getDatasetSampleCorrelation(
                        DatasetArg.valueOf( eeWithoutMvr.getId().toString() ),
                        DatasetsWebService.CorrelationMatrixChoice.best ) );
        assertThat( nfe ).isNotNull();
        assertThat( target( "/datasets/" + eeWithoutMvr.getId() + "/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    /**
     * PUT /samples/{bioAssayId}/outlier flips {@code isOutlier=true} on the targeted
     * BioAssay. Re-load through the BioAssayService to confirm the persisted state.
     */
    @Test
    public void testMarkSampleOutlierViaPutEndpointFlipsIsOutlier() {
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( eeWithMvr );
        BioAssay target = thawed.getBioAssays().iterator().next();
        Long bioAssayId = target.getId();
        assertThat( target.getIsOutlier() ).isFalse();

        DatasetsWebService.SampleOutlierRequest body = new DatasetsWebService.SampleOutlierRequest();
        body.setOutlier( true );
        datasetsWebService.markDatasetSampleOutlier(
                DatasetArg.valueOf( eeWithMvr.getId().toString() ), bioAssayId, body );

        BioAssay reloaded = bioAssayService.loadOrFail( bioAssayId );
        assertThat( reloaded.getIsOutlier() ).isTrue();
    }

    /**
     * POST /samples/outliers atomically marks one assay and unmarks another. The
     * response carries the post-state full outlier set; the marked id is present,
     * the unmarked id is absent.
     */
    @Test
    public void testBatchMarkOutliersMarksAndUnmarksAtomically() {
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( eeWithMvr );
        List<BioAssay> assays = new java.util.ArrayList<>( thawed.getBioAssays() );
        assertThat( assays.size() ).isGreaterThanOrEqualTo( 2 );
        BioAssay toMark = assays.get( 0 );
        BioAssay toUnmark = assays.get( 1 );

        // Seed the unmark target as already-outlier so the unmark side has work to do.
        DatasetsWebService.SampleOutlierRequest seed = new DatasetsWebService.SampleOutlierRequest();
        seed.setOutlier( true );
        datasetsWebService.markDatasetSampleOutlier(
                DatasetArg.valueOf( eeWithMvr.getId().toString() ), toUnmark.getId(), seed );

        DatasetsWebService.BatchOutlierRequest body = new DatasetsWebService.BatchOutlierRequest();
        body.mark = Collections.singletonList( toMark.getId() );
        body.unmark = Collections.singletonList( toUnmark.getId() );
        DatasetsWebService.BatchOutlierResponse out = datasetsWebService.batchMarkSampleOutliers(
                DatasetArg.valueOf( eeWithMvr.getId().toString() ), body ).getData();

        assertThat( out.outlierBioAssayIds ).contains( toMark.getId() );
        assertThat( out.outlierBioAssayIds ).doesNotContain( toUnmark.getId() );
        assertThat( out.markedCount ).isEqualTo( 1 );
        assertThat( out.unmarkedCount ).isEqualTo( 1 );
    }

    /**
     * POST /samples/outliers must reject ids that don't belong to the path-derived
     * dataset with HTTP 400, AND must not mutate any state. Validation runs before
     * any service call so the EE's outlier set is unchanged after the call.
     */
    @Test
    public void testBatchMarkOutliersReturns400WhenIdDoesntBelongToDataset() {
        ExpressionExperiment thawedBefore = expressionExperimentService.thawBioAssays( eeWithMvr );
        long[] outlierIdsBefore = thawedBefore.getBioAssays().stream()
                .filter( BioAssay::getIsOutlier )
                .mapToLong( BioAssay::getId )
                .sorted()
                .toArray();
        // pick a BioAssay from a DIFFERENT EE — guaranteed not to belong here.
        ExpressionExperiment otherThawed = expressionExperimentService.thawBioAssays( eeWithoutMvr );
        BioAssay foreignAssay = otherThawed.getBioAssays().iterator().next();

        DatasetsWebService.BatchOutlierRequest body = new DatasetsWebService.BatchOutlierRequest();
        body.mark = Collections.singletonList( foreignAssay.getId() );

        BadRequestException bre = catchThrowableOfType( BadRequestException.class,
                () -> datasetsWebService.batchMarkSampleOutliers(
                        DatasetArg.valueOf( eeWithMvr.getId().toString() ), body ) );
        assertThat( bre ).isNotNull();

        // Critical: NO mutation. Outlier set is unchanged.
        ExpressionExperiment thawedAfter = expressionExperimentService.thawBioAssays( eeWithMvr );
        long[] outlierIdsAfter = thawedAfter.getBioAssays().stream()
                .filter( BioAssay::getIsOutlier )
                .mapToLong( BioAssay::getId )
                .sorted()
                .toArray();
        assertThat( outlierIdsAfter ).containsExactly( Arrays.stream( outlierIdsBefore ).boxed()
                .mapToLong( Long::longValue ).toArray() );
    }

    /**
     * POST /samples/outliers rejects an id that appears in BOTH {@code mark} and
     * {@code unmark} (ambiguous caller-side delta) with HTTP 400.
     */
    @Test
    public void testBatchMarkOutliersReturns400WhenIdInBothMarkAndUnmark() {
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( eeWithMvr );
        BioAssay any = thawed.getBioAssays().iterator().next();

        DatasetsWebService.BatchOutlierRequest body = new DatasetsWebService.BatchOutlierRequest();
        body.mark = Collections.singletonList( any.getId() );
        body.unmark = Collections.singletonList( any.getId() );

        BadRequestException bre = catchThrowableOfType( BadRequestException.class,
                () -> datasetsWebService.batchMarkSampleOutliers(
                        DatasetArg.valueOf( eeWithMvr.getId().toString() ), body ) );
        assertThat( bre ).isNotNull();
    }
}

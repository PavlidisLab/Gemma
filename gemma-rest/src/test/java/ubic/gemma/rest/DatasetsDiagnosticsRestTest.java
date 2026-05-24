package ubic.gemma.rest;

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
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;
import ubic.gemma.rest.util.args.DatasetArg;

import java.util.ArrayList;
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
        List<CompositeSequence> probes = new ArrayList<>(
                assays.get( 0 ).getArrayDesignUsed().getCompositeSequences() );
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
}

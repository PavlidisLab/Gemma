package ubic.gemma.rest;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;
import ubic.gemma.rest.util.args.DatasetArg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * Integration tests for the diagnostics endpoints (mean-variance, sample-correlation, svd) on
 * {@link DatasetsWebService}. Each test exercises the real Hibernate session lifecycle that the
 * mock-based {@code DatasetsWebServiceTest} cannot reproduce.
 * <p>
 * Regression target: the mean-variance handler previously called
 * {@code ee.getMeanVarianceRelation().getMeans()} on the entity returned by
 * {@code DatasetArgService.getEntity()}, which carries a lazy MVR proxy. Outside the service-bounded
 * Hibernate session this threw {@code LazyInitializationException} (HTTP 500). The fix in commit
 * {@code a70e3dc8f6} switched to {@code expressionExperimentService.loadWithMeanVarianceRelation}.
 * Builds before that commit fail {@link #testGetDatasetMeanVarianceReturnsArrays()} with a 500.
 */
@Tag("integration")
public class DatasetsDiagnosticsRestTest extends BaseJerseyIntegrationTest5 {

    @Autowired
    private DatasetsWebService datasetsWebService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    private ExpressionExperiment eeWithMvr;
    private ExpressionExperiment eeWithoutMvr;

    @BeforeEach
    public void setUpFixtures() {
        testHelper.resetSeed();
        eeWithMvr = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        eeWithoutMvr = testHelper.getTestExpressionExperimentWithAllDependencies( false );

        // Build the MVR fixture by hand and persist via the EE service. Fast: no preprocess pipeline.
        double[] means = { 1.0, 2.0, 3.0, 4.0 };
        double[] variances = { 0.1, 0.4, 0.9, 1.6 };
        MeanVarianceRelation mvr = MeanVarianceRelation.Factory.newInstance( means, variances );
        expressionExperimentService.updateMeanVarianceRelation( eeWithMvr, mvr );
    }

    @AfterEach
    public void tearDownFixtures() {
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
}

package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * A paired analysis at the analyzer level: the subject factor is in the model (its columns adjust every
 * other factor's contrasts) but produces NO result set.
 * <p>
 * The fixture is six subjects, each carrying both a control and a treated sample, plus a tissue factor with
 * no pairing structure. A balanced two-subject-per-cell design like the old ones would hide the distinction
 * between "subject left out of the model" and "subject suppressed from the results", so the subject factor
 * has one level per subject and tissue stays fully crossed.
 *
 * @author paul
 */
@ContextConfiguration
public class PairedDesignAnalysisTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class PairedDesignAnalysisTestContextConfiguration {

        @Bean
        public DiffExAnalyzer diffExAnalyzer() {
            return new LinearModelAnalyzer();
        }

        @Bean
        public CompositeSequenceService compositeSequenceService() {
            return mock();
        }

        @Bean
        public AsyncTaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }
    }

    private static final int NUM_PROBES = 50;

    @Autowired
    private DiffExAnalyzer analyzer;

    private ExpressionExperiment ee;
    private BioAssayDimension dimension;
    private ExperimentalFactor treatment, tissue, subject;
    private FactorValue caseFv, controlFv, cortexFv, liverFv;

    /**
     * Twelve samples: 6 subjects x (case, control), 6 of each tissue (3 subjects per tissue).
     */
    private void buildFixture() {
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < NUM_PROBES; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, ad );
            cs.setId( ( long ) i );
            ad.getCompositeSequences().add( cs );
        }

        ee = new ExpressionExperiment();

        treatment = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        treatment.setId( 1L );
        caseFv = FactorValue.Factory.newInstance( treatment, Characteristic.Factory.newInstance( Categories.TREATMENT, "case", null ) );
        caseFv.setId( 1L );
        controlFv = FactorValue.Factory.newInstance( treatment, Characteristic.Factory.newInstance( Categories.TREATMENT, "control", null ) );
        controlFv.setId( 2L );
        treatment.getFactorValues().addAll( Arrays.asList( caseFv, controlFv ) );

        tissue = ExperimentalFactor.Factory.newInstance( "tissue", FactorType.CATEGORICAL );
        tissue.setId( 2L );
        cortexFv = FactorValue.Factory.newInstance( tissue, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "cortex", null ) );
        cortexFv.setId( 3L );
        liverFv = FactorValue.Factory.newInstance( tissue, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "liver", null ) );
        liverFv.setId( 4L );
        tissue.getFactorValues().addAll( Arrays.asList( cortexFv, liverFv ) );

        subject = ExperimentalFactor.Factory.newInstance( "subject", FactorType.CATEGORICAL );
        subject.setId( 3L );

        dimension = new BioAssayDimension();
        int n = 0;
        for ( int s = 0; s < 6; s++ ) {
            FactorValue subjectFv = FactorValue.Factory.newInstance( subject,
                    Characteristic.Factory.newInstance( Categories.BLOCK, "subj" + s, null ) );
            subjectFv.setId( 10L + s );
            subject.getFactorValues().add( subjectFv );

            for ( FactorValue trtFv : Arrays.asList( controlFv, caseFv ) ) {
                BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + n );
                bm.setId( ( long ) n );
                bm.getFactorValues().add( subjectFv );
                bm.getFactorValues().add( trtFv );
                // alternate tissue by subject so each tissue has 3 subjects x 2 arms = 6 samples
                bm.getFactorValues().add( s < 3 ? cortexFv : liverFv );
                BioAssay ba = BioAssay.Factory.newInstance( "ba" + n, ad, bm );
                ba.setSampleUsed( bm );
                bm.getBioAssaysUsedIn().add( ba );
                dimension.getBioAssays().add( ba );
                ee.getBioAssays().add( ba );
                n++;
            }
        }
    }

    private static Set<String> factorNames( DifferentialExpressionAnalysis analysis ) {
        Set<String> names = new HashSet<>();
        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            List<String> perSet = new ArrayList<>();
            for ( ExperimentalFactor f : rs.getExperimentalFactors() ) {
                perSet.add( f.getName() );
            }
            Collections.sort( perSet );
            names.add( String.join( ":", perSet ) );
        }
        return names;
    }

    /**
     * The rule: subject columns are in the model (the treatment contrast is within-subject) but the subject
     * factor itself yields no result set, and tissue is unaffected.
     */
    @Test
    public void blockingFactorIsModelledButNotReported() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, tissue, subject ) );
        config.setBlockingFactors( Collections.singletonList( subject ) );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config );
        assertThat( analyses ).hasSize( 1 );

        DifferentialExpressionAnalysis analysis = analyses.iterator().next();

        // three model factors, but only the two reported ones get result sets
        assertThat( factorNames( analysis ) ).containsExactlyInAnyOrder( "tissue", "treatment" );

        // the analysis's own record says what was blocked
        assertThat( analysis.getProtocol().getDescription() )
                .contains( "# Blocking factors (no results reported for them): " );

        // the treatment result set has 12 samples' worth of contrasts (case vs control), one per probe,
        // and every probe's contrast is a case-vs-control contrast, not a subject contrast
        ExpressionAnalysisResultSet treatmentRs = analysis.getResultSets().stream()
                .filter( rs -> rs.getExperimentalFactors().size() == 1 && rs.getExperimentalFactors().iterator().next().equals( treatment ) )
                .findFirst().orElseThrow();
        assertThat( treatmentRs.getResults() ).hasSize( NUM_PROBES );
        for ( var r : treatmentRs.getResults() ) {
            assertThat( r.getContrasts() ).hasSize( 1 );
            assertThat( r.getContrasts().iterator().next().getFactorValue() ).isEqualTo( caseFv );
            assertThat( r.getContrasts().iterator().next().getSecondFactorValue() ).isNull();
        }
    }

    /**
     * Positive control off the same fixture: without the blocking flag, the subject factor gets its own
     * result set (six subject contrasts per probe), so the test above cannot pass merely because the
     * fixture is degenerate.
     */
    @Test
    public void withoutBlockingFlagTheSubjectFactorIsReported() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, tissue, subject ) );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config );
        assertThat( analyses ).hasSize( 1 );

        DifferentialExpressionAnalysis analysis = analyses.iterator().next();
        assertThat( factorNames( analysis ) ).contains( "subject" );
        assertThat( analysis.getProtocol().getDescription() )
                .doesNotContain( "# Blocking factors" );
    }

    /**
     * A config assembled field-by-field (bypassing addInteractionToInclude) that asks for a subject-by-treatment
     * interaction is refused at the analyzer, not silently reinterpreted.
     */
    @Test
    public void interactionWithBlockingFactorIsRefused() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, subject ) );
        config.setBlockingFactors( Collections.singletonList( subject ) );
        config.getInteractionsToInclude().add( new HashSet<>( Arrays.asList( treatment, subject ) ) );

        org.assertj.core.api.Assertions.assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "mixed model" );
    }

    /**
     * A blocking factor that is not in factorsToInclude is ignored with a warning, not a crash: the columns
     * simply never enter the model.
     */
    @Test
    public void blockingFactorNotInFactorsToIncludeIsIgnored() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, tissue ) );
        config.setBlockingFactors( Collections.singletonList( subject ) );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config );
        assertThat( analyses ).hasSize( 1 );
        assertThat( factorNames( analyses.iterator().next() ) ).containsExactlyInAnyOrder( "tissue", "treatment" );
        // protocol written from the config still names it -- the config is what the user asked for
        assertThat( analyses.iterator().next().getProtocol().getDescription() )
                .contains( "# Blocking factors" );
    }

    /**
     * With only the blocking factor in the model there is nothing to report; refused up front rather than left
     * to fail on an empty model formula.
     */
    @Test
    public void blockingFactorAsTheOnlyFactorIsRefused() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Collections.singletonList( subject ) );
        config.setBlockingFactors( Collections.singletonList( subject ) );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config ) )
                .hasMessageContaining( "Nothing left to model" );
    }

    private DifferentialExpressionAnalysisConfig mixedConfig( ExperimentalFactor... factors ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( factors ) );
        config.setBlockingFactors( Collections.singletonList( subject ) );
        config.setEstimateBlockingCorrelation( true );
        return config;
    }

    /**
     * Mixed model end to end: the subject has no columns and no result set, treatment and tissue are
     * reported with finite p-values, and the protocol records both the blocking factor and that its
     * correlation was estimated (a redo reads that line back).
     */
    @Test
    public void mixedModelReportsTreatmentAndTissueButNotSubject() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, mixedConfig( treatment, tissue, subject ) );
        assertThat( analyses ).hasSize( 1 );
        DifferentialExpressionAnalysis analysis = analyses.iterator().next();

        assertThat( factorNames( analysis ) ).containsExactlyInAnyOrder( "tissue", "treatment" );
        assertThat( analysis.getProtocol().getDescription() )
                .contains( "# Blocking factors (no results reported for them): " )
                .contains( DiffExAnalyzerUtils.ESTIMATE_BLOCKING_CORRELATION_PROTOCOL_LINE );
        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            assertThat( rs.getResults() ).hasSize( NUM_PROBES );
            for ( var r : rs.getResults() ) {
                assertThat( r.getPvalue() ).isBetween( 0.0, 1.0 );
            }
        }
    }

    /**
     * The fixed-effect protocol does not claim a correlation was estimated.
     */
    @Test
    public void fixedEffectBlockingDoesNotWriteTheEstimatedCorrelationLine() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, tissue, subject ) );
        config.setBlockingFactors( Collections.singletonList( subject ) );

        DifferentialExpressionAnalysis analysis = analyzer.run( ee, dmatrix, config ).iterator().next();
        assertThat( analysis.getProtocol().getDescription() )
                .contains( "# Blocking factors" )
                .doesNotContain( DiffExAnalyzerUtils.ESTIMATE_BLOCKING_CORRELATION_PROTOCOL_LINE );
    }

    @Test
    public void mixedModelWithoutABlockingFactorIsRefused() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, tissue ) );
        config.setEstimateBlockingCorrelation( true );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config ) )
                .hasMessageContaining( "requires at least one blocking factor" );
    }

    /**
     * A blocking factor left out of factorsToInclude is a warning for the fixed-effect fit but an error for
     * the mixed model, where silently running unblocked would report an unadjusted analysis as a mixed one.
     */
    @Test
    public void mixedModelWithTheBlockingFactorNotInTheModelIsRefused() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, mixedConfig( treatment, tissue ) ) )
                .hasMessageContaining( "needs every sample in a block" );
    }

    @Test
    public void mixedModelWithVoomWeightsIsRefused() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );
        DifferentialExpressionAnalysisConfig config = mixedConfig( treatment, tissue, subject );
        config.setUseWeights( true );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config ) )
                .isInstanceOf( UnsupportedOperationException.class )
                .hasMessageContaining( "voom weights" );
    }

    @Test
    public void mixedModelWithTwoBlockingFactorsIsRefused() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );
        DifferentialExpressionAnalysisConfig config = mixedConfig( treatment, tissue, subject );
        config.setBlockingFactors( Arrays.asList( subject, tissue ) );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config ) )
                .isInstanceOf( UnsupportedOperationException.class )
                .hasMessageContaining( "Only one blocking factor" );
    }

    /**
     * Analyzer-level counterpart of the CLI check: a blocking factor cannot be set once an interaction with
     * it is configured, whichever order the config is assembled in.
     */
    @Test
    public void blockingFactorAfterAnInteractionIsStillRefusedByTheAnalyzer() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, tissue, subject ) );
        config.addInteractionToInclude( Arrays.asList( treatment, subject ) );
        config.setBlockingFactors( Collections.singletonList( subject ) );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config ) )
                .hasMessageContaining( "cannot also be in an interaction" );
    }
}

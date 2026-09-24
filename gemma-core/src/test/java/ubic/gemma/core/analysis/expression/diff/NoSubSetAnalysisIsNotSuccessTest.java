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
 * Producing no analysis at all is a failure of the experiment, not a quiet empty result.
 *
 * <h2>What went wrong</h2>
 *
 * <p>A subset can leave the per-subset loop without raising anything — it is DE_Exclude, it has no samples left, or
 * {@code fixFactorsForSubset} finds nothing it can model and the loop logs "Experimental design is not valid for
 * subset: ...; skipping". The old guard fired only when the result was empty AND at least one subset had thrown, so
 * an experiment whose subsets were all skipped returned an empty collection. GSE74400 (eid 12822) did exactly that:
 * the CLI printed "Performed 0 differential expression analyses." through {@code addSuccessObject} and exited 0, and
 * a batch runner testing the exit status counted it as done.</p>
 *
 * <p>This is a different question from {@code -ignoreFailingSubsets}, which is about carrying on when SOME subsets
 * succeed, so the exception is raised whatever that flag says.</p>
 *
 * @author paul
 */
@ContextConfiguration
public class NoSubSetAnalysisIsNotSuccessTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class NoSubSetAnalysisIsNotSuccessTestContextConfiguration {

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

    private static final int NUM_PROBES = 100;

    @Autowired
    private DiffExAnalyzer analyzer;

    private ExpressionExperiment ee;
    private BioAssayDimension dimension;
    private ExperimentalFactor treatment, cellType;
    private FactorValue caseFv, controlFv, ct1, ct2;

    /**
     * <p>Twelve samples over cellType(ct1, ct2). When {@code treatmentVariesWithinArm} is false, every ct1 sample is
     * a case and every ct2 sample a control, so treatment is complete across the experiment — it survives
     * {@code dropIncompleteFactors} — but constant inside each arm, which is what {@code fixFactorsForSubset}
     * removes. Both arms are then left with nothing to model and are skipped.</p>
     */
    private void buildFixture( boolean treatmentVariesWithinArm ) {
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

        cellType = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL );
        cellType.setId( 2L );
        ct1 = FactorValue.Factory.newInstance( cellType, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct1", null ) );
        ct1.setId( 3L );
        ct2 = FactorValue.Factory.newInstance( cellType, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct2", null ) );
        ct2.setId( 4L );
        cellType.getFactorValues().addAll( Arrays.asList( ct1, ct2 ) );

        dimension = new BioAssayDimension();
        int n = 0;
        for ( FactorValue subsetFv : Arrays.asList( ct1, ct2 ) ) {
            for ( int i = 0; i < 6; i++ ) {
                BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + n );
                bm.setId( ( long ) n );
                bm.getFactorValues().add( subsetFv );
                if ( treatmentVariesWithinArm ) {
                    bm.getFactorValues().add( i < 3 ? caseFv : controlFv );
                } else {
                    bm.getFactorValues().add( ct1.equals( subsetFv ) ? caseFv : controlFv );
                }
                BioAssay ba = BioAssay.Factory.newInstance( "ba" + n, ad, bm );
                ba.setSampleUsed( bm );
                bm.getBioAssaysUsedIn().add( ba );
                ee.getBioAssays().add( ba );
                dimension.getBioAssays().add( ba );
                n++;
            }
        }
    }

    private DifferentialExpressionAnalysisConfig config( boolean ignoreFailingSubsets ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Collections.singleton( treatment ) );
        config.setSubsetFactor( cellType );
        config.setIgnoreFailingSubsets( ignoreFailingSubsets );
        return config;
    }

    /**
     * The rule. Every subset skipped as "design is not valid" is not a quiet empty result.
     */
    @Test
    public void testEverySubsetSkippedIsAFailure() {
        buildFixture( false );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config( false ) ) )
                .isInstanceOf( AllSubSetAnalysesFailedException.class )
                .hasMessageContaining( "0 failed, 2 skipped" );
    }

    /**
     * -ignoreFailingSubsets is about carrying on when SOME subsets succeed; it does not make "none of them did" a
     * success. GSE74400 would have run with it set.
     */
    @Test
    public void testIgnoreFailingSubsetsDoesNotSuppressIt() {
        buildFixture( false );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        assertThatThrownBy( () -> analyzer.run( ee, dmatrix, config( true ) ) )
                .isInstanceOf( AllSubSetAnalysesFailedException.class );
    }

    /**
     * Positive control off the same fixture: let treatment vary within each arm and both subsets are analyzed, so
     * an analyzer that had simply become unable to analyze anything could not pass the two tests above.
     */
    @Test
    public void testASubsetThatCanBeModelledStillRuns() {
        buildFixture( true );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config( false ) );

        assertThat( analyses ).hasSize( 2 );
        assertThat( analyses ).extracting( DifferentialExpressionAnalysis::getSubsetFactorValue )
                .containsExactlyInAnyOrder( ct1, ct2 );
    }
}

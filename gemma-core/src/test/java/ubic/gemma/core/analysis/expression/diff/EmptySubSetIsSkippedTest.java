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
import static org.mockito.Mockito.mock;

/**
 * An arm of the subset factor that no analyzed sample carries is skipped, not a fatal condition for the experiment.
 *
 * <h2>Why an arm can be empty</h2>
 *
 * <p>{@code makeSubSetMatrices} partitions {@code samplesUsed}, which is what survived filtering and QC — not the
 * samples assigned to the factor value. So a factor value several samples carry can still partition to nothing.
 * GSE74998 (eid 34217) aborted on exactly that: {@code IllegalArgumentException: The subset was empty for fv: ...
 * Category=cell type Value=embryonic stem cell}.</p>
 *
 * <p>Two things made {@code -ignoreFailingSubsets} unable to help, and the second is the one that decides where the
 * fix goes: the exception was outside the {@link AnalysisException} hierarchy the per-subset catch matches, and it
 * was raised from matrix construction, which runs BEFORE the guarded loop. The empty arm therefore has to be
 * dropped while the matrices are built.</p>
 *
 * @author paul
 */
@ContextConfiguration
public class EmptySubSetIsSkippedTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class EmptySubSetIsSkippedTestContextConfiguration {

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
    private FactorValue caseFv, controlFv, ct1, ct2, ctEmpty;
    private Map<FactorValue, ExpressionExperimentSubSet> subsets;

    /**
     * <p>cellType carries three values. ct1 and ct2 get six samples each, three case and three control. ctEmpty
     * gets one sample that belongs to the experiment and to ctEmpty's stored subset but is left OUT of the
     * BioAssayDimension, so it never reaches the data matrix — the shape filtering and QC leave behind.</p>
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

        cellType = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL );
        cellType.setId( 2L );
        ct1 = FactorValue.Factory.newInstance( cellType, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct1", null ) );
        ct1.setId( 3L );
        ct2 = FactorValue.Factory.newInstance( cellType, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct2", null ) );
        ct2.setId( 4L );
        ctEmpty = FactorValue.Factory.newInstance( cellType, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "embryonic stem cell", null ) );
        ctEmpty.setId( 5L );
        cellType.getFactorValues().addAll( Arrays.asList( ct1, ct2, ctEmpty ) );

        subsets = new HashMap<>();
        for ( FactorValue fv : Arrays.asList( ct1, ct2, ctEmpty ) ) {
            subsets.put( fv, ExpressionExperimentSubSet.Factory
                    .newInstance( "Subset for " + FactorValueUtils.getSummaryString( fv ), ee ) );
        }

        dimension = new BioAssayDimension();
        int n = 0;
        for ( FactorValue subsetFv : Arrays.asList( ct1, ct2 ) ) {
            for ( int i = 0; i < 6; i++ ) {
                BioAssay ba = newSample( ad, n++, subsetFv, i < 3 ? caseFv : controlFv );
                dimension.getBioAssays().add( ba );
            }
        }

        // the empty arm: assigned to ctEmpty and listed by its subset, but absent from the dimension and so from
        // the matrix, exactly as a sample dropped by filtering or QC would be
        newSample( ad, n, ctEmpty, caseFv );
    }

    private BioAssay newSample( ArrayDesign ad, int n, FactorValue subsetFv, FactorValue treatmentFv ) {
        BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + n );
        bm.setId( ( long ) n );
        bm.getFactorValues().add( subsetFv );
        bm.getFactorValues().add( treatmentFv );
        BioAssay ba = BioAssay.Factory.newInstance( "ba" + n, ad, bm );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        ee.getBioAssays().add( ba );
        subsets.get( subsetFv ).getBioAssays().add( ba );
        return ba;
    }

    private DifferentialExpressionAnalysisConfig config( boolean ignoreFailingSubsets ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Collections.singleton( treatment ) );
        config.setSubsetFactor( cellType );
        config.setIgnoreFailingSubsets( ignoreFailingSubsets );
        return config;
    }

    private static Set<String> factorNames( DifferentialExpressionAnalysis analysis ) {
        Set<String> names = new HashSet<>();
        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            for ( ExperimentalFactor f : rs.getExperimentalFactors() ) {
                names.add( f.getName() );
            }
        }
        return names;
    }

    /**
     * The rule, on the path production took. The empty arm is skipped and the two populated arms are analyzed —
     * and it holds with {@code -ignoreFailingSubsets} unset, because an arm with no samples is an absence of data
     * to analyze rather than an analysis that failed.
     */
    @Test
    public void testAnEmptyArmDoesNotAbortTheExperiment() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config( false ) );

        assertThat( analyses ).hasSize( 2 );
        assertThat( analyses ).extracting( DifferentialExpressionAnalysis::getSubsetFactorValue )
                .containsExactlyInAnyOrder( ct1, ct2 );
        // the two survivors really were analyzed, so an empty result cannot pass this test
        assertThat( analyses ).allSatisfy( a -> assertThat( factorNames( a ) ).containsExactly( "treatment" ) );
    }

    /**
     * The other entry point, {@code run(ee, subsets, dmatrix, config)}, is handed subsets that already exist rather
     * than deriving them from the matrices — {@code AnalysisSelectionAndExecutionService} reuses stored ones. The
     * stored subset for the empty arm still lists its samples, so this map and the map of matrices disagree, and
     * the loop has to skip the arm it has no matrix for.
     */
    @Test
    public void testAnEmptyArmWithAStoredSubsetIsSkipped() {
        buildFixture();
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        assertThat( subsets.get( ctEmpty ).getBioAssays() ).hasSize( 1 );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, subsets, dmatrix, config( true ) );

        assertThat( analyses ).hasSize( 2 );
        assertThat( analyses ).extracting( DifferentialExpressionAnalysis::getSubsetFactorValue )
                .containsExactlyInAnyOrder( ct1, ct2 );
        assertThat( analyses ).allSatisfy( a -> assertThat( factorNames( a ) ).containsExactly( "treatment" ) );
    }
}

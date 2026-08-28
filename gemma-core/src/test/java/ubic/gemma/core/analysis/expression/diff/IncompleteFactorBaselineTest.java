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
 * A factor dropped for missing values must not be left behind in the baseline map.
 *
 * <p>{@code dropIncompleteFactors} removes a factor some sample carries no value for, and the design matrix is
 * built from what survives. The baseline map is a second description of the same model, and
 * {@code makeDesignMatrix} calls {@code setBaseline} for every entry it holds — so an entry for a dropped factor
 * fails the whole analysis with {@code No factor known by name fact.2, choices are: fact.1}.</p>
 *
 * <p>The subset path derives its baselines after both drops for exactly this reason, with a comment saying so.
 * The whole-experiment path derived them before the drop.</p>
 *
 * @author gembro
 */
@ContextConfiguration
public class IncompleteFactorBaselineTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class IncompleteFactorBaselineTestContextConfiguration {

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
    private ExperimentalFactor treatment, sex;

    /**
     * Sixteen samples on {@code treatment}(case, control), with {@code sex} assigned to all but the last two.
     *
     * <p>The FIRST sample carries a sex value on purpose: {@code BaselineSelection.getBaselineConditions} falls
     * back to the first sample's factor values for any factor without an obvious baseline, so this is what mints
     * a baseline entry for the factor that is about to be dropped. A fixture whose first sample lacked the value
     * would leave the map empty and pass whatever the ordering.</p>
     */
    private void buildFixture( boolean sexIncomplete ) {
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < NUM_PROBES; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, ad );
            cs.setId( ( long ) i );
            ad.getCompositeSequences().add( cs );
        }

        ee = new ExpressionExperiment();

        treatment = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        treatment.setId( 1L );
        FactorValue caseFv = FactorValue.Factory.newInstance( treatment, Characteristic.Factory.newInstance( Categories.TREATMENT, "case", null ) );
        caseFv.setId( 1L );
        FactorValue controlFv = FactorValue.Factory.newInstance( treatment, Characteristic.Factory.newInstance( Categories.TREATMENT, "control", null ) );
        controlFv.setId( 2L );
        treatment.getFactorValues().addAll( Arrays.asList( caseFv, controlFv ) );

        sex = ExperimentalFactor.Factory.newInstance( "sex", FactorType.CATEGORICAL );
        sex.setId( 2L );
        FactorValue male = FactorValue.Factory.newInstance( sex, Characteristic.Factory.newInstance( Categories.BIOLOGICAL_SEX, "male", null ) );
        male.setId( 3L );
        FactorValue female = FactorValue.Factory.newInstance( sex, Characteristic.Factory.newInstance( Categories.BIOLOGICAL_SEX, "female", null ) );
        female.setId( 4L );
        sex.getFactorValues().addAll( Arrays.asList( male, female ) );

        dimension = new BioAssayDimension();
        for ( int i = 0; i < 16; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            bm.setId( ( long ) i );
            bm.getFactorValues().add( i < 8 ? caseFv : controlFv );
            boolean withoutSex = sexIncomplete && i >= 14;
            if ( !withoutSex ) {
                bm.getFactorValues().add( ( i % 4 ) < 2 ? male : female );
            }
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            ba.setSampleUsed( bm );
            bm.getBioAssaysUsedIn().add( ba );
            dimension.getBioAssays().add( ba );
            ee.getBioAssays().add( ba );
        }
    }

    private DifferentialExpressionAnalysisConfig config() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, sex ) );
        return config;
    }

    private static Set<String> modelledFactorNames( DifferentialExpressionAnalysis analysis ) {
        Set<String> names = new HashSet<>();
        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            for ( ExperimentalFactor f : rs.getExperimentalFactors() ) {
                names.add( f.getName() );
            }
        }
        return names;
    }

    /** The rule: the dropped factor leaves the model AND the baseline map, and the analysis runs. */
    @Test
    public void testAnIncompleteFactorDoesNotLeaveABaselineBehind() {
        buildFixture( true );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config() );

        assertThat( analyses ).hasSize( 1 );
        assertThat( modelledFactorNames( analyses.iterator().next() ) )
                .withFailMessage( "sex is missing for two samples, so it cannot be modelled" )
                .containsExactly( "treatment" );
    }

    /**
     * The positive control, off the same fixture: with every sample assigned a sex, nothing is dropped and both
     * factors are modelled. Without it, an analysis that simply lost `sex` for an unrelated reason would pass the
     * test above.
     */
    @Test
    public void testACompleteFactorIsModelled() {
        buildFixture( false );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config() );

        assertThat( analyses ).hasSize( 1 );
        assertThat( modelledFactorNames( analyses.iterator().next() ) )
                .containsExactlyInAnyOrder( "treatment", "sex" );
    }
}

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
 * A subset analysis has to describe itself with the factors it actually modelled.
 *
 * <p>{@code fixFactorsForSubset} drops the factors a subset cannot model — a factor constant within that subset has
 * no contrast to fit — and the design matrix is built from what it returns. Everything decided beside it has to read
 * the same list: the config carries {@code interactionsToInclude} into {@code buildModelFormula}, so a config
 * narrowed against the full factor list names an interaction whose factor has no column in the design matrix.</p>
 *
 * @author paul
 */
@ContextConfiguration
public class SubsettedAnalysisFactorConsistencyTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class SubsettedAnalysisFactorConsistencyTestContextConfiguration {

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
    private ExperimentalFactor treatment, sex, cellType;
    private FactorValue caseFv, controlFv, male, female, ct1, ct2;
    private Map<FactorValue, ExpressionExperimentSubSet> subsets;

    /**
     * Sixteen samples over cellType(ct1, ct2) x treatment(case, control) x sex(male, female).
     *
     * <p>ct1 is deliberately all-male, so {@code sex} is constant there and the subset can model only
     * {@code treatment}. ct2 carries both sexes and is the positive control: it keeps both factors and their
     * interaction, off this same fixture, so an analysis that simply produced nothing cannot pass as the rule
     * working.</p>
     */
    private void buildFixture( boolean ct1AllMale ) {
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

        sex = ExperimentalFactor.Factory.newInstance( "sex", FactorType.CATEGORICAL );
        sex.setId( 2L );
        male = FactorValue.Factory.newInstance( sex, Characteristic.Factory.newInstance( Categories.BIOLOGICAL_SEX, "male", null ) );
        male.setId( 3L );
        female = FactorValue.Factory.newInstance( sex, Characteristic.Factory.newInstance( Categories.BIOLOGICAL_SEX, "female", null ) );
        female.setId( 4L );
        sex.getFactorValues().addAll( Arrays.asList( male, female ) );

        cellType = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL );
        cellType.setId( 3L );
        ct1 = FactorValue.Factory.newInstance( cellType, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct1", null ) );
        ct1.setId( 5L );
        ct2 = FactorValue.Factory.newInstance( cellType, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct2", null ) );
        ct2.setId( 6L );
        cellType.getFactorValues().addAll( Arrays.asList( ct1, ct2 ) );

        subsets = new HashMap<>();
        for ( FactorValue fv : Arrays.asList( ct1, ct2 ) ) {
            subsets.put( fv, ExpressionExperimentSubSet.Factory
                    .newInstance( "Subset for " + FactorValueUtils.getSummaryString( fv ), ee ) );
        }

        dimension = new BioAssayDimension();
        int n = 0;
        for ( FactorValue subsetFv : Arrays.asList( ct1, ct2 ) ) {
            for ( int i = 0; i < 8; i++ ) {
                BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + n );
                bm.setId( ( long ) n );
                bm.getFactorValues().add( subsetFv );
                // four case, four control within each subset
                bm.getFactorValues().add( i < 4 ? caseFv : controlFv );
                if ( subsetFv.equals( ct1 ) && ct1AllMale ) {
                    bm.getFactorValues().add( male );
                } else {
                    // alternate within each treatment arm so every cell of treatment x sex is filled
                    bm.getFactorValues().add( ( i % 4 ) < 2 ? male : female );
                }
                BioAssay ba = BioAssay.Factory.newInstance( "ba" + n, ad, bm );
                ba.setSampleUsed( bm );
                bm.getBioAssaysUsedIn().add( ba );
                dimension.getBioAssays().add( ba );
                ee.getBioAssays().add( ba );
                subsets.get( subsetFv ).getBioAssays().add( ba );
                n++;
            }
        }
    }

    private DifferentialExpressionAnalysisConfig configWithInteraction() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Arrays.asList( treatment, sex ) );
        config.addInteractionToInclude( Arrays.asList( treatment, sex ) );
        config.setSubsetFactor( cellType );
        return config;
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
     * The rule. ct1 cannot model sex, so neither sex nor the treatment:sex interaction may appear in its analysis —
     * the interaction reaches the model formula through the config, not through the factor list, so a config
     * narrowed against the wrong list puts a term in the formula for a factor the design matrix does not have.
     */
    @Test
    public void testASubsetThatCannotModelAFactorDoesNotCarryItsInteraction() {
        buildFixture( true );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        Collection<DifferentialExpressionAnalysis> analyses =
                analyzer.run( ee, subsets, dmatrix, configWithInteraction() );

        assertThat( analyses ).hasSize( 2 );

        DifferentialExpressionAnalysis ct1Analysis = analyses.stream()
                .filter( a -> ct1.equals( a.getSubsetFactorValue() ) )
                .findFirst().orElseThrow( () -> new AssertionError( "no analysis for ct1" ) );
        DifferentialExpressionAnalysis ct2Analysis = analyses.stream()
                .filter( a -> ct2.equals( a.getSubsetFactorValue() ) )
                .findFirst().orElseThrow( () -> new AssertionError( "no analysis for ct2" ) );

        // ct1 is all-male: treatment only, and no interaction term
        assertThat( factorNames( ct1Analysis ) ).containsExactly( "treatment" );

        // The analysis has to describe itself with what it modelled. The config becomes the stored protocol
        // (createProtocolForConfig reads factorsToInclude, interactionsToInclude and the baseline map), so a
        // config or a baseline map narrowed against the full factor list leaves ct1's own record naming sex.
        assertThat( ct1Analysis.getProtocol().getDescription() )
                .contains( "# No interactions defined." )
                .doesNotContain( "sex" );

        // positive control, same fixture: ct2 carries both sexes, so it keeps both factors AND the interaction
        assertThat( factorNames( ct2Analysis ) )
                .contains( "treatment", "sex", "sex:treatment" );
        assertThat( ct2Analysis.getProtocol().getDescription() )
                .contains( "# Interactions: " )
                .contains( "sex" );
    }

    /**
     * The single-subset overload, {@code run(ExpressionExperimentSubSet, ...)}, is a separate path -- it is what a
     * redo of one subset analysis takes -- and it drops incomplete factors itself. A factor some sample in the
     * subset carries no value for cannot be modelled even though it is not constant, so it must not reach the
     * model or the analysis's own record of itself.
     */
    @Test
    public void testTheSingleSubsetPathDropsAFactorWithMissingValues() {
        buildFixture( false );

        // strip sex from one ct1 sample: within ct1 sex now has a missing value while still carrying both levels,
        // so fixFactorsForSubset keeps it and only the incompleteness check can remove it
        BioAssay firstOfCt1 = subsets.get( ct1 ).getBioAssays().iterator().next();
        firstOfCt1.getSampleUsed().getFactorValues().removeIf( fv -> sex.equals( fv.getExperimentalFactor() ) );

        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysis analysis =
                analyzer.run( subsets.get( ct1 ), dmatrix, configWithInteraction() );

        assertThat( analysis ).isNotNull();
        assertThat( factorNames( analysis ) ).containsExactly( "treatment" );
        assertThat( analysis.getProtocol().getDescription() )
                .contains( "# No interactions defined." )
                .doesNotContain( "sex" );
    }

    /**
     * Positive control for the single-subset path, off the same fixture: with nothing missing, ct1 models both
     * factors and keeps the interaction, so an analysis that simply modelled less cannot pass the test above.
     */
    @Test
    public void testTheSingleSubsetPathKeepsACompleteFactor() {
        buildFixture( false );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        DifferentialExpressionAnalysis analysis =
                analyzer.run( subsets.get( ct1 ), dmatrix, configWithInteraction() );

        assertThat( analysis ).isNotNull();
        assertThat( factorNames( analysis ) ).contains( "treatment", "sex", "sex:treatment" );
    }

    /**
     * The positive control on its own: with both subsets carrying both sexes, nothing is dropped anywhere and both
     * analyses keep the interaction. Pins that the rule above narrows only the subset that needs narrowing.
     */
    @Test
    public void testASubsetThatCanModelEveryFactorKeepsThemAll() {
        buildFixture( false );
        ExpressionDataDoubleMatrix dmatrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee, dimension );

        Collection<DifferentialExpressionAnalysis> analyses =
                analyzer.run( ee, subsets, dmatrix, configWithInteraction() );

        assertThat( analyses ).hasSize( 2 );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            assertThat( factorNames( analysis ) )
                    .as( "subset %s", analysis.getSubsetFactorValue() )
                    .contains( "treatment", "sex", "sex:treatment" );
        }
    }
}

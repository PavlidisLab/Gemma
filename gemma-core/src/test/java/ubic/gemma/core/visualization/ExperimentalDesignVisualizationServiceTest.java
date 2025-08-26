package ubic.gemma.core.visualization;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellDataVectorAggregatorUtils;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.RandomExperimentalDesignUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ubic.gemma.persistence.service.expression.experiment.RandomExperimentalDesignUtils.randomContinuousFactor;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class ExperimentalDesignVisualizationServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public ExperimentalDesignVisualizationService experimentalDesignVisualizationService() {
            return new ExperimentalDesignVisualizationServiceImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }
    }

    @Autowired
    private ExperimentalDesignVisualizationService experimentalDesignVisualizationService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @After
    public void resetMocks() {
        reset( expressionExperimentService );
    }

    @Test
    @WithMockUser
    public void testOrderBasicExperiment() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "de" + i, ad ) );
        }
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        for ( int i = 0; i < 10; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i, ad, BioMaterial.Factory.newInstance( "bm" + i ) ) );
        }

        ee.setExperimentalDesign( new ExperimentalDesign() );
        randomCategoricalFactor( ee, "genotype", 2 );
        randomCategoricalFactor( ee, "treatment", 2 );
        randomContinuousFactor( ee, "age" );

        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<RawExpressionDataVector> vectors = RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class );

        ExpressionExperimentValueObject eeVo = new ExpressionExperimentValueObject( ee );
        QuantitationTypeValueObject qtVo = new QuantitationTypeValueObject( vectors.iterator().next().getQuantitationType() );
        BioAssayDimension bad = vectors.iterator().next().getBioAssayDimension();
        BioAssayDimensionValueObject badVo = new BioAssayDimensionValueObject( bad );
        ArrayDesignValueObject adVo = new ArrayDesignValueObject( vectors.iterator().next().getDesignElement().getArrayDesign() );

        when( expressionExperimentService.loadAndThawLiteOrFail( eq( ee.getId() ), any(), any() ) )
                .thenReturn( ee );
        when( expressionExperimentService.getBioAssayDimensions( ee ) ).thenReturn( Collections.singleton( bad ) );

        Collection<DoubleVectorValueObject> voVectors = vectors.stream()
                .map( v -> new DoubleVectorValueObject( v, eeVo, qtVo, badVo, adVo, null ) )
                .collect( Collectors.toList() );

        assertThat( experimentalDesignVisualizationService.sortVectorDataByDesign( voVectors, null ) )
                .hasEntrySatisfying( 1L, m -> assertThat( m ).hasSize( 10 ) );
    }

    @Test
    @WithMockUser
    public void testOrderExperimentSubSet() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "de" + i, ad ) );
        }
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        List<ExpressionExperimentSubSet> subsets = new ArrayList<>( 4 );
        for ( int i = 0; i < 4; i++ ) {
            ExpressionExperimentSubSet subset = ExpressionExperimentSubSet.Factory.newInstance( "eess" + i, ee );
            subset.setId( i + 2L );
            subsets.add( subset );
        }
        for ( int i = 0; i < 12; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, BioMaterial.Factory.newInstance( "bm" + i ) );
            ee.getBioAssays().add( ba );
            subsets.get( i % 4 ).getBioAssays().add( ba );
        }

        ee.setExperimentalDesign( new ExperimentalDesign() );
        randomCategoricalFactor( ee, "genotype", 2 );
        randomCategoricalFactor( ee, "treatment", 2 );
        randomContinuousFactor( ee, "age" );

        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );

        Collection<RawExpressionDataVector> vectors = RandomBulkDataUtils.randomBulkVectors( ee, subsets, ad, qt, RawExpressionDataVector.class );

        ExpressionExperimentValueObject eeVo = new ExpressionExperimentValueObject( ee );
        QuantitationTypeValueObject qtVo = new QuantitationTypeValueObject( vectors.iterator().next().getQuantitationType() );
        BioAssayDimension bad = vectors.iterator().next().getBioAssayDimension();
        BioAssayDimensionValueObject badVo = new BioAssayDimensionValueObject( bad );
        ArrayDesignValueObject adVo = new ArrayDesignValueObject( vectors.iterator().next().getDesignElement().getArrayDesign() );

        when( expressionExperimentService.loadAndThawLiteOrFail( eq( ee.getId() ), any(), any() ) ).thenReturn( ee );

        // for regular subsets, it's the same
        when( expressionExperimentService.getBioAssayDimensions( ee ) ).thenReturn( Collections.singleton( bad ) );
        when( expressionExperimentService.getBioAssayDimensionsFromSubSets( ee ) ).thenReturn( Collections.singleton( bad ) );

        ExpressionExperimentSubsetValueObject subsetZero = new ExpressionExperimentSubsetValueObject( subsets.get( 0 ) );
        BioAssayDimensionValueObject badZero = new BioAssayDimensionValueObject( BioAssayDimension.Factory.newInstance( new ArrayList<>( subsets.get( 0 ).getBioAssays() ) ) );
        Collection<DoubleVectorValueObject> voVectors = vectors.stream()
                .map( v -> new DoubleVectorValueObject( v, eeVo, qtVo, badVo, adVo, null ).slice( subsetZero, badZero ) )
                .collect( Collectors.toList() );

        assertThat( experimentalDesignVisualizationService.sortVectorDataByDesign( voVectors, null ) )
                .hasEntrySatisfying( 2L, m -> assertThat( m )
                        .hasSize( 3 ) );
    }

    @Test
    @WithMockUser
    public void testOrderSingleCellData() {
        Random random = new Random( 123L );
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors();
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        ee.setId( 1L );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        List<Characteristic> cts = new ArrayList<>( 4 );
        for ( int i = 0; i < 4; i++ ) {
            cts.add( Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct" + i, null ) );
        }
        int[] indices = new int[dimension.getNumberOfCells()];
        for ( int i = 0; i < dimension.getNumberOfCells(); i++ ) {
            if ( random.nextDouble() < 0.1 ) {
                indices[i] = -1;
            } else {
                indices[i] = random.nextInt();
            }
        }
        ExpressionExperimentValueObject eeVo = new ExpressionExperimentValueObject( ee );
        CellLevelCharacteristics cta = CellLevelCharacteristics.Factory.newInstance( null, null, cts, indices );
        Collection<RawExpressionDataVector> aggregatedVectors = SingleCellDataVectorAggregatorUtils.aggregate( vectors, SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod.SUM, cta, false );
        QuantitationTypeValueObject qtVo = new QuantitationTypeValueObject( aggregatedVectors.iterator().next().getQuantitationType() );
        BioAssayDimension bad = aggregatedVectors.iterator().next().getBioAssayDimension();
        BioAssayDimensionValueObject badVo = new BioAssayDimensionValueObject( bad );
        ArrayDesignValueObject adVo = new ArrayDesignValueObject( aggregatedVectors.iterator().next().getDesignElement().getArrayDesign() );
        Collection<DoubleVectorValueObject> voVectors = aggregatedVectors.stream()
                .map( v -> new DoubleVectorValueObject( v, eeVo, qtVo, badVo, adVo, null ) )
                .collect( Collectors.toList() );

        when( expressionExperimentService.loadAndThawLiteOrFail( eq( ee.getId() ), any(), any() ) )
                .thenReturn( ee );
        // for single-cell subsets,
        when( expressionExperimentService.getBioAssayDimensions( ee ) ).thenReturn( Collections.emptySet() );
        when( expressionExperimentService.getBioAssayDimensions( ee ) ).thenReturn( Collections.singleton( bad ) );

        assertThat( experimentalDesignVisualizationService.sortVectorDataByDesign( voVectors, null ) )
                .containsKey( 1L );
        // RandomExperimentalDesignUtils.randomExperimentalDesign( ee );
        // RandomCellLevelCharacteristicsUtils.randomCellTypeAssignment( ee );
    }

    private final AtomicLong idGenerator = new AtomicLong( 0L );

    /**
     * Unfortunately, the visualization tool relies on FV IDs being assigned.
     */
    private ExperimentalFactor randomCategoricalFactor( ExpressionExperiment ee, String name, int numValues ) {
        ExperimentalFactor factor = RandomExperimentalDesignUtils.randomCategoricalFactor( ee, name, numValues );
        for ( FactorValue fv : factor.getFactorValues() ) {
            fv.setId( idGenerator.incrementAndGet() );
        }
        return factor;
    }
}
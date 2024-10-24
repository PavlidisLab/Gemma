package ubic.gemma.persistence.service.expression.experiment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ubic.gemma.persistence.service.expression.experiment.SingleCellTestUtils.randomSingleCellVectors;
import static ubic.gemma.persistence.util.ByteArrayUtils.byteArrayToDoubles;

@ContextConfiguration
public class SingleCellExpressionExperimentAggregatorServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public SingleCellExpressionExperimentAggregatorService singleCellExpressionExperimentAggregatorService() {
            return new SingleCellExpressionExperimentAggregatorServiceImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public BioAssayDimensionService bioAssayDimensionService() {
            return mock();
        }
    }

    @Autowired
    private SingleCellExpressionExperimentAggregatorService singleCellExpressionExperimentAggregatorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    private ExpressionExperiment ee;
    private ArrayDesign ad;
    private List<BioAssay> cellBAs;
    private Random random;

    @Before
    public void setUp() {
        SingleCellTestUtils.setSeed( 123 );
        random = new Random( 123 );
        ad = new ArrayDesign();
        ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs1" ) );
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ExperimentalDesign ed = new ExperimentalDesign();
        ee.setExperimentalDesign( ed );
        List<FactorValue> cellTypeFactors = new ArrayList<>();
        ExperimentalFactor ctf = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL, Categories.CELL_TYPE );
        for ( int i = 0; i < 4; i++ ) {
            cellTypeFactors.add( FactorValue.Factory.newInstance( ctf, Characteristic.Factory.newInstance( Categories.CELL_TYPE, "c" + i, null ) ) );
        }
        ctf.getFactorValues().addAll( cellTypeFactors );
        ed.getExperimentalFactors().add( ctf );
        cellBAs = new ArrayList<>();
        for ( int i = 0; i < 4; i++ ) {
            BioMaterial subject = BioMaterial.Factory.newInstance( "s" + i );
            BioAssay subjectBa = BioAssay.Factory.newInstance( "s" + i, ad, subject );
            subject.getBioAssaysUsedIn().add( subjectBa );
            ee.getBioAssays().add( subjectBa );
            for ( int j = 0; j < 4; j++ ) {
                BioMaterial cellPop = BioMaterial.Factory.newInstance( "s" + i + "c" + j );
                cellPop.setSourceBioMaterial( subject );
                cellPop.getFactorValues().add( cellTypeFactors.get( i ) );
                BioAssay cellPopBa = BioAssay.Factory.newInstance( "s" + i + "c" + j, ad, cellPop );
                cellPop.getBioAssaysUsedIn().add( cellPopBa );
                cellBAs.add( cellPopBa );
            }
        }
        when( expressionExperimentService.loadOrFail( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addRawDataVectors( any(), any(), any() ) ).thenAnswer( a -> a.getArgument( 2, Collection.class ).size() );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) ).thenReturn( Optional.of( ctf ) );
        when( bioAssayDimensionService.findOrCreate( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
    }

    @After
    public void resetMocks() {
        reset( expressionExperimentService );
    }

    @Test
    public void testCounts() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = new CellTypeAssignment();
        int[] indices = new int[dimension.getCellIds().size()];
        for ( int i = 0; i < dimension.getCellIds().size(); i++ ) {
            indices[i] = random.nextInt( 4 );
        }
        cta.setCellTypes( IntStream.range( 0, 4 )
                .mapToObj( i -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, "c" + i, null ) )
                .collect( Collectors.toList() ) );
        cta.setNumberOfCellTypes( 4 );
        cta.setCellTypeIndices( indices );
        cta.setPreferred( true );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectors( ee, qt, cellBAs, true );
        assertThat( newQt.getName() ).isEqualTo( "Counts aggregated by cell type" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using SUM." );
        ArgumentCaptor<Collection<RawExpressionDataVector>> capt = ArgumentCaptor.captor();
        verify( expressionExperimentService ).addRawDataVectors( eq( ee ), eq( newQt ), capt.capture() );
        assertThat( capt.getValue() )
                .hasSameSizeAs( ad.getCompositeSequences() )
                .anySatisfy( rawVec -> {
                    assertThat( rawVec.getDesignElement().getName() ).isEqualTo( "cs1" );
                    assertThat( rawVec.getBioAssayDimension().getName() )
                            .isEqualTo( "Bunch of test cells aggregated by cell type" );
                    assertThat( rawVec.getBioAssayDimension().getBioAssays() )
                            .hasSize( 4 * 4 );
                    assertThat( rawVec.getQuantitationType() ).isSameAs( newQt );
                    assertThat( byteArrayToDoubles( rawVec.getData() ) )
                            .hasSize( 16 )
                            .containsExactly( 165.0, 165.0, 165.0, 165.0, 161.0, 161.0, 161.0, 161.0, 124.0, 124.0, 124.0, 124.0, 0.0, 0.0, 0.0, 0.0 );
                } );
    }

    @Test
    public void testLogTransformedData() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "log2cpm" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = new CellTypeAssignment();
        int[] indices = new int[dimension.getCellIds().size()];
        for ( int i = 0; i < dimension.getCellIds().size(); i++ ) {
            indices[i] = random.nextInt( 4 );
        }
        cta.setCellTypes( IntStream.range( 0, 4 )
                .mapToObj( i -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, "c" + i, null ) )
                .collect( Collectors.toList() ) );
        cta.setNumberOfCellTypes( 4 );
        cta.setCellTypeIndices( indices );
        cta.setPreferred( true );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectors( ee, qt, cellBAs, true );
        assertThat( newQt.getName() ).isEqualTo( "log2cpm aggregated by cell type" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using LOG_SUM." );
        ArgumentCaptor<Collection<RawExpressionDataVector>> capt = ArgumentCaptor.captor();
        verify( expressionExperimentService ).addRawDataVectors( eq( ee ), eq( newQt ), capt.capture() );
        assertThat( capt.getValue() )
                .hasSameSizeAs( ad.getCompositeSequences() )
                .anySatisfy( rawVec -> {
                    assertThat( rawVec.getDesignElement().getName() ).isEqualTo( "cs1" );
                    assertThat( rawVec.getBioAssayDimension().getName() )
                            .isEqualTo( "Bunch of test cells aggregated by cell type" );
                    assertThat( rawVec.getBioAssayDimension().getBioAssays() )
                            .hasSize( 4 * 4 );
                    assertThat( rawVec.getQuantitationType() ).isSameAs( newQt );
                    assertThat( byteArrayToDoubles( rawVec.getData() ) )
                            .hasSize( 16 )
                            .containsExactly( 5.979489082617791, 5.979489082617791, 5.979489082617791, 5.979489082617791, 5.93240457783411, 5.93240457783411, 5.93240457783411, 5.93240457783411, 5.761516807620513, 5.761516807620513, 5.761516807620513, 5.761516807620513, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY );
                } );
    }

    @Test
    public void testLog1PData() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "log1p" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LOG1P );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = new CellTypeAssignment();
        int[] indices = new int[dimension.getCellIds().size()];
        for ( int i = 0; i < dimension.getCellIds().size(); i++ ) {
            indices[i] = random.nextInt( 4 );
        }
        cta.setCellTypes( IntStream.range( 0, 4 )
                .mapToObj( i -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, "c" + i, null ) )
                .collect( Collectors.toList() ) );
        cta.setNumberOfCellTypes( 4 );
        cta.setCellTypeIndices( indices );
        cta.setPreferred( true );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectors( ee, qt, cellBAs, true );
        assertThat( newQt.getName() ).isEqualTo( "log1p aggregated by cell type" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using LOG1P_SUM." );
        ArgumentCaptor<Collection<RawExpressionDataVector>> capt = ArgumentCaptor.captor();
        verify( expressionExperimentService ).addRawDataVectors( eq( ee ), eq( newQt ), capt.capture() );
        assertThat( capt.getValue() )
                .hasSameSizeAs( ad.getCompositeSequences() )
                .anySatisfy( rawVec -> {
                    assertThat( rawVec.getDesignElement().getName() ).isEqualTo( "cs1" );
                    assertThat( rawVec.getBioAssayDimension().getName() )
                            .isEqualTo( "Bunch of test cells aggregated by cell type" );
                    assertThat( rawVec.getBioAssayDimension().getBioAssays() )
                            .hasSize( 4 * 4 );
                    assertThat( rawVec.getQuantitationType() ).isSameAs( newQt );
                    assertThat( byteArrayToDoubles( rawVec.getData() ) )
                            .hasSize( 16 )
                            .containsExactly( 5.111987788356544, 5.111987788356544, 5.111987788356544, 5.111987788356544, 5.087596335232384, 5.087596335232384, 5.087596335232384, 5.087596335232384, 4.8283137373023015, 4.8283137373023015, 4.8283137373023015, 4.8283137373023015, 0.0, 0.0, 0.0, 0.0
                            );
                } );
    }

    @Test
    public void testUnsupportedTransformation() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.PERCENT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = new CellTypeAssignment();
        int[] indices = new int[dimension.getCellIds().size()];
        for ( int i = 0; i < dimension.getCellIds().size(); i++ ) {
            indices[i] = random.nextInt( 4 );
        }
        cta.setCellTypes( IntStream.range( 0, 4 )
                .mapToObj( i -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, "c" + i, null ) )
                .collect( Collectors.toList() ) );
        cta.setNumberOfCellTypes( 4 );
        cta.setCellTypeIndices( indices );
        cta.setPreferred( true );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        assertThatThrownBy( () -> singleCellExpressionExperimentAggregatorService.aggregateVectors( ee, qt, cellBAs, true ) )
                .isInstanceOf( UnsupportedOperationException.class )
                .hasMessage( "Unsupported scale type for aggregation: PERCENT" );
    }
}
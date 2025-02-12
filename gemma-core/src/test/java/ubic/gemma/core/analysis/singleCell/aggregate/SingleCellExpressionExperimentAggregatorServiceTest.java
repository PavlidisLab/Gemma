package ubic.gemma.core.analysis.singleCell.aggregate;

import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
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
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVectors;

@ContextConfiguration
public class SingleCellExpressionExperimentAggregatorServiceTest extends BaseTest {

    @Autowired
    private QuantitationTypeService quantitationTypeService;

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

        @Bean
        public BioAssayService bioAssayService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public SingleCellSparsityMetrics singleCellSparsityMetrics() {
            return new SingleCellSparsityMetrics();
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
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

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private AuditTrailService auditTrailService;

    private ExpressionExperiment ee;
    private ExperimentalFactor ctf;
    private ArrayDesign ad;
    private List<BioAssay> cellBAs;
    private Random random;

    @Before
    public void setUp() {
        RandomSingleCellDataUtils.setSeed( 123 );
        random = new Random( 123 );
        ad = new ArrayDesign();
        ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs1" ) );
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ExperimentalDesign ed = new ExperimentalDesign();
        ee.setExperimentalDesign( ed );
        List<FactorValue> cellTypeFactors = new ArrayList<>();
        ctf = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL, Categories.CELL_TYPE );
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
        when( expressionExperimentService.reload( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( expressionExperimentService.addRawDataVectors( any(), any(), any() ) ).thenAnswer( a -> a.getArgument( 2, Collection.class ).size() );
        when( quantitationTypeService.reload( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) ).thenReturn( Optional.of( ctf ) );
        when( bioAssayDimensionService.findOrCreate( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
    }

    @After
    public void resetMocks() {
        reset( expressionExperimentService, bioAssayDimensionService, bioAssayService, singleCellExpressionExperimentService, auditTrailService );
    }

    @Test
    public void testCounts() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // these are the actual library sizes, no adjustment is necessary
        long[] sourceLibrarySize = { 2236, 2392, 2260, 2460 };
        int i = 0;
        for ( BioAssay ba : dimension.getBioAssays() ) {
            ba.setSequenceReadCount( sourceLibrarySize[i++] );
            ba.setSequenceReadLength( 100 );
            ba.setSequencePairedReads( true );
        }
        // randomly assign cell types
        CellTypeAssignment cta = createCellTypeAssignment( dimension );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        AggregateConfig config = AggregateConfig.builder().makePreferred( true ).build();
        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectorsByCellType( ee, cellBAs, config );
        assertThat( newQt.getName() ).isEqualTo( "Counts aggregated by cell type (log2cpm)" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using SUM. The data was subsequently converted to log2cpm." );
        assertThat( newQt.getIsPreferred() ).isTrue();
        assertThat( newQt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( newQt.getScale() ).isEqualTo( ScaleType.LOG2 );

        verify( bioAssayDimensionService ).findOrCreate( any() );
        ArgumentCaptor<Collection<BioAssay>> capt2 = ArgumentCaptor.captor();
        verify( bioAssayService, times( 2 ) ).update( capt2.capture() );
        assertThat( capt2.getValue() )
                .hasSize( 16 )
                .satisfies( bas -> {
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfCells )
                            .containsExactly( 24, 24, 24, 24, 20, 20, 20, 20, 31, 31, 31, 31, 33, 33, 33, 33 );
                } )
                .satisfies( bas -> {
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfDesignElements )
                            .containsExactly( 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 );
                } )
                .satisfies( bas -> {
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfCellsByDesignElements )
                            .containsExactly( 24, 24, 24, 24, 20, 20, 20, 20, 31, 31, 31, 31, 33, 33, 33, 33 );
                } );
        ArgumentCaptor<Collection<RawExpressionDataVector>> capt = ArgumentCaptor.captor();
        verify( expressionExperimentService ).addRawDataVectors( eq( ee ), eq( newQt ), capt.capture() );
        assertThat( capt.getValue() )
                .hasSameSizeAs( ad.getCompositeSequences() )
                .satisfies( vecs -> {
                    // make sure the data is log2cpm
                    double total = 0;
                    for ( RawExpressionDataVector vec : vecs ) {
                        total += Math.pow( 2, vec.getDataAsDoubles()[0] );
                    }
                    // because the numerator (librarySize + 1) and numerical error from log/exp transformation, the
                    // offset will be pretty large. This is attenuated by large library sizes.
                    assertThat( total ).isEqualTo( 1e6, Offset.offset( 1e4 ) );
                } )
                .anySatisfy( rawVec -> {
                    assertThat( rawVec.getDesignElement().getName() ).isEqualTo( "cs1" );
                    assertThat( rawVec.getBioAssayDimension().getName() )
                            .isEqualTo( "Bunch of test cells aggregated by cell type" );
                    assertThat( rawVec.getBioAssayDimension().getBioAssays() )
                            .hasSize( 4 * 4 );
                    assertThat( rawVec.getQuantitationType() ).isSameAs( newQt );
                    assertThat( rawVec.getDataAsDoubles() )
                            .hasSize( 16 )
                            .containsExactly( 19.927216544784468, 19.927216544784468, 19.927216544784468, 19.927216544784468, 19.927108921133478, 19.927108921133478, 19.927108921133478, 19.927108921133478, 19.925786216730167, 19.925786216730167, 19.925786216730167, 19.925786216730167, 19.925544781696406, 19.925544781696406, 19.925544781696406, 19.925544781696406 );
                } );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any(), any( String.class ) );
    }

    @Test
    public void testCountWithAdjustedLibrarySize() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        long[] sourceLibrarySize = { ( long ) ( 1.1 * 2236 ), ( long ) ( 1.1 * 2392 ), ( long ) ( 1.1 * 2260 ), ( long ) ( 1.1 * 2460 ) };
        int i = 0;
        for ( BioAssay ba : dimension.getBioAssays() ) {
            ba.setSequenceReadCount( sourceLibrarySize[i++] );
            ba.setSequenceReadLength( 100 );
            ba.setSequencePairedReads( true );
        }
        for ( BioAssay ba : cellBAs ) {
            ba.setSequenceReadLength( 100 );
            ba.setSequencePairedReads( true );
        }
        // randomly assign cell types
        CellTypeAssignment cta = createCellTypeAssignment( dimension );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        AggregateConfig config = AggregateConfig.builder()
                .makePreferred( true )
                .adjustLibrarySizes( true )
                .build();
        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectorsByCellType( ee, cellBAs, config );
        assertThat( newQt.getName() ).isEqualTo( "Counts aggregated by cell type (log2cpm)" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using SUM. The data was subsequently converted to log2cpm." );
        assertThat( newQt.getIsPreferred() ).isTrue();
        assertThat( newQt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( newQt.getScale() ).isEqualTo( ScaleType.LOG2 );

        verify( bioAssayDimensionService ).findOrCreate( any() );
        ArgumentCaptor<Collection<BioAssay>> capt2 = ArgumentCaptor.captor();
        verify( bioAssayService, times( 2 ) ).update( capt2.capture() );
        assertThat( capt2.getValue() )
                .hasSize( 16 )
                .satisfies( bas -> {
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfCells )
                            .containsExactly( 24, 24, 24, 24, 20, 20, 20, 20, 31, 31, 31, 31, 33, 33, 33, 33 );
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfDesignElements )
                            .containsExactly( 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 );
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfCellsByDesignElements )
                            .containsExactly( 24, 24, 24, 24, 20, 20, 20, 20, 31, 31, 31, 31, 33, 33, 33, 33 );
                    assertThat( bas )
                            .extracting( BioAssay::getSequenceReadLength )
                            .containsOnly( 100 );
                    assertThat( bas )
                            .extracting( BioAssay::getSequencePairedReads )
                            .containsOnly( true );
                } );
        ArgumentCaptor<Collection<RawExpressionDataVector>> capt = ArgumentCaptor.captor();
        verify( expressionExperimentService ).addRawDataVectors( eq( ee ), eq( newQt ), capt.capture() );
        assertThat( capt.getValue() )
                .hasSameSizeAs( ad.getCompositeSequences() )
                .satisfies( vecs -> {
                    // make sure the data is log2cpm
                    double total = 0;
                    for ( RawExpressionDataVector vec : vecs ) {
                        total += Math.pow( 2, vec.getDataAsDoubles()[0] );
                    }
                    // because the numerator (librarySize + 1) and numerical error from log/exp transformation, the
                    // offset will be pretty large. This is attenuated by large library sizes.
                    // also, because we have about 10% of unaccounted reads, the total should reflect that
                    assertThat( total ).isEqualTo( 1e6 / 1.1, Offset.offset( 1e4 ) );
                } )
                .anySatisfy( rawVec -> {
                    assertThat( rawVec.getDesignElement().getName() ).isEqualTo( "cs1" );
                    assertThat( rawVec.getBioAssayDimension().getName() )
                            .isEqualTo( "Bunch of test cells aggregated by cell type" );
                    assertThat( rawVec.getBioAssayDimension().getBioAssays() )
                            .hasSize( 4 * 4 );
                    assertThat( rawVec.getQuantitationType() ).isSameAs( newQt );
                    assertThat( rawVec.getDataAsDoubles() )
                            .hasSize( 16 )
                            .containsExactly( 19.79085337048598, 19.79085337048598, 19.79085337048598, 19.79085337048598, 19.790524266814366, 19.790524266814366, 19.790524266814366, 19.790524266814366, 19.789332307461518, 19.789332307461518, 19.789332307461518, 19.789332307461518, 19.78913462294074, 19.78913462294074, 19.78913462294074, 19.78913462294074 );
                } );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any(), any( String.class ) );
    }

    @Test
    public void testLogTransformedData() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "log2cpm" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = createCellTypeAssignment( dimension );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        AggregateConfig config = AggregateConfig.builder().makePreferred( true ).build();
        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectorsByCellType( ee, cellBAs, config );
        assertThat( newQt.getName() ).isEqualTo( "log2cpm aggregated by cell type (log2cpm)" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using LOG_SUM. The data was subsequently converted to log2cpm." );
        assertThat( newQt.getIsPreferred() ).isTrue();
        assertThat( newQt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( newQt.getScale() ).isEqualTo( ScaleType.LOG2 );

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
                    assertThat( rawVec.getDataAsDoubles() )
                            .hasSize( 16 )
                            .containsExactly(
                                    19.929746930992494, 19.929746930992494, 19.929746930992494, 19.929746930992494,
                                    19.9296592830575, 19.9296592830575, 19.9296592830575, 19.9296592830575,
                                    19.929304310569375, 19.929304310569375, 19.929304310569375, 19.929304310569375,
                                    19.92901519935523, 19.92901519935523, 19.92901519935523, 19.92901519935523 );
                } );
        verify( bioAssayService, times( 2 ) ).update( anyCollection() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any(), any( String.class ) );
    }

    @Test
    public void testLog1PData() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "log1p" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LOG1P );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = createCellTypeAssignment( dimension );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        AggregateConfig config = AggregateConfig.builder().makePreferred( true ).build();
        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectorsByCellType( ee, cellBAs, config );
        assertThat( newQt.getName() ).isEqualTo( "log1p aggregated by cell type (log2cpm)" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using LOG1P_SUM. The data was subsequently converted to log2cpm." );
        assertThat( newQt.getIsPreferred() ).isTrue();
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
                    assertThat( rawVec.getDataAsDoubles() )
                            .hasSize( 16 )
                            .containsExactly(
                                    19.927216544784468, 19.927216544784468, 19.927216544784468, 19.927216544784468,
                                    19.927108921133478, 19.927108921133478, 19.927108921133478, 19.927108921133478,
                                    19.925786216730167, 19.925786216730167, 19.925786216730167, 19.925786216730167,
                                    19.925544781696406, 19.925544781696406, 19.925544781696406, 19.925544781696406 );
                } );
        verify( bioAssayService, times( 2 ) ).update( anyCollection() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any(), any( String.class ) );
    }

    @Test
    public void testAggregateToNonPreferredVectors() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "log1p" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LOG1P );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = createCellTypeAssignment( dimension );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        AggregateConfig config = AggregateConfig.builder().makePreferred( false ).build();
        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectorsByCellType( ee, cellBAs, config );
        assertThat( newQt.getName() ).isEqualTo( "log1p aggregated by cell type (log2cpm)" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using LOG1P_SUM. The data was subsequently converted to log2cpm." );
        assertThat( newQt.getIsPreferred() ).isFalse();
        assertThat( newQt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( newQt.getScale() ).isEqualTo( ScaleType.LOG2 );
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
                    assertThat( rawVec.getDataAsDoubles() )
                            .hasSize( 16 )
                            .containsExactly(
                                    19.927216544784468, 19.927216544784468, 19.927216544784468, 19.927216544784468,
                                    19.927108921133478, 19.927108921133478, 19.927108921133478, 19.927108921133478,
                                    19.925786216730167, 19.925786216730167, 19.925786216730167, 19.925786216730167,
                                    19.925544781696406, 19.925544781696406, 19.925544781696406, 19.925544781696406 );
                } );
        verify( bioAssayService ).update( anyCollection() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any(), any( String.class ) );
    }


    @Test
    public void testUnsupportedTransformation() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.PERCENT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = createCellTypeAssignment( dimension );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        AggregateConfig config = AggregateConfig.builder().makePreferred( true ).build();
        assertThatThrownBy( () -> singleCellExpressionExperimentAggregatorService.aggregateVectorsByCellType( ee, cellBAs, config ) )
                .isInstanceOf( UnsupportedScaleTypeForAggregationException.class )
                .hasMessage( "Unsupported scale type for aggregation: PERCENT" );

        verifyNoInteractions( bioAssayService );
        verifyNoInteractions( auditTrailService );
    }

    /**
     * This cover the case where a FV was removed by a curator.
     */
    @Test
    public void testAggregationWithUnmappedFactorValue() {
        // drop one factor value
        Statement cellTypeToRemove = Statement.Factory.newInstance( Characteristic.Factory.newInstance( Categories.CELL_TYPE, "c3", null ) );
        FactorValue factorValueToRemove = ctf.getFactorValues().stream()
                .filter( fv -> fv.getCharacteristics().contains( cellTypeToRemove ) )
                .findFirst()
                .orElseThrow( IllegalStateException::new );
        ctf.getFactorValues().remove( factorValueToRemove );
        cellBAs.removeIf( ba -> ba.getSampleUsed().getAllFactorValues().contains( factorValueToRemove ) );

        QuantitationType qt = new QuantitationType();
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> vectors = randomSingleCellVectors( ee, ad, qt );
        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();
        // randomly assign cell types
        CellTypeAssignment cta = createCellTypeAssignment( dimension );
        dimension.getCellTypeAssignments().add( cta );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) )
                .thenReturn( vectors );

        AggregateConfig config = AggregateConfig.builder().makePreferred( true ).build();
        QuantitationType newQt = singleCellExpressionExperimentAggregatorService.aggregateVectorsByCellType( ee, cellBAs, config );
        assertThat( newQt.getName() ).isEqualTo( "Counts aggregated by cell type (log2cpm)" );
        assertThat( newQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using SUM. The data was subsequently converted to log2cpm." );
        assertThat( newQt.getIsPreferred() ).isTrue();
        assertThat( newQt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( newQt.getScale() ).isEqualTo( ScaleType.LOG2 );

        verify( bioAssayDimensionService ).findOrCreate( any() );
        ArgumentCaptor<Collection<BioAssay>> capt2 = ArgumentCaptor.captor();
        verify( bioAssayService, times( 2 ) ).update( capt2.capture() );
        assertThat( capt2.getValue() )
                .hasSize( 12 )
                .satisfies( bas -> {
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfCells )
                            .containsExactly( 24, 24, 24, 24, 20, 20, 20, 20, 31, 31, 31, 31 );
                } )
                .satisfies( bas -> {
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfDesignElements )
                            .containsExactly( 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 );
                } )
                .satisfies( bas -> {
                    assertThat( bas )
                            .extracting( BioAssay::getNumberOfCellsByDesignElements )
                            .containsExactly( 24, 24, 24, 24, 20, 20, 20, 20, 31, 31, 31, 31 );
                } );
        ArgumentCaptor<Collection<RawExpressionDataVector>> capt = ArgumentCaptor.captor();
        verify( expressionExperimentService ).addRawDataVectors( eq( ee ), eq( newQt ), capt.capture() );
        assertThat( capt.getValue() )
                .hasSameSizeAs( ad.getCompositeSequences() )
                .satisfies( vecs -> {
                    // make sure the data is log2cpm
                    double total = 0;
                    for ( RawExpressionDataVector vec : vecs ) {
                        total += Math.pow( 2, vec.getDataAsDoubles()[0] );
                    }
                    // because the numerator (librarySize + 1) and numerical error from log/exp transformation, the
                    // offset will be pretty large. This is attenuated by large library sizes.
                    assertThat( total ).isEqualTo( 1e6, Offset.offset( 1e4 ) );
                } )
                .anySatisfy( rawVec -> {
                    assertThat( rawVec.getDesignElement().getName() ).isEqualTo( "cs1" );
                    assertThat( rawVec.getBioAssayDimension().getName() )
                            .isEqualTo( "Bunch of test cells aggregated by cell type" );
                    assertThat( rawVec.getBioAssayDimension().getBioAssays() )
                            .hasSize( 4 * 3 );
                    assertThat( rawVec.getQuantitationType() ).isSameAs( newQt );
                    assertThat( rawVec.getDataAsDoubles() )
                            .hasSize( 12 )
                            .containsExactly( 19.927216544784468, 19.927216544784468, 19.927216544784468, 19.927216544784468, 19.927108921133478, 19.927108921133478, 19.927108921133478, 19.927108921133478, 19.925786216730167, 19.925786216730167, 19.925786216730167, 19.925786216730167 );
                } );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any(), any( String.class ) );
    }

    private CellTypeAssignment createCellTypeAssignment( SingleCellDimension dimension ) {
        CellTypeAssignment cta = new CellTypeAssignment();
        int[] indices = new int[dimension.getNumberOfCells()];
        for ( int i = 0; i < indices.length; i++ ) {
            indices[i] = random.nextInt( 4 );
        }
        cta.setCellTypes( IntStream.range( 0, 4 )
                .mapToObj( i -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, "c" + i, null ) )
                .collect( Collectors.toList() ) );
        cta.setNumberOfCellTypes( 4 );
        cta.setCellTypeIndices( indices );
        cta.setNumberOfAssignedCells( indices.length );
        cta.setPreferred( true );
        return cta;
    }
}
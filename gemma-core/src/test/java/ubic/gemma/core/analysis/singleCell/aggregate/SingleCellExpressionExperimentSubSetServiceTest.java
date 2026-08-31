package ubic.gemma.core.analysis.singleCell.aggregate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetReadService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class SingleCellExpressionExperimentSubSetServiceTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class SingleCellExpressionExperimentSplitServiceTestContextConfiguration {

        @Bean
        public SingleCellExpressionExperimentSubSetService service() {
            return new SingleCellExpressionExperimentSubSetServiceImpl();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSubSetService expressionExperimentSubSetService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService() {
            return mock();
        }

        @Bean
        public BioAssayService bioAssayService() {
            return mock();
        }

        @Bean
        public BioMaterialService bioMaterialService() {
            return mock();
        }

        @Bean
        public SingleCellExpressionExperimentSubSetAuditService subSetAuditService() {
            return mock();
        }
    }

    @Autowired
    private SingleCellExpressionExperimentSubSetService service;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Autowired
    private ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService;

    /**
     * A second aggregation run must reuse the subsets the first run left behind.
     * <p>
     * Before this was guarded, every run took the create path unconditionally, so an experiment
     * aggregated N times ended up with N complete sets of subsets — and N sets of pseudo-bulk
     * assays and samples underneath them. On production that is 47,143 subset rows under 19,391
     * distinct names (4,136 parent experiments), the worst being 275 subsets over 45 cell types.
     * <p>
     * The two runs here share a mutable {@code persisted} list: {@code create} appends to it and
     * the read service hands it back, which is what the second run sees in the database. Removing
     * the reuse lookup takes the second run back to 20 subsets and 80 assays.
     */
    @Test
    public void testSecondRunReusesSubSetsInsteadOfCreatingASecondSet() {
        CellTypeAssignment cta = new CellTypeAssignment();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setName( "ee" );
        ExperimentalFactor cf = new ExperimentalFactor();
        cf.setType( FactorType.CATEGORICAL );
        ArrayDesign ad = new ArrayDesign();
        SingleCellDimension scd = new SingleCellDimension();
        for ( int i = 0; i < 4; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setName( "ba" + i );
            BioMaterial bm = new BioMaterial();
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
            scd.getBioAssays().add( ba );
        }
        for ( int i = 0; i < 10; i++ ) {
            Characteristic ct = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct" + i, null );
            cta.getCellTypes().add( ct );
            cf.getFactorValues().add( FactorValue.Factory.newInstance( cf, ct ) );
        }

        // what the store holds; the second run reads it back
        List<ExpressionExperimentSubSet> persisted = new ArrayList<>();
        List<BioAssay> createdAssays = new ArrayList<>();
        List<BioMaterial> createdSamples = new ArrayList<>();

        when( bioMaterialService.create( any( BioMaterial.class ) ) ).thenAnswer( a -> {
            BioMaterial bm = a.getArgument( 0 );
            createdSamples.add( bm );
            return bm;
        } );
        when( bioAssayService.create( any( BioAssay.class ) ) ).thenAnswer( a -> {
            BioAssay ba = a.getArgument( 0 );
            createdAssays.add( ba );
            return ba;
        } );
        when( expressionExperimentSubSetService.create( any( ExpressionExperimentSubSet.class ) ) ).thenAnswer( a -> {
            ExpressionExperimentSubSet subset = a.getArgument( 0 );
            persisted.add( subset );
            return subset;
        } );
        when( expressionExperimentSubSetReadService.getSubSetsWithBioAssays( ee ) ).thenReturn( persisted );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .thenReturn( Optional.of( cf ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee ) )
                .thenReturn( Optional.of( scd ) );

        SingleCellExperimentSubSetsCreationConfig config = SingleCellExperimentSubSetsCreationConfig.builder().build();

        List<ExpressionExperimentSubSet> firstRun = service.createSubSetsByCellType( ee, config );
        assertThat( firstRun ).hasSize( 10 );
        assertThat( persisted ).hasSize( 10 );
        assertThat( createdAssays ).hasSize( 40 );
        assertThat( createdSamples ).hasSize( 40 );

        List<ExpressionExperimentSubSet> secondRun = service.createSubSetsByCellType( ee, config );

        assertThat( secondRun ).hasSize( 10 );
        assertThat( persisted )
                .as( "the second run must not add a second set of subsets" )
                .hasSize( 10 );
        assertThat( createdAssays )
                .as( "the second run must not mint a second set of pseudo-bulk assays" )
                .hasSize( 40 );
        assertThat( createdSamples )
                .as( "the second run must not mint a second set of pseudo-bulk samples" )
                .hasSize( 40 );
        assertThat( secondRun )
                .as( "the second run must hand back the subsets the first run created" )
                .containsExactlyInAnyOrderElementsOf( firstRun );
    }

    /**
     * Reuse is refused when the existing subset does not cover the samples being aggregated —
     * aggregating over the wrong assays is worse than the duplicate row the reuse prevents.
     */
    @Test
    public void testReuseIsRefusedWhenTheExistingSubSetDoesNotCoverTheSamples() {
        CellTypeAssignment cta = new CellTypeAssignment();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setName( "ee" );
        ExperimentalFactor cf = new ExperimentalFactor();
        cf.setType( FactorType.CATEGORICAL );
        ArrayDesign ad = new ArrayDesign();
        SingleCellDimension scd = new SingleCellDimension();
        for ( int i = 0; i < 3; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setName( "ba" + i );
            BioMaterial bm = new BioMaterial();
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
            scd.getBioAssays().add( ba );
        }
        Characteristic ct = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct0", null );
        cta.getCellTypes().add( ct );
        cf.getFactorValues().add( FactorValue.Factory.newInstance( cf, ct ) );

        List<ExpressionExperimentSubSet> persisted = new ArrayList<>();
        when( bioMaterialService.create( any( BioMaterial.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( bioAssayService.create( any( BioAssay.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( expressionExperimentSubSetService.create( any( ExpressionExperimentSubSet.class ) ) ).thenAnswer( a -> {
            ExpressionExperimentSubSet subset = a.getArgument( 0 );
            persisted.add( subset );
            return subset;
        } );
        when( expressionExperimentSubSetReadService.getSubSetsWithBioAssays( ee ) ).thenReturn( persisted );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .thenReturn( Optional.of( cf ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee ) )
                .thenReturn( Optional.of( scd ) );

        SingleCellExperimentSubSetsCreationConfig config = SingleCellExperimentSubSetsCreationConfig.builder().build();
        assertThat( service.createSubSetsByCellType( ee, config ) ).hasSize( 1 );

        // a fourth sample turns up in the single-cell dimension, so the stored subset no longer covers it
        BioAssay extra = new BioAssay();
        extra.setArrayDesignUsed( ad );
        extra.setName( "ba3" );
        BioMaterial extraBm = new BioMaterial();
        extraBm.setName( "bm3" );
        extra.setSampleUsed( extraBm );
        extraBm.getBioAssaysUsedIn().add( extra );
        ee.getBioAssays().add( extra );
        scd.getBioAssays().add( extra );

        assertThatThrownBy( () -> service.createSubSetsByCellType( ee, config ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "cannot be reused" )
                .hasMessageContaining( "ba3 - ct0" );
        assertThat( persisted )
                .as( "a refused reuse must not leave a second subset behind" )
                .hasSize( 1 );
    }

    @Test
    public void test() {
        CellTypeAssignment cta = new CellTypeAssignment();
        ExpressionExperiment ee = new ExpressionExperiment();
        ExperimentalFactor cf = new ExperimentalFactor();
        cf.setType( FactorType.CATEGORICAL );
        ArrayDesign ad = new ArrayDesign();
        SingleCellDimension scd = new SingleCellDimension();
        // create 4 samples
        for ( int i = 0; i < 4; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setName( "ba" + i );
            ba.setSequencePairedReads( true );
            ba.setSequenceReadLength( 100 );
            ba.setSequenceReadCount( 10000L );
            BioMaterial bm = new BioMaterial();
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
            scd.getBioAssays().add( ba );
        }
        // create 10 cell types
        for ( int i = 0; i < 10; i++ ) {
            Characteristic ct = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct" + i, null );
            cta.getCellTypes().add( ct );
            cf.getFactorValues().add( FactorValue.Factory.newInstance( cf, ct ) );
        }
        // Single-cell dimension covers all four sample bioassays — none are dataless.
        when( bioMaterialService.create( any( BioMaterial.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( bioAssayService.create( any( BioAssay.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .thenReturn( Optional.of( cf ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee ) )
                .thenReturn( Optional.of( scd ) );
        when( expressionExperimentSubSetService.create( any( ExpressionExperimentSubSet.class ) ) )
                .thenAnswer( a -> a.getArgument( 0 ) );
        List<ExpressionExperimentSubSet> subsets = service.createSubSetsByCellType( ee, SingleCellExperimentSubSetsCreationConfig.builder().build() );
        assertThat( subsets )
                .hasSize( 10 )
                .allSatisfy( subset -> {
                    Characteristic cellType = subset.getCharacteristics().iterator().next();
                    String cellTypeName = cellType.getValue();
                    assertThat( subset.getSourceExperiment() ).isEqualTo( ee );
                    assertThat( subset.getName() ).isEqualTo( ee.getName() + " - " + cellTypeName );
                    assertThat( subset.getCharacteristics() )
                            .hasSize( 1 )
                            .first()
                            .satisfies( c -> {
                                assertThat( c.getCategory() ).isEqualTo( Categories.CELL_TYPE.getCategory() );
                                assertThat( c.getCategoryUri() ).isEqualTo( Categories.CELL_TYPE.getCategoryUri() );
                                assertThat( c.getValue() ).isEqualTo( cellTypeName );
                            } );
                    assertThat( subset.getBioAssays() )
                            .hasSize( 4 )
                            .allSatisfy( ba -> {
                                assertThat( ba.getName() )
                                        .matches( "ba\\d+ - " + cellTypeName );
                                assertThat( ba.getArrayDesignUsed() )
                                        .isEqualTo( ad );
                                assertThat( ba.getSequenceReadCount() ).isNull();
                                assertThat( ba.getSequenceReadLength() ).isEqualTo( 100 );
                                assertThat( ba.getSequencePairedReads() ).isTrue();
                                assertThat( ba.getSampleUsed() )
                                        .isNotNull()
                                        .satisfies( bm -> {
                                            assertThat( bm.getName() )
                                                    .matches( "bm\\d+ - " + cellTypeName );
                                            assertThat( bm.getBioAssaysUsedIn() )
                                                    .contains( ba );
                                            assertThat( bm.getCharacteristics() )
                                                    .hasSize( 1 )
                                                    .first()
                                                    .satisfies( c -> {
                                                        assertThat( c.getCategory() ).isEqualTo( Categories.CELL_TYPE.getCategory() );
                                                        assertThat( c.getCategoryUri() ).isEqualTo( Categories.CELL_TYPE.getCategoryUri() );
                                                        assertThat( c.getValue() ).isEqualTo( cellTypeName );
                                                    } );
                                        } );
                            } );
                } );
    }

    @Test
    public void testCreateSubSetsWhenMissingFactorValue() {
        CellTypeAssignment cta = new CellTypeAssignment();
        ExpressionExperiment ee = new ExpressionExperiment();
        ExperimentalFactor cf = new ExperimentalFactor();
        cf.setType( FactorType.CATEGORICAL );
        ArrayDesign ad = new ArrayDesign();
        SingleCellDimension scd = new SingleCellDimension();
        // create 4 samples
        for ( int i = 0; i < 4; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setName( "ba" + i );
            BioMaterial bm = new BioMaterial();
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
            scd.getBioAssays().add( ba );
        }
        // create 10 cell types
        for ( int i = 0; i < 10; i++ ) {
            Characteristic ct = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct" + i, null );
            cta.getCellTypes().add( ct );
            if ( i % 2 == 0 ) {
                cf.getFactorValues().add( FactorValue.Factory.newInstance( cf, ct ) );
            }
        }
        // Single-cell dimension covers all four sample bioassays — none are dataless.
        when( bioMaterialService.create( any( BioMaterial.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( bioAssayService.create( any( BioAssay.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .thenReturn( Optional.of( cf ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee ) )
                .thenReturn( Optional.of( scd ) );
        when( expressionExperimentSubSetService.create( any( ExpressionExperimentSubSet.class ) ) )
                .thenAnswer( a -> a.getArgument( 0 ) );
        SingleCellExperimentSubSetsCreationConfig config = SingleCellExperimentSubSetsCreationConfig.builder().ignoreUnmatchedCharacteristics( true ).build();
        List<ExpressionExperimentSubSet> subsets = service.createSubSetsByCellType( ee, config );
        assertThat( subsets )
                .hasSize( 5 )
                .allSatisfy( subset -> {
                    Characteristic cellType = subset.getCharacteristics().iterator().next();
                    String cellTypeName = cellType.getValue();
                    assertThat( subset.getSourceExperiment() ).isEqualTo( ee );
                    assertThat( subset.getName() ).isEqualTo( ee.getName() + " - " + cellTypeName );
                    assertThat( subset.getCharacteristics() )
                            .hasSize( 1 )
                            .first()
                            .satisfies( c -> {
                                assertThat( c.getCategory() ).isEqualTo( Categories.CELL_TYPE.getCategory() );
                                assertThat( c.getCategoryUri() ).isEqualTo( Categories.CELL_TYPE.getCategoryUri() );
                                assertThat( c.getValue() ).isEqualTo( cellTypeName );
                            } );
                    assertThat( subset.getBioAssays() )
                            .hasSize( 4 )
                            .allSatisfy( ba -> {
                                assertThat( ba.getName() )
                                        .matches( "ba\\d+ - " + cellTypeName );
                                assertThat( ba.getArrayDesignUsed() )
                                        .isEqualTo( ad );
                                assertThat( ba.getSampleUsed() )
                                        .isNotNull()
                                        .satisfies( bm -> {
                                            assertThat( bm.getName() )
                                                    .matches( "bm\\d+ - " + cellTypeName );
                                            assertThat( bm.getBioAssaysUsedIn() )
                                                    .contains( ba );
                                            assertThat( bm.getCharacteristics() )
                                                    .hasSize( 1 )
                                                    .first()
                                                    .satisfies( c -> {
                                                        assertThat( c.getCategory() ).isEqualTo( Categories.CELL_TYPE.getCategory() );
                                                        assertThat( c.getCategoryUri() ).isEqualTo( Categories.CELL_TYPE.getCategoryUri() );
                                                        assertThat( c.getValue() ).isEqualTo( cellTypeName );
                                                    } );
                                        } );
                            } );
                } );
    }

    @Test
    public void testCreateSubSetsSkipsSamplesNotInSingleCellDimension() {
        CellTypeAssignment cta = new CellTypeAssignment();
        ExpressionExperiment ee = new ExpressionExperiment();
        ExperimentalFactor cf = new ExperimentalFactor();
        cf.setType( FactorType.CATEGORICAL );
        ArrayDesign ad = new ArrayDesign();
        // create 4 samples; keep them in a list so SCD membership is deterministic
        // (ee.getBioAssays() is a HashSet so its iteration order is not insertion order).
        List<BioAssay> samples = new java.util.ArrayList<>( 4 );
        for ( int i = 0; i < 4; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setName( "ba" + i );
            BioMaterial bm = new BioMaterial();
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
            samples.add( ba );
        }
        // create 3 cell types
        for ( int i = 0; i < 3; i++ ) {
            Characteristic ct = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct" + i, null );
            cta.getCellTypes().add( ct );
            cf.getFactorValues().add( FactorValue.Factory.newInstance( cf, ct ) );
        }
        // SCD covers only ba0/ba1/ba2; ba3 contributed no cells and must be skipped.
        SingleCellDimension scd = new SingleCellDimension();
        scd.getBioAssays().addAll( samples.subList( 0, 3 ) );
        BioAssay datalessSample = samples.get( 3 );
        when( bioMaterialService.create( any( BioMaterial.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( bioAssayService.create( any( BioAssay.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( Optional.of( cta ) );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .thenReturn( Optional.of( cf ) );
        when( singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee ) )
                .thenReturn( Optional.of( scd ) );
        when( expressionExperimentSubSetService.create( any( ExpressionExperimentSubSet.class ) ) )
                .thenAnswer( a -> a.getArgument( 0 ) );

        List<ExpressionExperimentSubSet> subsets = service.createSubSetsByCellType( ee, SingleCellExperimentSubSetsCreationConfig.builder().build() );

        assertThat( subsets )
                .hasSize( 3 )
                .allSatisfy( subset -> {
                    String cellTypeName = subset.getCharacteristics().iterator().next().getValue();
                    assertThat( subset.getBioAssays() )
                            .hasSize( 3 )
                            .extracting( BioAssay::getName )
                            .containsExactlyInAnyOrder(
                                    "ba0 - " + cellTypeName,
                                    "ba1 - " + cellTypeName,
                                    "ba2 - " + cellTypeName )
                            .doesNotContain( datalessSample.getName() + " - " + cellTypeName );
                } );
    }
}
package ubic.gemma.persistence.service.expression.experiment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellAggregationConfig;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExperimentSubSetsCreationConfig;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentAggregateService;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentSubSetService;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVectors;

public class SingleCellIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private BioAssayService bioAssayService;
    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private SingleCellExpressionExperimentSubSetService singleCellExpressionExperimentSubSetService;
    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    @Autowired
    private SingleCellExpressionExperimentAggregateService singleCellExpressionExperimentAggregateService;

    @Autowired
    private PersistentDummyObjectHelper helper;

    private ArrayDesign ad;
    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        ad = helper.getTestPersistentArrayDesign( 100, true, false );
        ee = helper.getTestPersistentBasicExpressionExperiment( ad );
    }

    @After
    public void tearDown() {
        // redundant for the current test
        // if ( ee != null ) {
        //     expressionExperimentService.remove( ee );
        // }
        // if ( ad != null ) {
        //     arrayDesignService.remove( ad );
        // }
    }

    @Test
    public void test() {
        // possibly replace it with getTestPersistentSingleCellExpressionExperiment. creation of qt and cell type assignments
        // are repeated here
        Random random = new Random( 123L );
        QuantitationType qt = new QuantitationType();
        qt.setName( "counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.COUNT );
        qt.setIsSingleCellPreferred( true );
        singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, randomSingleCellVectors( ee, ad, qt ), null, true, false );

        SingleCellDimension scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithCellLevelCharacteristics( ee )
                .orElse( null );
        assertThat( scd ).isNotNull();
        assertThat( scd.getCellIds() ).isNotNull().hasSize( 8000 );
        assertThat( scd.getNumberOfCellIds() ).isEqualTo( 8000 );

        // at some point, we add single cell labels
        List<String> labels = new ArrayList<>( scd.getNumberOfCellIds() );
        for ( int i = 0; i < 8000; i++ ) {
            labels.add( String.valueOf( "ABCD".charAt( random.nextInt( 4 ) ) ) );
        }
        CellTypeAssignment cta = singleCellExpressionExperimentService.relabelCellTypes( ee, qt, scd, labels, null, null, true, false );
        assertThat( cta.getNumberOfCellTypes() ).isEqualTo( 4 );
        assertThat( cta.getNumberOfAssignedCells() ).isEqualTo( 8000 );
        assertThat( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .isNotNull();

        List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSubSetService.createSubSetsByCellType( ee, SingleCellExperimentSubSetsCreationConfig.builder().build() );

        // one for each cell type and subject
        assertThat( subsets )
                .hasSize( 4 )
                .allSatisfy( subset -> {
                    assertThat( subset.getCharacteristics() )
                            .hasSize( 1 )
                            .first()
                            .satisfies( c -> {
                                assertThat( c.getCategory() ).isEqualTo( Categories.CELL_TYPE.getCategory() );
                                assertThat( c.getCategoryUri() ).isEqualTo( Categories.CELL_TYPE.getCategoryUri() );
                            } );
                    assertThat( subset.getBioAssays() ).hasSize( 8 );
                } );

        List<BioAssay> cellBAs = new ArrayList<>( subsets.get( 0 ).getBioAssays() );
        for ( ExpressionExperimentSubSet subset : subsets ) {
            cellBAs.addAll( subset.getBioAssays() );
        }
        SingleCellAggregationConfig config = SingleCellAggregationConfig.builder().makePreferred( true ).build();
        QuantitationType aggregatedQt = singleCellExpressionExperimentAggregateService.aggregateVectorsByCellType( ee, cellBAs, config );

        assertThat( aggregatedQt.getName() ).isEqualTo( "counts aggregated by cell type (log2cpm)" );
        assertThat( aggregatedQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using SUM. The data was subsequently converted to log2cpm." );
        assertThat( aggregatedQt.getIsPreferred() ).isTrue();

        Collection<RawExpressionDataVector> vectors = expressionExperimentService.getRawDataVectors( ee, aggregatedQt );
        assertThat( vectors )
                .hasSize( ad.getCompositeSequences().size() )
                .allSatisfy( vec -> {
                    assertThat( vec.getExpressionExperiment().getId() ).isEqualTo( ee.getId() );
                    assertThat( vec.getBioAssayDimension().getBioAssays() ).isEqualTo( cellBAs );
                    assertThat( vec.getQuantitationType() ).isEqualTo( aggregatedQt );
                } );

        List<BioAssay> subAssays = subsets.stream()
                .flatMap(subset -> subset.getBioAssays().stream())
                .collect(Collectors.toList());

        List<Long> subAssayIds = subAssays.stream().map( BioAssay::getId ).collect(Collectors.toList());

        List<Long> dimIds = subAssays.stream()
                .flatMap( ba -> bioAssayService.findBioAssayDimensions( ba ).stream() )
                .map( BioAssayDimension::getId )
                .collect( Collectors.toList() );

        List<Long> assayIds = ee.getBioAssays().stream().map( BioAssay::getId ).collect( Collectors.<Long>toList() );
        List<Long> subsetIds = subsets.stream().map( ExpressionExperimentSubSet::getId ).collect( Collectors.<Long>toList() );



        assertNotNull( expressionExperimentService.load( ee.getId() ) );
        expressionExperimentService.remove( ee );
        assertNull( expressionExperimentService.load( ee.getId() ) );

        for ( Long id : assayIds ) {
            BioAssay ba = bioAssayService.load( id );
            assertNull( ba );
        }

        for ( Long id : subAssayIds ) {
            BioAssay ba = bioAssayService.load( id );
            assertNull( ba );
        }

        for ( Long id: dimIds ) {
            BioAssayDimension ba = bioAssayDimensionService.load( id );
            assertNull( ba );
        }

        for (Long id: subsetIds) {
            assertNull ( expressionExperimentSubSetService.load( id ) );
        }

    }
}

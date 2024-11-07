package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVectors;

public class SingleCellIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private SingleCellExpressionExperimentSplitService singleCellExpressionExperimentSplitService;
    @Autowired
    private SingleCellExpressionExperimentAggregatorService singleCellExpressionExperimentAggregatorService;

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
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
        if ( ad != null ) {
            arrayDesignService.remove( ad );
        }
    }

    @Test
    public void test() {
        QuantitationType qt = new QuantitationType();
        qt.setName( "counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.COUNT );
        qt.setIsSingleCellPreferred( true );
        singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, randomSingleCellVectors( ee, ad, qt ), null );

        SingleCellDimension scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithCellLevelCharacteristics( ee )
                .orElse( null );
        assertThat( scd ).isNotNull();
        assertThat( scd.getCellIds() ).hasSize( 8000 );
        assertThat( scd.getNumberOfCells() ).isEqualTo( 8000 );

        // at some point, we add single cell labels
        List<String> labels = new ArrayList<>( scd.getNumberOfCells() );
        for ( int i = 0; i < 8000; i++ ) {
            labels.add( String.valueOf( "ABCD".charAt( RandomUtils.nextInt( 4 ) ) ) );
        }
        singleCellExpressionExperimentService.relabelCellTypes( ee, scd, labels, null, null );
        assertThat( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .isNotNull();

        List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSplitService.splitByCellType( ee );

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
        QuantitationType aggregatedQt = singleCellExpressionExperimentAggregatorService.aggregateVectors( ee, qt, cellBAs, true );

        assertThat( aggregatedQt.getName() ).isEqualTo( "counts aggregated by cell type (log2cpm)" );
        assertThat( aggregatedQt.getDescription() ).isEqualTo( "Expression data has been aggregated by cell type using SUM. The data was subsequently converted to log2cpm." );
        assertThat( aggregatedQt.getIsPreferred() ).isTrue();

        Collection<RawExpressionDataVector> vectors = expressionExperimentService.getRawDataVectors( ee, aggregatedQt );
        assertThat( vectors )
                .hasSize( ad.getCompositeSequences().size() )
                .allSatisfy( vec -> {
                    assertThat( vec.getExpressionExperiment().getId() ).isEqualTo( ee.getId() );
                    assertThat( vec.getBioAssayDimension().getName() ).isEqualTo( "Bunch of test cells aggregated by cell type" );
                    assertThat( vec.getBioAssayDimension().getDescription() ).isNull();
                    assertThat( vec.getBioAssayDimension().getBioAssays() ).isEqualTo( cellBAs );
                    assertThat( vec.getQuantitationType() ).isEqualTo( aggregatedQt );
                } );
    }
}
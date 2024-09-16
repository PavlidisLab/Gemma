package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleCellIntegrationTest extends BaseIntegrationTest {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

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
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.COUNT );
        qt.setIsSingleCellPreferred( true );
        singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, randomSingleCellVectors( ee, ad, qt ) );

        SingleCellDimension scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithCellTypeAssignments( ee );
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
        QuantitationType aggregatedQt = singleCellExpressionExperimentAggregatorService.aggregateVectors( ee, cellBAs );
    }

    private Collection<SingleCellExpressionDataVector> randomSingleCellVectors( ExpressionExperiment ee, ArrayDesign ad, QuantitationType qt ) {
        List<BioAssay> samples = new ArrayList<>( ee.getBioAssays() );
        int numCells = 1000 * ee.getBioAssays().size();
        SingleCellDimension dimension = new SingleCellDimension();
        dimension.setCellIds( IntStream.rangeClosed( 1, numCells ).mapToObj( Integer::toString ).collect( Collectors.toList() ) );
        dimension.setNumberOfCells( numCells );
        dimension.setBioAssays( samples );
        int[] offsets = new int[samples.size()];
        for ( int i = 0; i < samples.size(); i++ ) {
            offsets[i] = i * 1000;
        }
        dimension.setBioAssaysOffset( offsets );
        Collection<SingleCellExpressionDataVector> results = new ArrayList<>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
            vector.setDesignElement( cs );
            vector.setQuantitationType( qt );
            vector.setSingleCellDimension( dimension );
            // 10% sparsity
            int N = 100;
            double[] X = new double[N];
            int[] IX = new int[X.length];
            for ( int i = 0; i < N; i++ ) {
                X[i] = RandomUtils.nextInt( 100000 );
                IX[i] = ( 100 * i ) + RandomUtils.nextInt( 100 );
            }
            vector.setData( byteArrayConverter.doubleArrayToBytes( X ) );
            vector.setDataIndices( IX );
            results.add( vector );
        }
        return results;
    }
}
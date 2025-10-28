package ubic.gemma.core.analysis.singleCell;

import org.junit.Test;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.util.UninitializedList;
import ubic.gemma.model.util.UninitializedSet;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleCellSlicerUtilsTest {

    @Test
    public void testSlice() {
        RandomSingleCellDataUtils.setSeed( 123L );
        List<SingleCellExpressionDataVector> vecs = RandomSingleCellDataUtils.randomSingleCellVectors();
        List<BioAssay> assays = Arrays.asList( vecs.iterator().next().getSingleCellDimension().getBioAssays().get( 1 ),
                vecs.iterator().next().getSingleCellDimension().getBioAssays().get( 3 ) );
        assertThat( SingleCellSlicerUtils.slice( vecs, assays ) )
                .hasSize( 100 )
                .allSatisfy( vec -> {
                    assertThat( vec.getExpressionExperiment() ).isNotNull();
                    assertThat( vec.getSingleCellDimension().getCellIds() ).hasSize( 2000 );
                    assertThat( vec.getSingleCellDimension().getNumberOfCellIds() ).isEqualTo( 2000 );
                    assertThat( vec.getSingleCellDimension().getBioAssays() ).containsExactlyElementsOf( assays );
                    assertThat( vec.getDataAsDoubles() ).hasSize( 200 );
                    assertThat( vec.getDataIndices() )
                            .hasSize( 200 )
                            .isSorted();
                    assertThat( vec.getDataIndices()[0] ).isGreaterThanOrEqualTo( 0 );
                    assertThat( vec.getDataIndices()[199] ).isLessThan( 2000 );
                } );
    }

    @Test
    public void testSlicePartiallyInitializedDimension() {
        RandomSingleCellDataUtils.setSeed( 123L );
        List<SingleCellExpressionDataVector> vecs = RandomSingleCellDataUtils.randomSingleCellVectors();
        SingleCellDimension dimension = vecs.iterator().next().getSingleCellDimension();
        dimension.setCellIds( new UninitializedList<>( dimension.getNumberOfCellIds() ) );
        dimension.setCellTypeAssignments( new UninitializedSet<>() );
        dimension.setCellLevelCharacteristics( new UninitializedSet<>() );
        List<BioAssay> assays = Arrays.asList( dimension.getBioAssays().get( 1 ),
                dimension.getBioAssays().get( 3 ) );
        assertThat( SingleCellSlicerUtils.slice( vecs, assays ) )
                .hasSize( 100 )
                .allSatisfy( vec -> {
                    assertThat( vec.getExpressionExperiment() ).isNotNull();
                    assertThat( vec.getSingleCellDimension().getCellIds() )
                            .isInstanceOf( UninitializedList.class )
                            .hasSize( 2000 );
                    assertThat( vec.getSingleCellDimension().getCellTypeAssignments() )
                            .isInstanceOf( UninitializedSet.class );
                    assertThat( vec.getSingleCellDimension().getCellLevelCharacteristics() )
                            .isInstanceOf( UninitializedSet.class );
                    assertThat( vec.getSingleCellDimension().getNumberOfCellIds() ).isEqualTo( 2000 );
                    assertThat( vec.getSingleCellDimension().getBioAssays() ).containsExactlyElementsOf( assays );
                    assertThat( vec.getDataAsDoubles() ).hasSize( 200 );
                    assertThat( vec.getDataIndices() )
                            .hasSize( 200 )
                            .isSorted();
                    assertThat( vec.getDataIndices()[0] ).isGreaterThanOrEqualTo( 0 );
                    assertThat( vec.getDataIndices()[199] ).isLessThan( 2000 );
                } );
    }
}
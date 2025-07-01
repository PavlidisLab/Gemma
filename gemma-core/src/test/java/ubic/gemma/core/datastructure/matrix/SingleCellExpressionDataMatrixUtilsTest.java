package ubic.gemma.core.datastructure.matrix;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleCellExpressionDataMatrixUtilsTest {

    @Test
    public void testDoubleMatrixToVectors() {
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors();
        SingleCellExpressionDataDoubleMatrix matrix = new SingleCellExpressionDataDoubleMatrix( vectors );
        List<SingleCellExpressionDataVector> convertedVectors = SingleCellExpressionDataMatrixUtils.toVectors( matrix );
        assertThat( convertedVectors )
                .zipSatisfy( vectors, ( vec1, vec2 ) -> {
                    assertThat( vec1 ).isEqualTo( vec2 );
                    assertThat( vec1.getData() ).isEqualTo( vec2.getData() );
                    assertThat( vec1.getDataIndices() ).isEqualTo( vec2.getDataIndices() );
                } );
    }

    @Test
    public void testIntMatrixToVectors() {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.INT );
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors( qt );
        SingleCellExpressionDataIntMatrix matrix = new SingleCellExpressionDataIntMatrix( vectors );
        List<SingleCellExpressionDataVector> convertedVectors = SingleCellExpressionDataMatrixUtils.toVectors( matrix );
        assertThat( convertedVectors )
                .zipSatisfy( vectors, ( vec1, vec2 ) -> {
                    assertThat( vec1 ).isEqualTo( vec2 );
                    assertThat( vec1.getData() ).isEqualTo( vec2.getData() );
                    assertThat( vec1.getDataIndices() ).isEqualTo( vec2.getDataIndices() );
                } );
    }
}
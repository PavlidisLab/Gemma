package ubic.gemma.core.datastructure.matrix;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVectors;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.setSeed;

public class SingleCellExpressionDataDoubleMatrixTest {

    @Test
    public void test() {
        List<SingleCellExpressionDataVector> vecs = randomSingleCellVectors();
        SingleCellExpressionDataDoubleMatrix mat = new SingleCellExpressionDataDoubleMatrix( vecs );
        assertThat( mat.getBioAssays() )
                .hasSize( 4000 );
        assertThat( mat.getCellIds() )
                .hasSize( 4000 );
        assertThat( mat.getCellIds() )
                .hasSize( 4000 );
        assertThat( mat.getCellIdForColumn( 0 ) )
                .isEqualTo( "1" );
        assertThat( mat.getCellIdForColumn( 1231 ) )
                .isEqualTo( "1232" );
        assertThat( mat.getBioAssayForColumn( 1231 ) )
                .extracting( BioAssay::getName )
                .isEqualTo( "ba1" );
    }

    /**
     * On a log-scale, zero is mapped to negative infinity.
     */
    @Test
    public void testLog() {
        setSeed( 123 );
        List<SingleCellExpressionDataVector> vecs = randomSingleCellVectors( 100, 4, 1000, 0.9, ScaleType.LOG2 );
        SingleCellExpressionDataDoubleMatrix mat = new SingleCellExpressionDataDoubleMatrix( vecs );
        assertThat( mat.get( 1, 2 ) ).isNegative().isInfinite();
        assertThat( mat.getColumn( 2 ) )
                .startsWith(
                        3.5849625007211565,
                        Double.NEGATIVE_INFINITY,
                        0.0,
                        Double.NEGATIVE_INFINITY,
                        Double.NEGATIVE_INFINITY,
                        Double.NEGATIVE_INFINITY,
                        Double.NEGATIVE_INFINITY,
                        Double.NEGATIVE_INFINITY );
    }

    /**
     * In log1p, 0 is mapped back to 0.
     */
    @Test
    public void testLog1p() {
        setSeed( 123 );
        List<SingleCellExpressionDataVector> vecs = randomSingleCellVectors( 100, 4, 1000, 0.9, ScaleType.LOG1P );
        SingleCellExpressionDataDoubleMatrix mat = new SingleCellExpressionDataDoubleMatrix( vecs );
        assertThat( mat.get( 1, 2 ) ).isZero();
        assertThat( mat.getColumn( 2 ) )
                .startsWith(
                        2.5649493574615367,
                        0.0,
                        0.6931471805599453,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0 );
    }
}
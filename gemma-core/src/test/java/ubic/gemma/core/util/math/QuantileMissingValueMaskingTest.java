package ubic.gemma.core.util.math;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which cells come back missing from quantile normalization.
 * <p>
 * A missing cell is imputed with its row mean so it can take part in the ranking, then re-masked at the end. The
 * record of which cells to re-mask is a {@link java.util.BitSet} over {@code row * columns + column} rather than a
 * same-shaped matrix, and an index that is off — transposed, or using the wrong dimension as the divisor — puts
 * the NaNs back in the wrong places while every other value stays correct. These tests are what would catch that.
 */
public class QuantileMissingValueMaskingTest {

    /**
     * 🛑 Non-square and asymmetric on purpose.
     * <p>
     * On a square matrix a transposed index is invisible, and with a whole column missing — the case
     * {@code QuantileReferenceColumnsTest} exercises — any row-major/column-major mix-up still lands inside the
     * same column. Neither shape can fail. Three rows by five columns, with the two missing cells in different
     * rows AND different columns, is the smallest thing that can.
     */
    @Test
    public void missingCellsAreRemaskedInTheirOwnPositions() {
        DoubleMatrix<String, String> m = matrix( new double[][] {
                { 1.0, 2.0, 3.0, 4.0, Double.NaN },
                { 5.0, 6.0, 7.0, 8.0, 9.0 },
                { 10.0, Double.NaN, 12.0, 13.0, 14.0 } } );

        DoubleMatrix<String, String> out = new MatrixNormalizer<String, String>().quantileNormalize( m );

        for ( int i = 0; i < 3; i++ ) {
            for ( int j = 0; j < 5; j++ ) {
                boolean shouldBeMissing = ( i == 0 && j == 4 ) || ( i == 2 && j == 1 );
                assertThat( Double.isNaN( out.get( i, j ) ) )
                        .as( "cell (%d,%d) missing", i, j )
                        .isEqualTo( shouldBeMissing );
            }
        }
    }

    /**
     * Data with nothing missing comes back with nothing missing. The empty case is worth pinning separately
     * because it is what the whole corpus looks like — the raw vectors measured on GSE260875 have no missing
     * values in any of their three quantitation types — so a masking bug that only fires on a set bit would
     * never show up in production and would still be wrong.
     */
    @Test
    public void completeDataComesBackComplete() {
        DoubleMatrix<String, String> m = matrix( new double[][] {
                { 1.0, 2.0, 3.0 },
                { 4.0, 5.0, 6.0 },
                { 7.0, 8.0, 9.0 },
                { 10.0, 11.0, 12.0 } } );

        DoubleMatrix<String, String> out = new MatrixNormalizer<String, String>().quantileNormalize( m );

        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 3; j++ ) {
                assertThat( out.get( i, j ) ).as( "cell (%d,%d)", i, j ).isNotNaN();
            }
        }
    }

    /**
     * A row that is missing everywhere is dropped rather than returned as a row of NaN — {@code RowMissingFilter}
     * with {@code minPresentCount=1} removes it before anything else runs, so the result is shorter than the
     * input and the surviving rows keep their names. The masking has to be indexed on the FILTERED shape; using
     * the input's column or row count would run off the end or mask the wrong cells.
     */
    @Test
    public void anAllMissingRowIsDroppedAndTheRestKeepTheirNames() {
        DoubleMatrix<String, String> m = matrix( new double[][] {
                { 1.0, 2.0, 3.0, 4.0 },
                { Double.NaN, Double.NaN, Double.NaN, Double.NaN },
                { 5.0, 6.0, 7.0, Double.NaN } } );

        DoubleMatrix<String, String> out = new MatrixNormalizer<String, String>().quantileNormalize( m );

        assertThat( out.rows() ).isEqualTo( 2 );
        assertThat( out.getRowNames() ).containsExactly( "r0", "r2" );
        // r2's own missing cell is still masked, in its position within the shortened matrix
        assertThat( Double.isNaN( out.get( 1, 3 ) ) ).isTrue();
        assertThat( Double.isNaN( out.get( 1, 0 ) ) ).isFalse();
        assertThat( Double.isNaN( out.get( 0, 3 ) ) ).isFalse();
    }

    private static DoubleMatrix<String, String> matrix( double[][] values ) {
        DoubleMatrix<String, String> m = new DenseDoubleMatrix<>( values.length, values[0].length );
        for ( int i = 0; i < values.length; i++ ) {
            m.setRowName( "r" + i, i );
            for ( int j = 0; j < values[i].length; j++ ) {
                m.set( i, j, values[i][j] );
            }
        }
        for ( int j = 0; j < values[0].length; j++ ) {
            m.setColumnName( "c" + j, j );
        }
        return m;
    }
}

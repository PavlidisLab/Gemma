package ubic.gemma.core.util.math;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Which columns define the quantile reference distribution.
 * <p>
 * Quantile normalization maps every column onto one shared distribution, so a column that contributes to that
 * distribution changes the values of every other column. Outlier assays must not get that vote — and the way they
 * were kept out of it (blanking their values) does not actually work, which is what these tests pin.
 */
public class QuantileReferenceColumnsTest {

    /**
     * 🛑 The trap this whole change exists for: blanking a column does NOT keep it out of the reference.
     * <p>
     * {@code imputeMissing} fills a missing cell with its ROW MEAN, so an all-NaN column arrives at the reference
     * computation as a synthetic average sample and pulls the distribution toward the centre. Naming it excluded
     * is what actually removes its vote — and the two give different answers, which is the proof.
     * <p>
     * ⚠️ The columns below deliberately rank their rows in DIFFERENT orders. When every column agrees on the
     * ordering, the imputed row-mean column sorts to exactly the rank means and has no effect at all — a first
     * draft of this test used such data and could not fail. Real samples never agree that precisely, but the
     * degenerate case is worth knowing about before reading too much into a small effect.
     */
    @Test
    public void aBlankedColumnStillVotesOnTheReferenceUnlessExcluded() {
        double[][] data = new double[][] {
                { 10.0, 1.0, 5.0, Double.NaN },
                { 1.0, 10.0, 6.0, Double.NaN },
                { 5.0, 6.0, 1.0, Double.NaN },
                { 6.0, 5.0, 10.0, Double.NaN } };
        DoubleMatrix<String, String> withBlank = matrix( data );
        DoubleMatrix<String, String> alsoWithBlank = matrix( data );

        DoubleMatrix<String, String> blankVotes = new MatrixNormalizer<String, String>()
                .quantileNormalize( withBlank );
        DoubleMatrix<String, String> blankExcluded = new MatrixNormalizer<String, String>()
                .quantileNormalize( alsoWithBlank, new boolean[] { true, true, true, false } );

        boolean anyDifference = false;
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 3; j++ ) {
                if ( Math.abs( blankVotes.get( i, j ) - blankExcluded.get( i, j ) ) > 1e-9 ) {
                    anyDifference = true;
                }
            }
        }
        assertThat( anyDifference )
                .as( "a blanked column changes the other columns' normalized values, so blanking is not exclusion" )
                .isTrue();
    }

    /**
     * The excluded column is still placed on the shared scale — it just does not get to define it. Otherwise the
     * outlier's values would come back on a different scale from everyone else's and the correlations built from
     * them would be meaningless.
     */
    @Test
    public void anExcludedColumnIsStillMappedOntoTheReference() {
        DoubleMatrix<String, String> m = matrix( new double[][] {
                { 1.0, 2.0, 900.0 },
                { 4.0, 5.0, 901.0 },
                { 7.0, 8.0, 902.0 } } );

        DoubleMatrix<String, String> out = new MatrixNormalizer<String, String>()
                .quantileNormalize( m, new boolean[] { true, true, false } );

        for ( int i = 0; i < 3; i++ ) {
            assertThat( out.get( i, 2 ) ).as( "excluded column must carry values, not NaN" ).isNotNaN();
        }
        // every column ends up drawing from the same set of reference values
        assertThat( out.get( 2, 2 ) ).isEqualTo( out.get( 2, 0 ) );
        assertThat( out.get( 0, 2 ) ).isEqualTo( out.get( 0, 0 ) );
    }

    /**
     * An extreme column left in the reference drags it; excluded, the remaining columns normalize among
     * themselves. This is the effect on the data everyone else reads, not just on the outlier's own column.
     */
    @Test
    public void excludingAnExtremeColumnLeavesTheOthersToNormalizeAmongThemselves() {
        double[][] data = new double[][] {
                { 1.0, 2.0, 1000.0 },
                { 3.0, 4.0, 2000.0 },
                { 5.0, 6.0, 3000.0 } };

        DoubleMatrix<String, String> included = new MatrixNormalizer<String, String>()
                .quantileNormalize( matrix( data ) );
        DoubleMatrix<String, String> excluded = new MatrixNormalizer<String, String>()
                .quantileNormalize( matrix( data ), new boolean[] { true, true, false } );

        assertThat( excluded.get( 0, 0 ) ).isEqualTo( 1.5 );
        assertThat( excluded.get( 1, 0 ) ).isEqualTo( 3.5 );
        assertThat( excluded.get( 2, 0 ) ).isEqualTo( 5.5 );
        assertThat( included.get( 0, 0 ) )
                .as( "with the extreme column voting, the reference is dragged far off the other two" )
                .isGreaterThan( 300.0 );
    }

    /**
     * Null means every column votes, which is what every caller outside the processed-vector pipeline still does.
     */
    @Test
    public void nullReferenceKeepsTheHistoricalBehaviour() {
        double[][] data = new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } };
        DoubleMatrix<String, String> viaNull = new MatrixNormalizer<String, String>()
                .quantileNormalize( matrix( data ), null );
        DoubleMatrix<String, String> viaAllTrue = new MatrixNormalizer<String, String>()
                .quantileNormalize( matrix( data ), new boolean[] { true, true } );
        DoubleMatrix<String, String> viaNoArg = new MatrixNormalizer<String, String>()
                .quantileNormalize( matrix( data ) );
        for ( int i = 0; i < 3; i++ ) {
            for ( int j = 0; j < 2; j++ ) {
                assertThat( viaNull.get( i, j ) ).isEqualTo( viaAllTrue.get( i, j ) );
                assertThat( viaNull.get( i, j ) ).isEqualTo( viaNoArg.get( i, j ) );
            }
        }
    }

    @Test
    public void everyColumnExcludedIsRejectedRatherThanProducingNaN() {
        DoubleMatrix<String, String> m = matrix( new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } } );
        assertThatThrownBy( () -> new MatrixNormalizer<String, String>()
                .quantileNormalize( m, new boolean[] { false, false } ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> new MatrixNormalizer<String, String>()
                .quantileNormalize( m, new boolean[] { true } ) )
                .isInstanceOf( IllegalArgumentException.class );
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

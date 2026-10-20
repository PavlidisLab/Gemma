package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GeeqServiceImpl#getLowerTriCormat} feeds the sample mean, median and variance correlation scores. Pseudo-bulk
 * samples with no cells correlate as NaN with every other sample, so single-cell matrices hold millions of NaNs.
 */
public class GeeqLowerTriCormatTest {

    @Test
    public void testDropsNaNsAndKeepsTheRestInOrder() {
        DoubleMatrix<BioAssay, BioAssay> m = cormat( new double[][] {
                { 1, 0.1, 0.2, 0.3 },
                { 0.1, 1, Double.NaN, 0.4 },
                { 0.2, Double.NaN, 1, 0.5 },
                { 0.3, 0.4, 0.5, 1 } } );
        // lower triangle by rows: 0.1 | 0.2, NaN | 0.3, 0.4, 0.5
        assertThat( GeeqServiceImpl.getLowerTriCormat( m ) ).containsExactly( 0.1, 0.2, 0.3, 0.4, 0.5 );
    }

    @Test
    public void testEmptyOrMissingMatrix() {
        assertThat( GeeqServiceImpl.getLowerTriCormat( null ) ).isEmpty();
        assertThat( GeeqServiceImpl.getLowerTriCormat( cormat( new double[0][0] ) ) ).isEmpty();
    }

    /**
     * Same values, in the same order, as the list-removal implementation it replaces, on a matrix shaped like a
     * pseudo-bulk one: a block of empty samples whose correlations are all NaN, plus scattered NaNs.
     */
    @Test
    public void testIdenticalToThePreviousImplementation() {
        Random r = new Random( 42L );
        int n = 300;
        double[][] x = new double[n][n];
        for ( int i = 0; i < n; i++ ) {
            for ( int j = 0; j <= i; j++ ) {
                double v = i == j ? 1.0 : r.nextDouble() * 2 - 1;
                if ( i != j && ( i % 7 == 0 || j % 7 == 0 || r.nextDouble() < 0.01 ) ) {
                    v = Double.NaN;
                }
                x[i][j] = v;
                x[j][i] = v;
            }
        }
        DoubleMatrix<BioAssay, BioAssay> m = cormat( x );
        assertThat( GeeqServiceImpl.getLowerTriCormat( m ) ).containsExactly( previousImplementation( x ) );
    }

    /**
     * 2,000 samples, 300 of them empty: about 560,000 NaNs among 2 million values. Removing them one at a time took
     * on the order of 10^12 element moves; one pass takes milliseconds.
     */
    @Test
    public void testManyNaNsAreFast() {
        int n = 2000;
        double[][] x = new double[n][n];
        for ( int i = 0; i < n; i++ ) {
            for ( int j = 0; j < n; j++ ) {
                x[i][j] = i == j ? 1.0 : ( i < 300 || j < 300 ) ? Double.NaN : 0.5;
            }
        }
        long start = System.nanoTime();
        double[] tri = GeeqServiceImpl.getLowerTriCormat( cormat( x ) );
        long elapsedMs = ( System.nanoTime() - start ) / 1_000_000;
        assertThat( tri ).hasSize( ( n - 300 ) * ( n - 301 ) / 2 ).containsOnly( 0.5 );
        assertThat( elapsedMs ).isLessThan( 10_000 );
    }

    private static double[] previousImplementation( double[][] mat ) {
        List<Double> list = new ArrayList<>();
        for ( int i = 0; i < mat.length; i++ ) {
            for ( int j = 0; j < i; j++ ) {
                list.add( mat[i][j] );
            }
        }
        //noinspection StatementWithEmptyBody
        while ( list.remove( Double.NaN ) ) {
        }
        return ArrayUtils.toPrimitive( list.toArray( new Double[0] ) );
    }

    private static DoubleMatrix<BioAssay, BioAssay> cormat( double[][] values ) {
        List<BioAssay> assays = new ArrayList<>();
        for ( int i = 0; i < values.length; i++ ) {
            assays.add( BioAssay.Factory.newInstance( "s" + i ) );
        }
        DoubleMatrix<BioAssay, BioAssay> m = new DenseDoubleMatrix<>( values.length == 0 ? new double[0][0] : values );
        m.setRowNames( assays );
        m.setColumnNames( assays );
        return m;
    }
}

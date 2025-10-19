package ubic.gemma.core.util;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix1D;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;

import java.util.Arrays;

/**
 * Extends {@link ubic.basecode.math.MatrixStats}.
 * <p>
 * TODO: move those methods to baseCode.
 */
public class MatrixStats {

    /**
     * Return the mean of each row in the matrix.
     * <p>
     * Missing values are ignored.
     */
    public static DoubleMatrix1D rowMeans( DoubleMatrix<?, ?> dmatrix ) {
        double[] means = new double[dmatrix.rows()];
        for ( int i = 0; i < dmatrix.rows(); i++ ) {
            means[i] = DescriptiveWithMissing.mean( new DoubleArrayList( dmatrix.getRow( i ) ) );
        }
        return new DenseDoubleMatrix1D( means );
    }

    /**
     * Calculate the ranks of each value in each column of the matrix.
     */
    public static DoubleMatrix2D ranksByColumn( DoubleMatrix<?, ?> dmatrix ) {
        double[][] ranks = new double[dmatrix.rows()][dmatrix.columns()];
        for ( int j = 0; j < dmatrix.columns(); j++ ) {
            double[] column = dmatrix.getColumn( j );
            double[] sorted = column.clone();
            Arrays.sort( sorted );
            for ( int i = 0; i < column.length; i++ ) {
                // find the rank of the value in the sorted array
                ranks[i][j] = ( double ) ArrayUtils.binarySearchFirst( sorted, column[i] ) / ( double ) sorted.length;
            }
        }
        return new DenseDoubleMatrix2D( ranks );
    }
}

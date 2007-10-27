/**
 * 
 */
package ubic.gemma.analysis.stats;

import java.io.IOException;
import java.io.InputStream;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;

/**
 * Given an ExpressionDataMatrix, compute the correlation of the columns (samples).
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataSampleCorrelation {

    public static DoubleMatrixNamed getMatrix( ExpressionDataDoubleMatrix matrix ) {
        int cols = matrix.columns();
        double[][] rawcols = new double[cols][];

        for ( int i = 0; i < cols; i++ ) {
            rawcols[i] = matrix.getColumn( i );
        }

        DoubleMatrixNamed columns = DoubleMatrix2DNamedFactory.dense( rawcols );
        return MatrixStats.correlationMatrix( columns ); // FIXME we need something that retains the sample
        // information.
    }

    /**
     * @param is input stream with tab-delimited expression data. This is here for testing.
     * @return
     * @throws IOException
     */
    protected static DoubleMatrixNamed getMatrixFromExperimentFile( InputStream is ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrixNamed matrix = ( DoubleMatrixNamed ) reader.read( is );
        int cols = matrix.columns();
        double[][] rawcols = new double[cols][];

        for ( int i = 0; i < cols; i++ ) {
            rawcols[i] = matrix.getColumn( i );
        }

        DoubleMatrixNamed columns = DoubleMatrix2DNamedFactory.dense( rawcols );
        return MatrixStats.correlationMatrix( columns ); // This has col/row names after the samples.
    }

}

package ubic.gemma.core.util.math.linalg;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SingularValueDecompositionTest {

    /**
     * A decomposition that runs past its limit is reported as a {@link TimeoutException}, which callers can handle,
     * rather than an unchecked exception that escapes diagnostics error handling.
     */
    @Test
    public void testTimeoutIsReportedAsTimeoutException() {
        // a zero limit expires before any decomposition of this size can finish
        TimeoutException e = assertThrows( TimeoutException.class,
                () -> new SingularValueDecomposition<>( randomMatrix( 400, 200 ), 0 ) );
        assertEquals( "SVD did not finish within 0 ms", e.getMessage() );
    }

    private static DoubleMatrix<Integer, Integer> randomMatrix( int rows, int columns ) {
        Random r = new Random( 42L );
        DoubleMatrix<Integer, Integer> m = new DenseDoubleMatrix<>( rows, columns );
        List<Integer> rowNames = new ArrayList<>(), colNames = new ArrayList<>();
        for ( int i = 0; i < rows; i++ ) {
            rowNames.add( i );
            for ( int j = 0; j < columns; j++ ) {
                m.set( i, j, r.nextGaussian() );
            }
        }
        for ( int j = 0; j < columns; j++ ) {
            colNames.add( j );
        }
        m.setRowNames( rowNames );
        m.setColumnNames( colNames );
        return m;
    }
}

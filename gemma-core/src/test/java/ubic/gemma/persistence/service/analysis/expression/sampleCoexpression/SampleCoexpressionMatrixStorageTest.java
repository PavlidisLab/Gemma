package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.math.MatrixStats;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The stored sample correlation matrix, through the service's own encoder and decoder, without a database.
 * <p>
 * New rows hold the lower triangle; rows written before hold the full matrix. Both must decode to the matrix that
 * was computed, bit for bit, NaN rows from empty samples included.
 */
public class SampleCoexpressionMatrixStorageTest {

    @Test
    public void aComputedMatrixIsStoredAsItsLowerTriangleAndReadsBackIdentically() {
        for ( int n : new int[] { 1, 2, 3, 59 } ) {
            List<BioAssay> bioAssays = bioAssays( n );
            DoubleMatrix<BioAssay, BioAssay> cormat = correlationMatrix( bioAssays, new Random( n ) );
            BioAssayDimension dimension = BioAssayDimension.Factory.newInstance( bioAssays );

            SampleCoexpressionMatrix stored = SampleCoexpressionAnalysisServiceImpl.toSampleCoexpressionMatrix( dimension, cormat );
            assertThat( stored.getCoexpressionMatrix() ).as( "n = %d", n ).hasSize( n * ( n + 1 ) / 2 * 8 );

            assertSameByKeys( SampleCoexpressionAnalysisServiceImpl.toDoubleMatrix( stored ), cormat );
        }
    }

    @Test
    public void aRowInTheOldFullFormatReadsAsBefore() {
        int n = 59;
        List<BioAssay> bioAssays = bioAssays( n );
        DoubleMatrix<BioAssay, BioAssay> cormat = correlationMatrix( bioAssays, new Random( 7 ) );
        BioAssayDimension dimension = BioAssayDimension.Factory.newInstance( bioAssays );
        SampleCoexpressionMatrix old = SampleCoexpressionMatrix.Factory.newInstance( dimension,
                ByteArrayUtils.doubleMatrixToBytes( cormat.getRawMatrix() ) );
        assertThat( old.getCoexpressionMatrix() ).hasSize( n * n * 8 );

        DoubleMatrix<BioAssay, BioAssay> read = SampleCoexpressionAnalysisServiceImpl.toDoubleMatrix( old );
        assertSameByKeys( read, cormat );

        // and the same matrix, row order included, as one stored in the new format
        DoubleMatrix<BioAssay, BioAssay> readNew = SampleCoexpressionAnalysisServiceImpl.toDoubleMatrix(
                SampleCoexpressionAnalysisServiceImpl.toSampleCoexpressionMatrix( dimension, cormat ) );
        assertThat( readNew.getRowNames() ).isEqualTo( read.getRowNames() );
        assertThat( readNew.getColNames() ).isEqualTo( read.getColNames() );
        for ( int i = 0; i < n; i++ ) {
            for ( int j = 0; j < n; j++ ) {
                assertThat( Double.doubleToRawLongBits( readNew.get( i, j ) ) )
                        .isEqualTo( Double.doubleToRawLongBits( read.get( i, j ) ) );
            }
        }
    }

    @Test
    public void aBlobOfTheWrongLengthFailsRatherThanReadingAsNoMatrix() {
        List<BioAssay> bioAssays = bioAssays( 4 );
        SampleCoexpressionMatrix corrupt = SampleCoexpressionMatrix.Factory.newInstance(
                BioAssayDimension.Factory.newInstance( bioAssays ), new byte[12 * 8] );
        assertThatThrownBy( () -> SampleCoexpressionAnalysisServiceImpl.toDoubleMatrix( corrupt ) )
                .isInstanceOf( UnreadableSampleCoexpressionMatrixException.class );
    }

    /**
     * Named in reverse so that the decoder's sort by sample name reorders them.
     */
    private static List<BioAssay> bioAssays( int n ) {
        List<BioAssay> bioAssays = new ArrayList<>( n );
        for ( int i = 0; i < n; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( String.format( "bm%03d", n - i ) );
            bm.setId( ( long ) i + 1 );
            BioAssay ba = BioAssay.Factory.newInstance( String.format( "ba%03d", n - i ), null, bm );
            ba.setId( ( long ) i + 1 );
            bioAssays.add( ba );
        }
        return bioAssays;
    }

    /**
     * A correlation matrix from the producer the service uses, with every value of the second and last samples
     * missing, as an empty pseudo-bulk sample has them.
     */
    private static DoubleMatrix<BioAssay, BioAssay> correlationMatrix( List<BioAssay> bioAssays, Random random ) {
        int n = bioAssays.size();
        int genes = 40;
        double[][] data = new double[n][genes];
        for ( int i = 0; i < n; i++ ) {
            for ( int g = 0; g < genes; g++ ) {
                data[i][g] = ( n > 2 && ( i == 1 || i == n - 1 ) ) ? Double.NaN : random.nextGaussian();
            }
        }
        DoubleMatrix<BioAssay, Integer> mat = new DenseDoubleMatrix<>( data );
        mat.setRowNames( bioAssays );
        List<Integer> cols = new ArrayList<>();
        for ( int g = 0; g < genes; g++ ) {
            cols.add( g );
        }
        mat.setColumnNames( cols );
        DoubleMatrix<BioAssay, BioAssay> cormat = MatrixStats.correlationMatrix( mat );
        if ( n > 2 ) {
            assertThat( cormat.get( 0, 1 ) ).isNaN();
        }
        return cormat;
    }

    private static void assertSameByKeys( DoubleMatrix<BioAssay, BioAssay> actual, DoubleMatrix<BioAssay, BioAssay> expected ) {
        assertThat( actual ).isNotNull();
        assertThat( actual.rows() ).isEqualTo( expected.rows() );
        assertThat( actual.columns() ).isEqualTo( expected.columns() );
        assertThat( actual.getRowNames() ).isEqualTo( actual.getColNames() );
        for ( BioAssay a : expected.getRowNames() ) {
            for ( BioAssay b : expected.getColNames() ) {
                assertThat( Double.doubleToRawLongBits( actual.getByKeys( a, b ) ) )
                        .as( "%s, %s", a.getName(), b.getName() )
                        .isEqualTo( Double.doubleToRawLongBits( expected.getByKeys( a, b ) ) );
            }
        }
    }
}

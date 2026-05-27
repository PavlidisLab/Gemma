package ubic.gemma.persistence.service.expression.bioAssayData;

import org.junit.Test;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.math.MatrixNormalizer;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies that {@link ProcessedExpressionDataVectorCreationHelperServiceImpl#quantileNormalizeInPlace}
 * is element-wise equivalent to {@link ubic.basecode.math.MatrixNormalizer#quantileNormalize}
 * on the same fixture used by {@link ubic.gemma.core.analysis.preprocess.QuantileNormalizerTest}.
 */
public class QuantileNormalizeInPlaceTest {

    private static final double EPS = 1e-9;

    @Test
    public void matchesMatrixNormalizerOnFixture() throws Exception {
        DoubleMatrix<String, String> tester = new DoubleMatrixReader()
                .read( getClass().getResourceAsStream( "/data/testdata.txt" ) );
        assertEquals( "fixture should still pin -0.525 at [0,9]",
                -0.525,
                new MatrixNormalizer<String, String>().quantileNormalize( deepCopy( tester ) ).get( 0, 9 ),
                0.001 );

        DoubleMatrix<String, String> reference = new MatrixNormalizer<String, String>()
                .quantileNormalize( deepCopy( tester ) );

        double[][] inPlace = toArray( tester );
        ProcessedExpressionDataVectorCreationHelperServiceImpl.quantileNormalizeInPlace( inPlace );

        assertEquals( reference.rows(), inPlace.length );
        assertEquals( reference.columns(), inPlace[0].length );
        for ( int i = 0; i < reference.rows(); i++ ) {
            for ( int j = 0; j < reference.columns(); j++ ) {
                assertEquals( "mismatch at [" + i + "," + j + "]",
                        reference.get( i, j ), inPlace[i][j], EPS );
            }
        }
    }

    @Test
    public void preservesNaNAndImputesForRanking() {
        // Row 1 has a NaN; row mean of {2, 8} = 5 is used in place for ranking,
        // and the original NaN is restored in the output.
        double[][] data = {
                { 1.0, 4.0, 7.0 },
                { 2.0, Double.NaN, 8.0 },
                { 3.0, 6.0, 9.0 },
                { 4.0, 5.0, 10.0 },
        };

        double[][] reference = new double[data.length][data[0].length];
        for ( int i = 0; i < data.length; i++ ) {
            System.arraycopy( data[i], 0, reference[i], 0, data[i].length );
        }
        ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<Integer, Integer> refMat =
                new ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<>( reference );
        for ( int i = 0; i < refMat.rows(); i++ ) refMat.setRowName( i, i );
        for ( int j = 0; j < refMat.columns(); j++ ) refMat.setColumnName( j, j );
        DoubleMatrix<Integer, Integer> refResult = new MatrixNormalizer<Integer, Integer>().quantileNormalize( refMat );

        ProcessedExpressionDataVectorCreationHelperServiceImpl.quantileNormalizeInPlace( data );

        for ( int i = 0; i < data.length; i++ ) {
            for ( int j = 0; j < data[i].length; j++ ) {
                double r = refResult.get( i, j );
                if ( Double.isNaN( r ) ) {
                    assertTrue( "expected NaN at [" + i + "," + j + "]", Double.isNaN( data[i][j] ) );
                } else {
                    assertEquals( "mismatch at [" + i + "," + j + "]", r, data[i][j], EPS );
                }
            }
        }
    }

    /**
     * Randomised property test: for many small matrices with varied dimensions, tie density, and NaN
     * density, the in-place implementation must agree element-wise with
     * {@link MatrixNormalizer#quantileNormalize} to within {@link #EPS}.
     * <p>
     * Catches divergences the two fixture-based tests can't: stability-sensitive tie patterns, NaN
     * placement edge cases, narrow matrices, very high tie density, etc.
     * <p>
     * Scope: realistic expression-data inputs. Signed-zero corner cases (mixed {@code -0.0}/{@code +0.0}
     * within a column) are deliberately excluded — see {@link #signedZerosKnownDivergence}. Such inputs
     * cannot arise in log2cpm / linear expression data.
     */
    @Test
    public void matchesMatrixNormalizerOnRandomInputs() {
        // Fixed seed → deterministic and reproducible. Bump to investigate flakes.
        Random rng = new Random( 0xC0FFEEBEEFL );
        int trials = 500;
        int mismatches = 0;
        StringBuilder firstFailure = new StringBuilder();
        for ( int trial = 0; trial < trials; trial++ ) {
            int rows = 3 + rng.nextInt( 40 );   // 3..42
            int cols = 2 + rng.nextInt( 12 );   // 2..13
            int regime = trial % 4;             // cycle through corner-case regimes (signed-zero regime under separate test)
            double nanProb;
            boolean injectTies;
            boolean injectSignedZeros;
            switch ( regime ) {
                case 0:
                    nanProb = 0.0;
                    injectTies = false;
                    injectSignedZeros = false;
                    break;
                case 1:
                    nanProb = 0.1;
                    injectTies = false;
                    injectSignedZeros = false;
                    break;
                case 2:
                    nanProb = 0.0;
                    injectTies = true;
                    injectSignedZeros = false;
                    break;
                case 3:
                default:
                    nanProb = 0.3;
                    injectTies = true;
                    injectSignedZeros = false;
                    break;
            }
            double[][] data = generateMatrix( rng, rows, cols, nanProb, injectTies, injectSignedZeros );
            // RowMissingFilter(minPresentCount=1) drops all-NaN rows. If too few non-all-NaN rows remain
            // to construct a matrix at all, basecode throws — skip such degenerate inputs.
            int presentRows = 0;
            for ( double[] row : data ) {
                for ( double v : row ) {
                    if ( !Double.isNaN( v ) ) {
                        presentRows++;
                        break;
                    }
                }
            }
            if ( presentRows < 2 ) {
                continue;
            }

            // Reference path (basecode). Use a deep copy because MatrixNormalizer's imputeMissing
            // mutates its argument.
            double[][] refArr = deepCopy2D( data );
            ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<Integer, Integer> refMat =
                    new ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<>( refArr );
            for ( int i = 0; i < refMat.rows(); i++ ) refMat.setRowName( i, i );
            for ( int j = 0; j < refMat.columns(); j++ ) refMat.setColumnName( j, j );
            DoubleMatrix<Integer, Integer> refResult;
            try {
                refResult = new MatrixNormalizer<Integer, Integer>().quantileNormalize( refMat );
            } catch ( RuntimeException e ) {
                // basecode can throw on very degenerate inputs (e.g. RowMissingFilter dropping everything).
                // Skip — there's nothing to compare against.
                continue;
            }

            // Our path.
            double[][] inPlace = deepCopy2D( data );
            ProcessedExpressionDataVectorCreationHelperServiceImpl.quantileNormalizeInPlace( inPlace );

            // RowMissingFilter may drop all-NaN rows before basecode returns. We keep them as all-NaN.
            // So compare on the rows basecode actually returned (mapped by row-name index) instead of by
            // raw position.
            for ( int riOut = 0; riOut < refResult.rows(); riOut++ ) {
                int originalRow = refResult.getRowName( riOut );
                for ( int j = 0; j < cols; j++ ) {
                    double refVal = refResult.get( riOut, j );
                    double mine = inPlace[originalRow][j];
                    boolean refNaN = Double.isNaN( refVal );
                    boolean mineNaN = Double.isNaN( mine );
                    if ( refNaN != mineNaN || ( !refNaN && Math.abs( refVal - mine ) > EPS ) ) {
                        mismatches++;
                        if ( firstFailure.length() == 0 ) {
                            firstFailure.append( "trial=" ).append( trial )
                                    .append( " regime=" ).append( regime )
                                    .append( " dims=" ).append( rows ).append( "x" ).append( cols )
                                    .append( " at [" ).append( originalRow ).append( "," ).append( j ).append( "]" )
                                    .append( " ref=" ).append( refVal )
                                    .append( " mine=" ).append( mine );
                        }
                    }
                }
            }
        }
        if ( mismatches > 0 ) {
            fail( "found " + mismatches + " element-wise mismatch(es) across " + trials + " random trials. First: " + firstFailure );
        }
    }

    /**
     * Documents — does NOT assert away — the one known corner case where the in-place implementation
     * diverges from {@link MatrixNormalizer#quantileNormalize}: a column containing both {@code -0.0}
     * and {@code +0.0}. Java's primitive {@code <} treats them as equal (which colt's sort uses), but
     * IEEE-754 / {@link Double#compare} orders {@code -0.0 < +0.0}. The two paths make different
     * choices about whether to tie those values, leading to different averaged ranks. This cannot
     * occur in realistic expression data (no operation in the pipeline produces a literal {@code -0.0}),
     * so we accept the divergence and pin it here to keep it visible.
     */
    @Test
    public void signedZerosKnownDivergence() {
        // Build a column where -0.0 and +0.0 coexist.
        int rows = 5000;
        double[][] data = new double[rows][2];
        Random rng = new Random( 42L );
        for ( int i = 0; i < rows; i++ ) {
            data[i][0] = rng.nextGaussian();
            // alternate signed zero in column 1, with non-zero filler
            if ( i % 7 == 0 ) {
                data[i][1] = ( i % 2 == 0 ) ? 0.0 : -0.0;
            } else {
                data[i][1] = rng.nextGaussian();
            }
        }

        double[][] refArr = deepCopy2D( data );
        ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<Integer, Integer> refMat =
                new ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<>( refArr );
        for ( int i = 0; i < refMat.rows(); i++ ) refMat.setRowName( i, i );
        for ( int j = 0; j < refMat.columns(); j++ ) refMat.setColumnName( j, j );
        DoubleMatrix<Integer, Integer> refResult = new MatrixNormalizer<Integer, Integer>().quantileNormalize( refMat );

        double[][] mine = deepCopy2D( data );
        ProcessedExpressionDataVectorCreationHelperServiceImpl.quantileNormalizeInPlace( mine );

        // Sanity: shapes match.
        assertEquals( refResult.rows(), mine.length );
        // Both produce valid output; we just don't expect element-wise equivalence here.
        // If a future change happens to make this case agree, this assert will start failing —
        // delete the test (no longer a divergence) and consider tightening the scope above.
        boolean anyDiff = false;
        for ( int riOut = 0; riOut < refResult.rows() && !anyDiff; riOut++ ) {
            int origRow = refResult.getRowName( riOut );
            for ( int j = 0; j < 2; j++ ) {
                if ( Math.abs( refResult.get( riOut, j ) - mine[origRow][j] ) > EPS ) {
                    anyDiff = true;
                    break;
                }
            }
        }
        assertTrue( "signed-zero divergence vanished — expected at least one mismatch", anyDiff );
    }

    private static double[][] generateMatrix( Random rng, int rows, int cols, double nanProb, boolean injectTies, boolean injectSignedZeros ) {
        double[][] m = new double[rows][cols];
        // optionally pre-build a small pool of "shared" values to inject ties across positions
        double[] tiePool = null;
        if ( injectTies ) {
            int pool = 1 + rng.nextInt( Math.max( 1, ( rows * cols ) / 8 ) );
            tiePool = new double[pool];
            for ( int t = 0; t < pool; t++ ) {
                tiePool[t] = rng.nextGaussian() * 5.0;
            }
        }
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                if ( rng.nextDouble() < nanProb ) {
                    m[i][j] = Double.NaN;
                } else if ( injectTies && rng.nextDouble() < 0.3 ) {
                    m[i][j] = tiePool[rng.nextInt( tiePool.length )];
                } else if ( injectSignedZeros && rng.nextDouble() < 0.1 ) {
                    m[i][j] = rng.nextBoolean() ? 0.0 : -0.0;
                } else {
                    m[i][j] = rng.nextGaussian() * 10.0;
                }
            }
        }
        return m;
    }

    private static double[][] deepCopy2D( double[][] src ) {
        double[][] out = new double[src.length][];
        for ( int i = 0; i < src.length; i++ ) {
            out[i] = src[i].clone();
        }
        return out;
    }

    private static double[][] toArray( DoubleMatrix<?, ?> m ) {
        double[][] out = new double[m.rows()][m.columns()];
        for ( int i = 0; i < m.rows(); i++ ) {
            for ( int j = 0; j < m.columns(); j++ ) {
                out[i][j] = m.get( i, j );
            }
        }
        return out;
    }

    private static DoubleMatrix<String, String> deepCopy( DoubleMatrix<String, String> src ) {
        double[][] arr = new double[src.rows()][src.columns()];
        for ( int i = 0; i < src.rows(); i++ ) {
            for ( int j = 0; j < src.columns(); j++ ) {
                arr[i][j] = src.get( i, j );
            }
        }
        ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<String, String> out =
                new ubic.basecode.dataStructure.matrix.DenseDoubleMatrix<>( arr );
        for ( int i = 0; i < src.rows(); i++ ) {
            out.setRowName( src.getRowName( i ), i );
        }
        for ( int j = 0; j < src.columns(); j++ ) {
            out.setColumnName( src.getColName( j ), j );
        }
        return out;
    }
}
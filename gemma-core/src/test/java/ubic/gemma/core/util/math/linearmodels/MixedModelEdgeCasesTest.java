package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.CholeskyDecomposition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Situations the golden fixtures do not reach: clamping, missing observations, weights, permutation, and the
 * small-sample gates of {@link MixedModelFit#estimateCorrelation}. Synthetic data, seeded.
 */
public class MixedModelEdgeCasesTest {

    private static final int SUBJECTS = 12;

    private static String[] pairedBlocks() {
        String[] b = new String[2 * SUBJECTS];
        for ( int i = 0; i < b.length; i++ ) b[i] = "s" + ( i / 2 );
        return b;
    }

    /**
     * Intercept + an arm column that alternates within each subject.
     */
    private static DoubleMatrix2D armDesign() {
        DoubleMatrix2D x = new DenseDoubleMatrix2D( 2 * SUBJECTS, 2 );
        for ( int i = 0; i < 2 * SUBJECTS; i++ ) {
            x.set( i, 0, 1.0 );
            x.set( i, 1, i % 2 );
        }
        return x;
    }

    /**
     * y = subjectEffect * sd + noise; sign flips the second member's subject effect (anticorrelated pairs).
     */
    private static DoubleMatrix2D data( int probes, double subjectSd, double noiseSd, double secondSign, long seed ) {
        Random rnd = new Random( seed );
        DoubleMatrix2D d = new DenseDoubleMatrix2D( probes, 2 * SUBJECTS );
        for ( int g = 0; g < probes; g++ ) {
            for ( int s = 0; s < SUBJECTS; s++ ) {
                double u = rnd.nextGaussian() * subjectSd;
                d.set( g, 2 * s, u + rnd.nextGaussian() * noiseSd );
                d.set( g, 2 * s + 1, secondSign * u + rnd.nextGaussian() * noiseSd + 0.5 );
            }
        }
        return d;
    }

    /**
     * Subject variance dwarfs the noise: the per-probe rho sits at its upper clamp (0.99) rather than at 1,
     * where the correlation matrix would no longer be positive definite.
     */
    @Test
    public void nearPerfectDuplicatesClampAtTheUpperBound() {
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( armDesign(), pairedBlocks(),
                data( 40, 10.0, 1e-4, 1.0, 1L ), null );
        assertThat( mmf.getConsensusCorrelation() ).isBetween( 0.98, 0.99 + 1e-12 );
        // the clamped value still gives a fittable (positive definite) whitening matrix
        assertThat( new CholeskyDecomposition( MixedModelFit.blockCorrelationMatrix( pairedBlocks(), mmf.getConsensusCorrelation() ) )
                .isSymmetricPositiveDefinite() ).isTrue();
    }

    /**
     * Mirror-image pairs: the estimate is bounded below by 1/(1 - maxBlockSize) + 0.01 = -0.99 for blocks of
     * two, so it can be negative but never singular.
     */
    @Test
    public void anticorrelatedPairsGoNegativeButStayAboveTheLowerBound() {
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( armDesign(), pairedBlocks(),
                data( 40, 10.0, 1e-4, -1.0, 2L ), null );
        // the design's arm column absorbs a constant offset only, so the mirrored subject effect stays in the residual
        assertThat( mmf.getConsensusCorrelation() ).isLessThan( 0.0 ).isGreaterThanOrEqualTo( -0.99 - 1e-12 );
        assertThat( new CholeskyDecomposition( MixedModelFit.blockCorrelationMatrix( pairedBlocks(), mmf.getConsensusCorrelation() ) )
                .isSymmetricPositiveDefinite() ).isTrue();
    }

    /**
     * No subject structure in the data: the consensus is small and either sign, and nothing degenerates.
     */
    @Test
    public void noSubjectVarianceGivesACorrelationNearZero() {
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( armDesign(), pairedBlocks(),
                data( 200, 0.0, 1.0, 1.0, 3L ), null );
        assertThat( mmf.isDegenerateToZero() ).isFalse();
        assertThat( mmf.getConsensusCorrelation() ).isBetween( -0.25, 0.25 );
    }

    /**
     * A probe with a missing observation is estimated on its observed samples (limma's per-probe subsetting);
     * a probe with so little left that it fails the gate keeps NaN and is left out of the consensus.
     */
    @Test
    public void probesWithMissingValuesAreEstimatedOrSkippedNotFatal() {
        DoubleMatrix2D d = data( 30, 2.0, 1.0, 1.0, 4L );
        d.set( 0, 3, Double.NaN ); // one observation missing
        for ( int j = 0; j < 2 * SUBJECTS - 3; j++ ) d.set( 1, j, Double.NaN ); // three left: below nbeta + 2
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( armDesign(), pairedBlocks(), d, null );

        assertThat( mmf.getAtanhCorrelations()[0] ).isFinite();
        assertThat( mmf.getAtanhCorrelations()[1] ).isNaN();
        assertThat( mmf.getConsensusCorrelation() ).isBetween( -0.99, 0.99 );
    }

    /**
     * If every probe fails the per-probe gate there is no consensus; Gemma reports rho = 0 (the fit becomes
     * OLS) rather than NaN, and does not flag the back-off as degenerate-by-design.
     */
    @Test
    public void allProbesTooSparseGivesZeroWithoutTheDegenerateFlag() {
        DoubleMatrix2D d = data( 5, 2.0, 1.0, 1.0, 5L );
        for ( int g = 0; g < 5; g++ ) {
            for ( int j = 0; j < 2 * SUBJECTS - 3; j++ ) d.set( g, j, Double.NaN );
        }
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( armDesign(), pairedBlocks(), d, null );
        assertThat( mmf.getConsensusCorrelation() ).isZero();
        assertThat( mmf.isDegenerateToZero() ).isFalse();
        assertThat( Arrays.stream( mmf.getAtanhCorrelations() ).allMatch( Double::isNaN ) ).isTrue();
    }

    /**
     * Uniform weights of one change nothing.
     */
    @Test
    public void unitWeightsGiveTheUnweightedEstimate() {
        DoubleMatrix2D d = data( 40, 2.0, 1.0, 1.0, 6L );
        DoubleMatrix2D w = new DenseDoubleMatrix2D( d.rows(), d.columns() );
        w.assign( 1.0 );
        double plain = new MixedModelFit().estimateCorrelation( armDesign(), pairedBlocks(), d, null ).getConsensusCorrelation();
        double weighted = new MixedModelFit().estimateCorrelation( armDesign(), pairedBlocks(), d, w ).getConsensusCorrelation();
        assertThat( weighted ).isCloseTo( plain, offset( 1e-9 ) );
    }

    /**
     * Reordering samples (design, block and data columns together) does not change the estimate, so a block's
     * members need not be adjacent. This also covers a block id that recurs in separate groups of columns,
     * which limma treats as one block.
     */
    @Test
    public void estimateIsInvariantToSampleOrder() {
        DoubleMatrix2D d = data( 40, 2.0, 1.0, 1.0, 7L );
        String[] block = pairedBlocks();
        DoubleMatrix2D x = armDesign();
        int n = block.length;
        int[] perm = new int[n];
        for ( int i = 0; i < n; i++ ) perm[i] = i;
        Random rnd = new Random( 8L );
        for ( int i = n - 1; i > 0; i-- ) {
            int j = rnd.nextInt( i + 1 );
            int t = perm[i];
            perm[i] = perm[j];
            perm[j] = t;
        }
        String[] pb = new String[n];
        DoubleMatrix2D px = new DenseDoubleMatrix2D( n, 2 );
        DoubleMatrix2D pd = new DenseDoubleMatrix2D( d.rows(), n );
        for ( int i = 0; i < n; i++ ) {
            pb[i] = block[perm[i]];
            px.set( i, 0, x.get( perm[i], 0 ) );
            px.set( i, 1, x.get( perm[i], 1 ) );
            for ( int g = 0; g < d.rows(); g++ ) pd.set( g, i, d.get( g, perm[i] ) );
        }
        double a = new MixedModelFit().estimateCorrelation( x, block, d, null ).getConsensusCorrelation();
        double b = new MixedModelFit().estimateCorrelation( px, pb, pd, null ).getConsensusCorrelation();
        assertThat( b ).isCloseTo( a, offset( 1e-9 ) );
    }

    /**
     * Blocks of different sizes: the lower clamp follows the LARGEST block (1/(1 - 4) + 0.01), and the
     * correlation matrix is block-diagonal with unit diagonal.
     */
    @Test
    public void correlationMatrixIsBlockDiagonalWithUnitDiagonal() {
        String[] block = { "a", "a", "a", "a", "b", "b", "c" };
        DoubleMatrix2D v = MixedModelFit.blockCorrelationMatrix( block, 0.3 );
        for ( int i = 0; i < block.length; i++ ) {
            for ( int j = 0; j < block.length; j++ ) {
                double expected = i == j ? 1.0 : block[i].equals( block[j] ) ? 0.3 : 0.0;
                assertThat( v.get( i, j ) ).isEqualTo( expected );
            }
        }
        assertThat( new CholeskyDecomposition( v ).isSymmetricPositiveDefinite() ).isTrue();
        // a rho below the size-4 block's limit (-1/3) is not positive definite: the reason for the clamp
        assertThat( new CholeskyDecomposition( MixedModelFit.blockCorrelationMatrix( block, -0.4 ) )
                .isSymmetricPositiveDefinite() ).isFalse();
    }

    /**
     * Fewer samples than the per-probe gate allows (nobs <= nbeta + 2): nothing estimable, rho = 0.
     */
    @Test
    public void tooFewSamplesForTheDesignGivesZero() {
        String[] block = { "a", "a", "b", "b" };
        DoubleMatrix2D x = new DenseDoubleMatrix2D( 4, 2 );
        for ( int i = 0; i < 4; i++ ) {
            x.set( i, 0, 1.0 );
            x.set( i, 1, i % 2 );
        }
        DoubleMatrix2D d = new DenseDoubleMatrix2D( 3, 4 );
        Random rnd = new Random( 9L );
        for ( int g = 0; g < 3; g++ ) for ( int j = 0; j < 4; j++ ) d.set( g, j, rnd.nextGaussian() );
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( x, block, d, null );
        assertThat( mmf.getConsensusCorrelation() ).isZero();
    }
}

package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Predicting a sample that did not contribute to the fit.
 * <p>
 * The sample-correlation matrix blanks flagged outliers before regressing, so they never influence the model —
 * and their residual came back NaN, which is why the regressed matrix could never show what an outlier
 * correlated at. The prediction was always computed ({@code A.beta} covers every row of the design) and then
 * masked away; {@code getFittedIncludingMissing()} keeps it.
 */
public class FittedValuesForExcludedSamplesTest {

    /**
     * Four samples, two groups, and the fourth blanked. The fit must be the fit of the other three, and the
     * blanked one must still get a prediction.
     */
    @Test
    public void aBlankedSampleIsPredictedButDoesNotInfluenceTheFit() {
        // intercept + group indicator
        DoubleMatrix2D design = new DenseDoubleMatrix2D( new double[][] {
                { 1, 0 }, { 1, 0 }, { 1, 1 }, { 1, 1 } } );
        // one row of data; the last sample is missing
        DoubleMatrix2D withMissing = new DenseDoubleMatrix2D( new double[][] {
                { 10.0, 12.0, 20.0, Double.NaN } } );
        // the same data with the missing sample simply absent, as three samples
        DoubleMatrix2D designWithoutIt = new DenseDoubleMatrix2D( new double[][] {
                { 1, 0 }, { 1, 0 }, { 1, 1 } } );
        DoubleMatrix2D withoutIt = new DenseDoubleMatrix2D( new double[][] { { 10.0, 12.0, 20.0 } } );

        LeastSquaresFit fit = new LeastSquaresFit( design, withMissing );
        LeastSquaresFit reference = new LeastSquaresFit( designWithoutIt, withoutIt );

        assertThat( fit.isHasMissing() ).isTrue();

        // the blanked sample did not move the model
        for ( int p = 0; p < reference.getCoefficients().rows(); p++ ) {
            assertThat( fit.getCoefficients().get( p, 0 ) )
                    .as( "coefficient %d must match the fit that never saw the blanked sample", p )
                    .isCloseTo( reference.getCoefficients().get( p, 0 ), org.assertj.core.data.Offset.offset( 1e-9 ) );
        }

        // ... and yet it has a prediction, which getFitted() hides
        assertThat( fit.getFitted().get( 0, 3 ) )
                .as( "getFitted() blanks a sample that contributed nothing" )
                .isNaN();
        assertThat( fit.getFittedIncludingMissing().get( 0, 3 ) )
                .as( "the prediction for the blanked sample is what places it on the fit" )
                .isNotNaN()
                .isCloseTo( 20.0, org.assertj.core.data.Offset.offset( 1e-9 ) );

        // the residual we can now compute for it: observed 26 against a fit built without it
        double observed = 26.0;
        double residual = observed - fit.getFittedIncludingMissing().get( 0, 3 );
        assertThat( residual ).isCloseTo( 6.0, org.assertj.core.data.Offset.offset( 1e-9 ) );
    }

    /**
     * The samples that did take part keep exactly the residuals they had. Filling in the excluded ones must not
     * disturb anything else — that is what makes this safe to turn on for every dataset with a flagged assay.
     */
    @Test
    public void theContributingSamplesKeepTheirResiduals() {
        DoubleMatrix2D design = new DenseDoubleMatrix2D( new double[][] {
                { 1, 0 }, { 1, 0 }, { 1, 1 }, { 1, 1 } } );
        DoubleMatrix2D data = new DenseDoubleMatrix2D( new double[][] {
                { 10.0, 12.0, 20.0, Double.NaN } } );

        LeastSquaresFit fit = new LeastSquaresFit( design, data );

        assertThat( fit.getResiduals().get( 0, 0 ) ).isCloseTo( -1.0, org.assertj.core.data.Offset.offset( 1e-9 ) );
        assertThat( fit.getResiduals().get( 0, 1 ) ).isCloseTo( 1.0, org.assertj.core.data.Offset.offset( 1e-9 ) );
        assertThat( fit.getResiduals().get( 0, 2 ) ).isCloseTo( 0.0, org.assertj.core.data.Offset.offset( 1e-9 ) );
        assertThat( fit.getResiduals().get( 0, 3 ) ).isNaN();
    }

    /**
     * With nothing missing the two views are the same matrix of numbers, so no caller can be surprised by
     * reaching for the new one.
     */
    @Test
    public void withoutMissingValuesBothViewsAgree() {
        DoubleMatrix2D design = new DenseDoubleMatrix2D( new double[][] {
                { 1, 0 }, { 1, 0 }, { 1, 1 }, { 1, 1 } } );
        DoubleMatrix2D data = new DenseDoubleMatrix2D( new double[][] {
                { 10.0, 12.0, 20.0, 26.0 } } );

        LeastSquaresFit fit = new LeastSquaresFit( design, data );

        assertThat( fit.isHasMissing() ).isFalse();
        for ( int j = 0; j < 4; j++ ) {
            assertThat( fit.getFittedIncludingMissing().get( 0, j ) )
                    .isCloseTo( fit.getFitted().get( 0, j ), org.assertj.core.data.Offset.offset( 1e-12 ) );
        }
    }
}

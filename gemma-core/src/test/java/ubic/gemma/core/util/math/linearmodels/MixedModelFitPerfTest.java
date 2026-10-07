package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The consensus-correlation estimation is a PER-PROBE loop (QR of the design, eigendecomposition of the
 * residual block span, a gamma IRLS) -- the cost scales with probes and could quietly become the new
 * bottleneck of the analysis phase. Guard: at production-ish shape (20k probes, 48 samples, 24 subjects of
 * 2), the whole mixed fit (estimation + whitened GLS) stays within a generous factor of the equivalent
 * fixed-effect fit on the same data.
 * <p>
 * Tagged {@code slow}: excluded from both surefire and failsafe by default (see the ${excludedGroups}
 * taxonomy in the parent pom); run with -DexcludedGroups=network. Wall-clock assertions are loose on
 * purpose -- CI timing jitter -- the test is a tripwire for order-of-magnitude regressions, and it LOGS
 * the actual timings for the perf probe workflow.
 */
@Tag("slow")
public class MixedModelFitPerfTest {

    private static final int PROBES = 20000;
    private static final int SUBJECTS = 24;
    private static final int SAMPLES = SUBJECTS * 2;

    private DoubleMatrix<String, String> data() {
        Random r = new Random( 42 );
        double[][] m = new double[PROBES][SAMPLES];
        for ( int g = 0; g < PROBES; g++ ) {
            double geneOffset = r.nextGaussian();
            for ( int j = 0; j < SAMPLES; j++ ) {
                m[g][j] = geneOffset + r.nextGaussian() * 0.3;
            }
        }
        DenseDoubleMatrix<String, String> data = new DenseDoubleMatrix<>( m );
        String[] probes = new String[PROBES];
        for ( int g = 0; g < PROBES; g++ ) {
            probes[g] = "probe_" + g;
        }
        data.setRowNames( Arrays.asList( probes ) );
        String[] samples = new String[SAMPLES];
        for ( int j = 0; j < SAMPLES; j++ ) {
            samples[j] = "s" + ( j / 2 + 1 ) + "_" + ( j % 2 == 0 ? "ctrl" : "treat" );
        }
        data.setColumnNames( Arrays.asList( samples ) );
        return data;
    }

    private DesignMatrix design() {
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( SAMPLES, 1 );
        design.setColumnNames( Arrays.asList( "treatment" ) );
        String[] names = new String[SAMPLES];
        String[] trt = new String[SAMPLES];
        for ( int j = 0; j < SAMPLES; j++ ) {
            trt[j] = j % 2 == 0 ? "ctrl" : "treat";
            names[j] = "s" + ( j / 2 + 1 ) + "_" + trt[j];
        }
        design.setRowNames( Arrays.asList( names ) );
        for ( int i = 0; i < SAMPLES; i++ ) {
            design.set( i, 0, trt[i] );
        }
        DesignMatrix dm = new DesignMatrix( design, true );
        dm.setBaseline( "treatment", "ctrl" );
        return dm;
    }

    private String[] blockIds() {
        String[] block = new String[SAMPLES];
        for ( int j = 0; j < SAMPLES; j++ ) {
            block[j] = "s" + ( j / 2 + 1 );
        }
        return block;
    }

    @Test
    public void mixedFitStaysWithinAFactorOfTheFixedFit() {
        DoubleMatrix<String, String> data = data();
        DesignMatrix dm = design();
        String[] block = blockIds();

        long t0 = System.nanoTime();
        LeastSquaresFit fixedFit = new LeastSquaresFit( dm, data );
        ModeratedTstat.ebayes( fixedFit );
        long fixedNanos = System.nanoTime() - t0;

        t0 = System.nanoTime();
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( dm.getDoubleMatrix(), block,
                new DenseDoubleMatrix2D( data.asArray() ), null );
        DoubleMatrix2D correlation = MixedModelFit.blockCorrelationMatrix( block, mmf.getConsensusCorrelation() );
        LeastSquaresFit mixedFit = new LeastSquaresFit( dm, correlation, data );
        ModeratedTstat.ebayes( mixedFit );
        long mixedNanos = System.nanoTime() - t0;

        System.out.printf( "fixed fit: %.2f s; mixed fit (estimation + GLS): %.2f s (rho=%.4f)%n",
                fixedNanos / 1e9, mixedNanos / 1e9, mmf.getConsensusCorrelation() );

        /*
         * Tripwire for order-of-magnitude regressions, not a benchmark. Measured baseline at this shape:
         * fixed 0.9-1.6 s, mixed 10-12 s -- the consensus-correlation estimation is a per-probe loop
         * (each probe: effects, an mq x mq eigendecomposition, ~20 gamma-IRLS iterations), inherently an
         * order of magnitude above the shared-QR OLS fit. An attempt to collapse the per-probe spectrum to
         * singular-value groups (cheap economy SVD + weighted GLM) was tried and REVERTED: it measured 48 s,
         * worse than the direct form. 10 s for an explicitly-requested mixed-model analysis at 20k probes is
         * acceptable; this guard fails if a change makes it e.g. 100 s.
         */
        assertThat( mixedNanos ).as( "mixed fit wall time" ).isLessThan( 15 * fixedNanos );
    }
}

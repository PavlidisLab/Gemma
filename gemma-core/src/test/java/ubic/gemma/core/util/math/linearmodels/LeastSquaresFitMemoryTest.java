package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;
import ubic.gemma.core.util.matrix.ObjectMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How much heap a bulk fit holds at GSE260875's shape, measured rather than reasoned about.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@code corrMat -force} on GSE260875 (eid 35280, 1,090 samples, 2,657 rows after filtering) reaches
 * ~22.8 GB of <b>live</b> heap and dies against the 30 GiB ergonomic ceiling. frb's GC logs settle that it is
 * liveness and not garbage: the peak collection in a 240 GiB run reclaimed two megabytes, and the final full
 * collections at the default heap reclaimed nothing at all.</p>
 *
 * <p>Adding up everything nameable as reachable at that point — the processed vectors, the rebuilt matrix, its
 * outlier-masked copy, the filtered matrices, the fit's residuals and fitted-including-missing — reaches about
 * 1.5 to 2 GB. An order of magnitude short, so the fit became the suspect: it is the only thing GSE260875 does
 * that the experiments larger than it do not, its own javadoc warns that missing values make it "less memory
 * efficient", and it caches a {@link ubic.gemma.core.util.math.linalg.QRDecomposition} per missingness pattern,
 * which at 2,657 rows could have been thousands of them.</p>
 *
 * <p><b>It is not where the memory is.</b> Measured here at 98 MB against 22 MB of data, 4.5x — and the QR
 * cache holds ONE entry, not thousands, because an outlier-masked column is missing in every row alike, so
 * every row presents the same pattern. The test stays as the budget that says so, and fails if someone makes
 * the fit the answer after all.</p>
 *
 * <p>The data here is synthetic and the numbers are meaningless; only the shape matters. Tagged {@code slow}
 * because it allocates at production scale.</p>
 *
 * @author gemma
 */
@Tag("slow")
class LeastSquaresFitMemoryTest {

    /** GSE260875's samples. */
    private static final int SAMPLES = 1090;
    /** GSE260875's design elements after the correlation-matrix filter. */
    private static final int ROWS = 2657;

    @Test
    void aBulkFitAtGse260875sShapeStaysWithinABudget() {
        ObjectMatrix<String, String, Object> design = design();
        DoubleMatrix<String, String> data = data();

        long before = liveHeap();

        DesignMatrix designMatrix = new DesignMatrix( design, true );
        LeastSquaresFit fit = new LeastSquaresFit( designMatrix, data );
        DoubleMatrix2D residuals = fit.getResiduals();
        DoubleMatrix2D fittedIncludingMissing = fit.getFittedIncludingMissing();

        long after = liveHeap();
        long heldMb = ( after - before ) / ( 1024 * 1024 );
        long dataMb = ( long ) ROWS * SAMPLES * 8 / ( 1024 * 1024 );

        System.out.printf( "LeastSquaresFit at %d x %d: data %d MB, fit holds %d MB (%.1fx)%n",
                ROWS, SAMPLES, dataMb, heldMb, ( double ) heldMb / dataMb );

        // Keep everything reachable across the measurement, the way regressionResiduals does.
        assertTrue( residuals.rows() > 0 && fittedIncludingMissing.rows() > 0 );

        // A budget, not a prediction. The fit legitimately holds residuals and fitted-including-missing, two
        // matrices the size of the data, plus per-missingness-pattern QR decompositions. Twelve times the data
        // is generous for that and still an order of magnitude below the 22.8 GB being chased, so this fails
        // loudly if the fit turns out to be where that lives -- and fails later if someone makes it so.
        assertTrue( heldMb < 12 * dataMb,
                "the fit holds " + heldMb + " MB for " + dataMb + " MB of data" );
    }

    /**
     * Three factors over the samples, which is what the SVD kept for GSE260875 before the regression ran.
     */
    private static ObjectMatrix<String, String, Object> design() {
        ObjectMatrixImpl<String, String, Object> design = new ObjectMatrixImpl<>( SAMPLES, 3 );
        List<String> sampleNames = new ArrayList<>( SAMPLES );
        for ( int i = 0; i < SAMPLES; i++ ) {
            sampleNames.add( "s" + i );
        }
        design.setRowNames( sampleNames );
        design.setColumnNames( java.util.Arrays.asList( "organismPart", "sex", "batch" ) );
        for ( int i = 0; i < SAMPLES; i++ ) {
            design.set( i, 0, "part" + ( i % 4 ) );
            design.set( i, 1, i % 2 == 0 ? "male" : "female" );
            design.set( i, 2, "batch" + ( i % 6 ) );
        }
        return design;
    }

    /**
     * One all-NaN column, which is what a flagged outlier looks like to the fit and what puts it on the
     * missing-value path.
     */
    private static DoubleMatrix<String, String> data() {
        Random random = new Random( 12345L );
        double[][] raw = new double[ROWS][SAMPLES];
        for ( int i = 0; i < ROWS; i++ ) {
            for ( int j = 0; j < SAMPLES; j++ ) {
                raw[i][j] = j == 7 ? Double.NaN : random.nextGaussian();
            }
        }
        DenseDoubleMatrix<String, String> data = new DenseDoubleMatrix<>( raw );
        List<String> rowNames = new ArrayList<>( ROWS );
        for ( int i = 0; i < ROWS; i++ ) {
            rowNames.add( "probe" + i );
        }
        List<String> colNames = new ArrayList<>( SAMPLES );
        for ( int j = 0; j < SAMPLES; j++ ) {
            colNames.add( "s" + j );
        }
        data.setRowNames( rowNames );
        data.setColumnNames( colNames );
        return data;
    }

    private static long liveHeap() {
        for ( int i = 0; i < 3; i++ ) {
            System.gc();
            try {
                Thread.sleep( 50 );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
            }
        }
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}

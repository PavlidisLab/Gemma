package ubic.gemma.persistence.service.expression.experiment;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The stored sample-correlation matrix keeps the outlier samples' values, so a curator can review the call
 * against them. Every GEEQ score is about the samples that count, so GEEQ blanks them on the way in.
 * <p>
 * Masking at the point of use also preserves {@code corrMatIssues == 2}, which is set when the matrix
 * contains NaNs and has therefore always meant "this dataset has flagged outliers".
 */
public class GeeqCormatMaskingTest {

    private BioAssay assay( String name, boolean outlier ) {
        BioAssay ba = BioAssay.Factory.newInstance( name );
        ba.setIsOutlier( outlier );
        return ba;
    }

    private DoubleMatrix<BioAssay, BioAssay> cormat( List<BioAssay> assays ) {
        DoubleMatrix<BioAssay, BioAssay> m = new DenseDoubleMatrix<>( assays.size(), assays.size() );
        m.setRowNames( assays );
        m.setColumnNames( assays );
        for ( int i = 0; i < assays.size(); i++ ) {
            for ( int j = 0; j < assays.size(); j++ ) {
                m.set( i, j, i == j ? 1.0 : 0.9 );
            }
        }
        return m;
    }

    @Test
    public void testAFlaggedSampleIsBlankedInBothDirections() {
        BioAssay ok1 = assay( "ok1", false ), bad = assay( "bad", true ), ok2 = assay( "ok2", false );
        DoubleMatrix<BioAssay, BioAssay> m = GeeqServiceImpl.maskOutliers( cormat( Arrays.asList( ok1, bad, ok2 ) ) );

        assertThat( m ).isNotNull();
        for ( int k = 0; k < 3; k++ ) {
            assertThat( m.get( 1, k ) ).isNaN();
            assertThat( m.get( k, 1 ) ).isNaN();
        }
        // everything not involving the flagged sample survives
        assertThat( m.get( 0, 2 ) ).isEqualTo( 0.9 );
        assertThat( m.get( 2, 0 ) ).isEqualTo( 0.9 );
        assertThat( m.get( 0, 0 ) ).isEqualTo( 1.0 );
    }

    @Test
    public void testAMatrixWithNoFlaggedSamplesIsUntouched() {
        BioAssay a = assay( "a", false ), b = assay( "b", false );
        DoubleMatrix<BioAssay, BioAssay> m = GeeqServiceImpl.maskOutliers( cormat( Arrays.asList( a, b ) ) );

        assertThat( m ).isNotNull();
        assertThat( m.get( 0, 1 ) ).isEqualTo( 0.9 );
        assertThat( m.get( 1, 0 ) ).isEqualTo( 0.9 );
    }

    @Test
    public void testNullIsPassedThrough() {
        assertThat( GeeqServiceImpl.maskOutliers( null ) ).isNull();
    }
}

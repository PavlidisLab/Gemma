package ubic.gemma.core.util.matrix.datafilter;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RowMissingFilter}: removes matrix rows that have too many
 * missing values (NaN, by the matrix's own {@code isMissing} contract). Pins the
 * setter validation contract, the no-op default behaviour (no min set -> no
 * filtering), the absolute-minimum guard ({@code ABSOLUTEMINPRESENT=1}), and the
 * row-name + column-name preservation through the filter pass.
 *
 * @author claude
 */
public class RowMissingFilterTest {

    /**
     * Build a 4x3 matrix where rows have varying numbers of present values:
     * <ul>
     *     <li>row 0: 3 present (all)</li>
     *     <li>row 1: 2 present + 1 NaN</li>
     *     <li>row 2: 1 present + 2 NaN</li>
     *     <li>row 3: 0 present (all NaN)</li>
     * </ul>
     */
    private DenseDoubleMatrix<String, String> buildMatrixWithMissing() {
        DenseDoubleMatrix<String, String> m = new DenseDoubleMatrix<>( 4, 3 );
        m.setRowNames( Arrays.asList( "r0", "r1", "r2", "r3" ) );
        m.setColumnNames( Arrays.asList( "c0", "c1", "c2" ) );
        m.set( 0, 0, 1.0 );
        m.set( 0, 1, 2.0 );
        m.set( 0, 2, 3.0 );
        m.set( 1, 0, 1.0 );
        m.set( 1, 1, Double.NaN );
        m.set( 1, 2, 3.0 );
        m.set( 2, 0, Double.NaN );
        m.set( 2, 1, Double.NaN );
        m.set( 2, 2, 3.0 );
        m.set( 3, 0, Double.NaN );
        m.set( 3, 1, Double.NaN );
        m.set( 3, 2, Double.NaN );
        return m;
    }

    @Test
    public void noMinPresentSet_returnsInputUnchanged() {
        // Use a >=5-column matrix so the implementation's default minPresentCount (5) doesn't
        // trip the dimensionality check before reaching the !minPresentIsSet short-circuit.
        DenseDoubleMatrix<String, String> in = new DenseDoubleMatrix<>( 2, 5 );
        in.setRowNames( Arrays.asList( "r0", "r1" ) );
        in.setColumnNames( Arrays.asList( "c0", "c1", "c2", "c3", "c4" ) );
        for ( int i = 0; i < 2; i++ ) for ( int j = 0; j < 5; j++ ) in.set( i, j, 1.0 );

        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        DoubleMatrix<String, String> out = filter.filter( in );
        // Same matrix instance returned when no filtering requested.
        assertThat( out ).isSameAs( in );
    }

    @Test
    public void minPresentCount_keepsOnlyRowsMeetingThreshold() {
        DoubleMatrix<String, String> in = buildMatrixWithMissing();
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        filter.setMinPresentCount( 2 );

        DoubleMatrix<String, String> out = filter.filter( in );
        // r0 has 3 present, r1 has 2 -> kept; r2 has 1, r3 has 0 -> dropped.
        assertThat( out.rows() ).isEqualTo( 2 );
        assertThat( out.getRowNames() ).containsExactly( "r0", "r1" );
        assertThat( out.getColNames() ).containsExactly( "c0", "c1", "c2" );
    }

    @Test
    public void minPresentCount_threeRequiresAllValues() {
        DoubleMatrix<String, String> in = buildMatrixWithMissing();
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        filter.setMinPresentCount( 3 );
        DoubleMatrix<String, String> out = filter.filter( in );
        assertThat( out.rows() ).isEqualTo( 1 );
        assertThat( out.getRowNames() ).containsExactly( "r0" );
    }

    @Test
    public void minPresentCount_oneAdmitsRowsWithAnyPresentValue() {
        DoubleMatrix<String, String> in = buildMatrixWithMissing();
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        filter.setMinPresentCount( 1 );
        DoubleMatrix<String, String> out = filter.filter( in );
        // r0,r1,r2 have at least one present; r3 has none -> dropped.
        assertThat( out.rows() ).isEqualTo( 3 );
        assertThat( out.getRowNames() ).containsExactly( "r0", "r1", "r2" );
    }

    @Test
    public void minPresentFraction_isConvertedToCount() {
        DoubleMatrix<String, String> in = buildMatrixWithMissing();
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        // 0.5 of 3 cols = ceil(1.5) = 2 required present.
        filter.setMinPresentFraction( 0.5 );
        filter.setMinPresentCount( 0 ); // setMinPresentFraction alone does NOT set the "is-set" flag for count; ensure both
        // re-call to engage the fraction path (the impl checks fractionIsSet then overrides count).
        filter.setMinPresentFraction( 0.5 );
        // We need minPresentIsSet=true; set count to satisfy that, then fraction will override during filter().
        filter.setMinPresentCount( 1 );
        filter.setMinPresentFraction( 0.5 );
        DoubleMatrix<String, String> out = filter.filter( in );
        // 0.5 * 3 = 1.5 -> ceil -> 2 required.
        assertThat( out.rows() ).isEqualTo( 2 );
    }

    @Test
    public void minPresentCount_exceedingColumnCount_throws() {
        DoubleMatrix<String, String> in = buildMatrixWithMissing();
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        filter.setMinPresentCount( 4 ); // matrix has only 3 cols
        assertThatThrownBy( () -> filter.filter( in ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "Minimum present count" );
    }

    @Test
    public void setMinPresentCount_rejectsNegative() {
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        assertThatThrownBy( () -> filter.setMinPresentCount( -1 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Minimum present count must be > 0" );
    }

    @Test
    public void setMinPresentFraction_rejectsOutOfRange() {
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        assertThatThrownBy( () -> filter.setMinPresentFraction( -0.01 ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> filter.setMinPresentFraction( 1.01 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void setMaxFractionRemoved_rejectsOutOfRange() {
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        assertThatThrownBy( () -> filter.setMaxFractionRemoved( -0.01 ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> filter.setMaxFractionRemoved( 1.01 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void valuesArePreservedAcrossFilter() {
        DoubleMatrix<String, String> in = buildMatrixWithMissing();
        RowMissingFilter<DoubleMatrix<String, String>, String, String, Double> filter = new RowMissingFilter<>();
        filter.setMinPresentCount( 2 );
        DoubleMatrix<String, String> out = filter.filter( in );
        // r0 row values intact
        assertThat( out.get( 0, 0 ) ).isEqualTo( 1.0 );
        assertThat( out.get( 0, 1 ) ).isEqualTo( 2.0 );
        assertThat( out.get( 0, 2 ) ).isEqualTo( 3.0 );
        // r1's NaN is preserved
        assertThat( Double.isNaN( out.get( 1, 1 ) ) ).isTrue();
        assertThat( out.get( 1, 0 ) ).isEqualTo( 1.0 );
        assertThat( out.get( 1, 2 ) ).isEqualTo( 3.0 );
    }
}

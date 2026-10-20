package ubic.gemma.core.analysis.preprocess.filter;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract-level unit tests for {@link TooFewDistinctValuesFilter}: pins the
 * threshold-validation invariants (constructor rejects out-of-range values) and
 * the {@code appliesTo} short-circuit (threshold zero -> no-op filter). The
 * actual row removal is delegated to {@link RowLevelFilter} and is exercised by
 * higher-level differential-expression tests; the boundary checks live here.
 *
 * @author claude
 */
public class TooFewDistinctValuesFilterTest {

    @Test
    public void constructor_rejectsNegativeThreshold() {
        assertThatThrownBy( () -> new TooFewDistinctValuesFilter( -0.01 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Threshold must be between 0 and 1" );
    }

    @Test
    public void constructor_rejectsThresholdAboveOne() {
        assertThatThrownBy( () -> new TooFewDistinctValuesFilter( 1.01 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Threshold must be between 0 and 1" );
    }

    @Test
    public void constructor_acceptsZeroThreshold() {
        TooFewDistinctValuesFilter filter = new TooFewDistinctValuesFilter( 0.0 );
        assertThat( filter ).isNotNull();
    }

    @Test
    public void constructor_acceptsOneThreshold() {
        TooFewDistinctValuesFilter filter = new TooFewDistinctValuesFilter( 1.0 );
        assertThat( filter ).isNotNull();
    }

    @Test
    public void appliesTo_returnsFalse_whenThresholdIsZero() {
        TooFewDistinctValuesFilter filter = new TooFewDistinctValuesFilter( 0.0 );
        ExpressionDataDoubleMatrix matrix = Mockito.mock( ExpressionDataDoubleMatrix.class );
        assertThat( filter.appliesTo( matrix ) ).isFalse();
    }

    @Test
    public void appliesTo_returnsTrue_whenThresholdIsPositive() {
        TooFewDistinctValuesFilter filter = new TooFewDistinctValuesFilter( 0.5 );
        ExpressionDataDoubleMatrix matrix = Mockito.mock( ExpressionDataDoubleMatrix.class );
        assertThat( filter.appliesTo( matrix ) ).isTrue();
    }

    @Test
    public void toString_includesPercentFormattedThreshold() {
        TooFewDistinctValuesFilter filter = new TooFewDistinctValuesFilter( 0.5 );
        assertThat( filter.toString() ).contains( "50.00%" ).contains( "TooFewDistinctValuesFilter" );
    }

    @Test
    public void toString_renderingForZeroThreshold() {
        TooFewDistinctValuesFilter filter = new TooFewDistinctValuesFilter( 0.0 );
        assertThat( filter.toString() ).contains( "0.00%" );
    }
}

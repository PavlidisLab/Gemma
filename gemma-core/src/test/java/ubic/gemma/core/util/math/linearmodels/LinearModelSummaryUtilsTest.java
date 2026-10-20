package ubic.gemma.core.util.math.linearmodels;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LinearModelSummaryUtils#createTerm2CoefficientNames}: the helper that
 * maps factor names to the set of R-side coefficient-row names belonging to that factor. The
 * routine is shape-sensitive (intercept rows are skipped; interaction rows are matched by a
 * regex-cleaned "fv_NNN" stripping; main-effect rows match by prefix), and is consumed by the
 * differential-expression pipeline when packaging contrast results.
 *
 * @author claude
 */
public class LinearModelSummaryUtilsTest {

    private DoubleMatrix<String, String> contrastsWithRowNames( String... rowNames ) {
        DenseDoubleMatrix<String, String> m = new DenseDoubleMatrix<>( rowNames.length, 1 );
        m.setRowNames( Arrays.asList( rowNames ) );
        m.setColumnNames( List.of( "Estimate" ) );
        return m;
    }

    @Test
    public void interceptRow_isSkipped() {
        DoubleMatrix<String, String> contrasts = contrastsWithRowNames( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME );
        Map<String, Collection<String>> result = LinearModelSummaryUtils.createTerm2CoefficientNames(
                List.of( "f1001" ), contrasts );

        assertThat( result ).containsOnlyKeys( "f1001" );
        assertThat( result.get( "f1001" ) ).isEmpty();
    }

    @Test
    public void mainEffectCoefficient_isAttachedToMatchingFactor() {
        DoubleMatrix<String, String> contrasts = contrastsWithRowNames(
                LinearModelSummary.INTERCEPT_COEFFICIENT_NAME,
                "f1001fv_2001",
                "f1001fv_2002" );

        Map<String, Collection<String>> result = LinearModelSummaryUtils.createTerm2CoefficientNames(
                List.of( "f1001" ), contrasts );

        assertThat( result.get( "f1001" ) ).containsExactlyInAnyOrder( "f1001fv_2001", "f1001fv_2002" );
    }

    @Test
    public void mainEffect_doesNotLeakAcrossFactors() {
        DoubleMatrix<String, String> contrasts = contrastsWithRowNames(
                "f1001fv_2001",
                "f1002fv_3001" );

        Map<String, Collection<String>> result = LinearModelSummaryUtils.createTerm2CoefficientNames(
                List.of( "f1001", "f1002" ), contrasts );

        assertThat( result.get( "f1001" ) ).containsExactly( "f1001fv_2001" );
        assertThat( result.get( "f1002" ) ).containsExactly( "f1002fv_3001" );
    }

    @Test
    public void interactionRow_matchesInteractionFactorAfterFvStripping() {
        // The cleaning regex strips "fv_NNN" tokens immediately preceding ':' or end-of-string;
        // "f1001fv_2001:f1002fv_3001" cleans to "f1001:f1002".
        DoubleMatrix<String, String> contrasts = contrastsWithRowNames(
                LinearModelSummary.INTERCEPT_COEFFICIENT_NAME,
                "f1001fv_2001:f1002fv_3001" );

        Map<String, Collection<String>> result = LinearModelSummaryUtils.createTerm2CoefficientNames(
                List.of( "f1001", "f1002", "f1001:f1002" ), contrasts );

        assertThat( result.get( "f1001:f1002" ) ).containsExactly( "f1001fv_2001:f1002fv_3001" );
        // Main-effect terms should NOT absorb the interaction row — the regex-cleaned token has
        // a colon, so the else-branch (prefix matching on main effects) is bypassed.
        assertThat( result.get( "f1001" ) ).isEmpty();
        assertThat( result.get( "f1002" ) ).isEmpty();
    }

    @Test
    public void factorWithNoCoefficients_yieldsEmptyCollection() {
        DoubleMatrix<String, String> contrasts = contrastsWithRowNames(
                LinearModelSummary.INTERCEPT_COEFFICIENT_NAME,
                "f1001fv_2001" );

        Map<String, Collection<String>> result = LinearModelSummaryUtils.createTerm2CoefficientNames(
                List.of( "f1001", "f9999" ), contrasts );

        assertThat( result.get( "f9999" ) ).isEmpty();
    }

    @Test
    public void emptyFactorList_yieldsEmptyMap() {
        DoubleMatrix<String, String> contrasts = contrastsWithRowNames(
                LinearModelSummary.INTERCEPT_COEFFICIENT_NAME );

        Map<String, Collection<String>> result = LinearModelSummaryUtils.createTerm2CoefficientNames(
                List.of(), contrasts );

        assertThat( result ).isEmpty();
    }
}

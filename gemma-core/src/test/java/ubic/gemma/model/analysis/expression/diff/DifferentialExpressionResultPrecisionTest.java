package ubic.gemma.model.analysis.expression.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.RoundingUtils.JSON_SIGNIFICANT_DIGITS;

/**
 * Wire-form guards for the differential-expression statistics, which serialize on every route that carries
 * these value objects — {@code /resultSets/{id}} and the {@code /datasets/analyses/differential/results}
 * family among them.
 *
 * @author paul
 */
public class DifferentialExpressionResultPrecisionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testResultStatisticsSerializeRounded() throws Exception {
        DifferentialExpressionAnalysisResultValueObject vo = new DifferentialExpressionAnalysisResultValueObject();
        vo.setPValue( 1.2345678901234E-7 );
        vo.setCorrectedPvalue( 9.8765432109876E-11 );

        String json = objectMapper.writeValueAsString( vo );

        assertThat( JSON_SIGNIFICANT_DIGITS ).isEqualTo( 4 );
        assertThat( json ).contains( "\"pValue\":1.235E-7" );
        assertThat( json ).contains( "\"correctedPvalue\":9.877E-11" );
    }

    /**
     * Four significant digits still resolves a p-value at the bottom of the double range, which is why the
     * rule is significant digits rather than decimal places.
     */
    @Test
    public void testAVerySmallPvalueSurvives() throws Exception {
        DifferentialExpressionAnalysisResultValueObject vo = new DifferentialExpressionAnalysisResultValueObject();
        vo.setPValue( 1.23456789E-300 );
        assertThat( objectMapper.writeValueAsString( vo ) ).contains( "\"pValue\":1.235E-300" );
    }

    /**
     * Rounding happens at serialization, so the in-memory value the diff-ex ranking sorts on stays exact.
     */
    @Test
    public void testTheInMemoryStatisticIsNotRounded() throws Exception {
        DifferentialExpressionAnalysisResultValueObject vo = new DifferentialExpressionAnalysisResultValueObject();
        vo.setCorrectedPvalue( 9.8765432109876E-11 );
        objectMapper.writeValueAsString( vo );
        assertThat( vo.getCorrectedPvalue() ).isEqualTo( 9.8765432109876E-11 );
    }

    @Test
    public void testContrastStatisticsSerializeRounded() throws Exception {
        ContrastResultValueObject vo = new ContrastResultValueObject();
        vo.setPvalue( 0.1234567890123 );
        vo.setTStat( -3.14159265358979 );
        vo.setCoefficient( 123456789.123 );
        vo.setLogFoldChange( 9.876543210987 );

        String json = objectMapper.writeValueAsString( vo );

        assertThat( json ).contains( "\"pvalue\":0.1235" );
        assertThat( json ).contains( "\"tStat\":-3.142" );
        assertThat( json ).contains( "\"coefficient\":1.235E8" );
        assertThat( json ).contains( "\"logFoldChange\":9.877" );
    }

    @Test
    public void testNullStatisticsAreStillNull() throws Exception {
        ContrastResultValueObject vo = new ContrastResultValueObject();
        vo.setPvalue( null );
        assertThat( objectMapper.writeValueAsString( vo ) ).contains( "\"pvalue\":null" );
    }

    /**
     * The result value object carries its contrasts inline, so the nested statistics have to round too.
     */
    @Test
    public void testNestedContrastsAreRoundedInsideAResult() throws Exception {
        ContrastResultValueObject contrast = new ContrastResultValueObject();
        contrast.setLogFoldChange( 9.876543210987 );
        DifferentialExpressionAnalysisResultValueObject vo = new DifferentialExpressionAnalysisResultValueObject();
        vo.setContrasts( Collections.singletonList( contrast ) );
        assertThat( objectMapper.writeValueAsString( vo ) ).contains( "\"logFoldChange\":9.877" );
    }
}

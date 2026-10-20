package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the differential-expression analysis exception family:
 * {@link AnalysisException} (base) and its sub-types ({@link MeanVarianceFailureException}).
 * Each exception carries the {@link DifferentialExpressionAnalysisConfig} that was active
 * at the time of failure; this contract is what lets upstream error handlers report which
 * analysis configuration triggered the failure. Tests pin:
 * <ul>
 *   <li>both constructors propagate the config + cause correctly</li>
 *   <li>the sub-type chain is intact ({@code instanceof RuntimeException} stays true)</li>
 *   <li>{@code MeanVarianceFailureException} routes via the cause-bearing constructor</li>
 * </ul>
 *
 * @author claude
 */
public class AnalysisExceptionTest {

    @Test
    public void analysisException_messageConstructor_carriesMessageAndConfig() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        AnalysisException ex = new AnalysisException( "boom", config );

        assertThat( ex.getMessage() ).isEqualTo( "boom" );
        assertThat( ex.getConfig() ).isSameAs( config );
        assertThat( ex.getCause() ).isNull();
    }

    @Test
    public void analysisException_causeConstructor_carriesCauseAndConfig() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        IllegalStateException cause = new IllegalStateException( "underlying" );
        AnalysisException ex = new AnalysisException( config, cause );

        assertThat( ex.getCause() ).isSameAs( cause );
        assertThat( ex.getConfig() ).isSameAs( config );
        // RuntimeException(cause) populates the message from the cause's toString
        assertThat( ex.getMessage() ).contains( "underlying" );
    }

    @Test
    public void analysisException_isARuntimeException() {
        AnalysisException ex = new AnalysisException( "x", new DifferentialExpressionAnalysisConfig() );
        assertThat( ex ).isInstanceOf( RuntimeException.class );
    }

    @Test
    public void meanVarianceFailureException_carriesCauseAndConfig() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        Exception cause = new Exception( "stat solver blew up" );
        MeanVarianceFailureException ex = new MeanVarianceFailureException( config, cause );

        assertThat( ex.getCause() ).isSameAs( cause );
        assertThat( ex.getConfig() ).isSameAs( config );
    }

    @Test
    public void meanVarianceFailureException_extendsAnalysisException() {
        MeanVarianceFailureException ex = new MeanVarianceFailureException(
                new DifferentialExpressionAnalysisConfig(), new Exception( "x" ) );
        assertThat( ex ).isInstanceOf( AnalysisException.class );
        assertThat( ex ).isInstanceOf( RuntimeException.class );
    }

    @Test
    public void analysisException_nullConfig_isPermitted_butReadsAsNull() {
        // Some failure sites construct the exception before a config is fully built
        // (e.g. validation failures during config assembly). Pinning that null config
        // is silently allowed prevents an over-eager NPE assertion regression.
        AnalysisException ex = new AnalysisException( "no config available", null );
        assertThat( ex.getConfig() ).isNull();
        assertThat( ex.getMessage() ).isEqualTo( "no config available" );
    }
}

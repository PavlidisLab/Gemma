package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExpressionAnalysisResultSetArgTest {

    @Test
    public void testValueOfNumeric() {
        ExpressionAnalysisResultSetArg arg = ExpressionAnalysisResultSetArg.valueOf( "1234" );
        assertThat( arg.getValue() ).isEqualTo( 1234L );
    }

    @Test
    public void testValueOfNonNumericRaises() {
        assertThatThrownBy( () -> ExpressionAnalysisResultSetArg.valueOf( "not-a-number" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> ExpressionAnalysisResultSetArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

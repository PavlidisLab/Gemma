package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FactorValueArgTest {

    @Test
    public void testValueOfNumericDispatchesToIdArg() {
        FactorValueArg<?> arg = FactorValueArg.valueOf( "77" );
        assertThat( arg ).isInstanceOf( FactorValueIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 77L );
    }

    @Test
    public void testValueOfStringDispatchesToValueArg() {
        FactorValueArg<?> arg = FactorValueArg.valueOf( "control" );
        assertThat( arg ).isInstanceOf( FactorValueValueArg.class );
        assertThat( arg.getValue() ).isEqualTo( "control" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> FactorValueArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> FactorValueArg.valueOf( "  " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

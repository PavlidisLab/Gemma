package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OffsetArgTest {

    @Test
    public void testValueOfNumeric() {
        assertThat( OffsetArg.valueOf( "42" ).getValue() ).isEqualTo( 42 );
    }

    @Test
    public void testValueOfZero() {
        assertThat( OffsetArg.valueOf( "0" ).getValue() ).isEqualTo( 0 );
    }

    @Test
    public void testValueOfNonNumericRaises() {
        assertThatThrownBy( () -> OffsetArg.valueOf( "xyz" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfNegativeRaises() {
        assertThatThrownBy( () -> OffsetArg.valueOf( "-5" ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

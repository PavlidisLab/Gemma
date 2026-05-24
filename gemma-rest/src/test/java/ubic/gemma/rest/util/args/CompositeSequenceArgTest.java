package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CompositeSequenceArgTest {

    @Test
    public void testValueOfNumericDispatchesToIdArg() {
        CompositeSequenceArg<?> arg = CompositeSequenceArg.valueOf( "555" );
        assertThat( arg ).isInstanceOf( CompositeSequenceIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 555L );
    }

    @Test
    public void testValueOfStringDispatchesToNameArg() {
        CompositeSequenceArg<?> arg = CompositeSequenceArg.valueOf( "201234_at" );
        assertThat( arg ).isInstanceOf( CompositeSequenceNameArg.class );
        assertThat( arg.getValue() ).isEqualTo( "201234_at" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> CompositeSequenceArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> CompositeSequenceArg.valueOf( "   " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

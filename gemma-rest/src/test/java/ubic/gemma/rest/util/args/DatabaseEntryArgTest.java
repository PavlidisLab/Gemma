package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatabaseEntryArgTest {

    @Test
    public void testValueOfNumericDispatchesToIdArg() {
        DatabaseEntryArg<?> arg = DatabaseEntryArg.valueOf( "100" );
        assertThat( arg ).isInstanceOf( DatabaseEntryIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 100L );
    }

    @Test
    public void testValueOfStringDispatchesToStringArg() {
        DatabaseEntryArg<?> arg = DatabaseEntryArg.valueOf( "GSE12345" );
        assertThat( arg ).isInstanceOf( DatabaseEntryStringArg.class );
        assertThat( arg.getValue() ).isEqualTo( "GSE12345" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> DatabaseEntryArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> DatabaseEntryArg.valueOf( "   " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

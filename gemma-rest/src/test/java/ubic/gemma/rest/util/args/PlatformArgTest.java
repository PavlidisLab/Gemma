package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PlatformArgTest {

    @Test
    public void testValueOfNumericDispatchesToIdArg() {
        PlatformArg<?> arg = PlatformArg.valueOf( "789" );
        assertThat( arg ).isInstanceOf( PlatformIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 789L );
    }

    @Test
    public void testValueOfStringDispatchesToStringArg() {
        PlatformArg<?> arg = PlatformArg.valueOf( "GPL570" );
        assertThat( arg ).isInstanceOf( PlatformStringArg.class );
        assertThat( arg.getValue() ).isEqualTo( "GPL570" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> PlatformArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> PlatformArg.valueOf( "  " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

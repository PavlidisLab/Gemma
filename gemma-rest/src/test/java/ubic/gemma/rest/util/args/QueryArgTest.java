package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QueryArgTest {

    @Test
    public void testValueOfNonBlank() {
        assertThat( QueryArg.valueOf( "brain tumor" ).getValue() ).isEqualTo( "brain tumor" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> QueryArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfWhitespaceRaises() {
        assertThatThrownBy( () -> QueryArg.valueOf( "   " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

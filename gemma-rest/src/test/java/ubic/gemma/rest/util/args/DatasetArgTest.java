package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatasetArgTest {

    @Test
    public void testValueOfNumericDispatchesToIdArg() {
        DatasetArg<?> arg = DatasetArg.valueOf( "12345" );
        assertThat( arg ).isInstanceOf( DatasetIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 12345L );
    }

    @Test
    public void testValueOfNumericIsTrimmed() {
        DatasetArg<?> arg = DatasetArg.valueOf( "  42  " );
        assertThat( arg ).isInstanceOf( DatasetIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 42L );
    }

    @Test
    public void testValueOfStringDispatchesToStringArg() {
        DatasetArg<?> arg = DatasetArg.valueOf( "GSE12345" );
        assertThat( arg ).isInstanceOf( DatasetStringArg.class );
        assertThat( arg.getValue() ).isEqualTo( "GSE12345" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> DatasetArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> DatasetArg.valueOf( "   " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

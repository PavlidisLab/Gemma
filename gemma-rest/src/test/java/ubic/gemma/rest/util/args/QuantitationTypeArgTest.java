package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuantitationTypeArgTest {

    @Test
    public void testValueOfNumericDispatchesToByIdArg() {
        QuantitationTypeArg<?> arg = QuantitationTypeArg.valueOf( "42" );
        assertThat( arg ).isInstanceOf( QuantitationTypeByIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 42L );
    }

    @Test
    public void testValueOfStringDispatchesToByNameArg() {
        QuantitationTypeArg<?> arg = QuantitationTypeArg.valueOf( "log2cpm" );
        assertThat( arg ).isInstanceOf( QuantitationTypeByNameArg.class );
        assertThat( arg.getValue() ).isEqualTo( "log2cpm" );
    }
}

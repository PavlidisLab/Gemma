package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LimitArgTest {

    @Test
    public void testValueOfNumeric() {
        assertThat( LimitArg.valueOf( "50" ).getValue() ).isEqualTo( 50 );
    }

    @Test
    public void testValueOfMaximumIsAllowed() {
        assertThat( LimitArg.valueOf( "100" ).getValue() ).isEqualTo( 100 );
    }

    @Test
    public void testValueOfNonNumericRaises() {
        assertThatThrownBy( () -> LimitArg.valueOf( "not-a-number" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfZeroRaises() {
        assertThatThrownBy( () -> LimitArg.valueOf( "0" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfNegativeRaises() {
        assertThatThrownBy( () -> LimitArg.valueOf( "-1" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testGetValueExceedsDefaultMaximumRaises() {
        LimitArg arg = LimitArg.valueOf( "500" );
        assertThatThrownBy( arg::getValue ).isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testGetValueWithCustomMaximum() {
        LimitArg arg = LimitArg.valueOf( "20" );
        assertThat( arg.getValue( 50 ) ).isEqualTo( 20 );
    }

    @Test
    public void testGetValueExceedsCustomMaximumRaises() {
        LimitArg arg = LimitArg.valueOf( "30" );
        assertThatThrownBy( () -> arg.getValue( 10 ) ).isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testGetValueNoMaximumAllowsLarge() {
        LimitArg arg = LimitArg.valueOf( "10000" );
        assertThat( arg.getValueNoMaximum() ).isEqualTo( 10000 );
    }
}

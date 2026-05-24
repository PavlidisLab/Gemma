package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TaxonArgTest {

    @Test
    public void testValueOfSmallNumberDispatchesToIdArg() {
        TaxonArg<?> arg = TaxonArg.valueOf( "1" );
        assertThat( arg ).isInstanceOf( TaxonIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 1L );
    }

    @Test
    public void testValueOfSmallNumberAtBoundaryDispatchesToIdArg() {
        // 999 is the MIN_NCBI_ID boundary; <= 999 -> IdArg
        TaxonArg<?> arg = TaxonArg.valueOf( "999" );
        assertThat( arg ).isInstanceOf( TaxonIdArg.class );
    }

    @Test
    public void testValueOfLargeNumberDispatchesToNcbiIdArg() {
        // 9606 (human NCBI) -> NcbiIdArg
        TaxonArg<?> arg = TaxonArg.valueOf( "9606" );
        assertThat( arg ).isInstanceOf( TaxonNcbiIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 9606 );
    }

    @Test
    public void testValueOfNameDispatchesToNameArg() {
        TaxonArg<?> arg = TaxonArg.valueOf( "human" );
        assertThat( arg ).isInstanceOf( TaxonNameArg.class );
        assertThat( arg.getValue() ).isEqualTo( "human" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> TaxonArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> TaxonArg.valueOf( "  " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

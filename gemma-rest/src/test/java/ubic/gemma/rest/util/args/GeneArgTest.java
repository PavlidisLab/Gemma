package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GeneArgTest {

    @Test
    public void testValueOfNumericDispatchesToNcbiIdArg() {
        GeneArg<?> arg = GeneArg.valueOf( "672" );
        assertThat( arg ).isInstanceOf( GeneNcbiIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 672 );
    }

    @Test
    public void testValueOfEnsemblIdDispatchesToEnsemblArg() {
        GeneArg<?> arg = GeneArg.valueOf( "ENSG00000139618" );
        assertThat( arg ).isInstanceOf( GeneEnsemblIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( "ENSG00000139618" );
    }

    @Test
    public void testValueOfMouseEnsemblIdDispatchesToEnsemblArg() {
        GeneArg<?> arg = GeneArg.valueOf( "ENSMUSG00000017167" );
        assertThat( arg ).isInstanceOf( GeneEnsemblIdArg.class );
    }

    @Test
    public void testValueOfSymbolDispatchesToSymbolArg() {
        GeneArg<?> arg = GeneArg.valueOf( "BRCA1" );
        assertThat( arg ).isInstanceOf( GeneSymbolArg.class );
        assertThat( arg.getValue() ).isEqualTo( "BRCA1" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> GeneArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> GeneArg.valueOf( "  " ) )
                .isInstanceOf( MalformedArgException.class );
    }
}

package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused tests for the {@link AbstractEntityArrayArg}-backed comma-delimited arg parsers.
 * <p>
 * All of these go through {@link AbstractArrayArg#valueOf(String, String, java.util.function.Function, boolean)}
 * so we test their per-class entry points to exercise both the compressed and non-compressed paths.
 */
public class EntityArrayArgsTest {

    @Test
    public void testDatasetArrayArg() {
        DatasetArrayArg arg = DatasetArrayArg.valueOf( "GSE1,GSE2,42" );
        assertThat( arg.getValue() ).containsExactly( "GSE1", "GSE2", "42" );
    }

    @Test
    public void testDatasetArrayArgTrimsWhitespace() {
        DatasetArrayArg arg = DatasetArrayArg.valueOf( "GSE1 ,  GSE2 , 42" );
        assertThat( arg.getValue() ).containsExactly( "GSE1", "GSE2", "42" );
    }

    @Test
    public void testDatasetArrayArgEmptyRaises() {
        assertThatThrownBy( () -> DatasetArrayArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testGeneArrayArg() {
        GeneArrayArg arg = GeneArrayArg.valueOf( "BRCA1,672,ENSG00000139618" );
        assertThat( arg.getValue() ).containsExactly( "BRCA1", "672", "ENSG00000139618" );
    }

    @Test
    public void testGeneArrayArgEmptyRaises() {
        assertThatThrownBy( () -> GeneArrayArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testTaxonArrayArg() {
        TaxonArrayArg arg = TaxonArrayArg.valueOf( "human,9606,1" );
        assertThat( arg.getValue() ).containsExactly( "human", "9606", "1" );
    }

    @Test
    public void testTaxonArrayArgEmptyRaises() {
        assertThatThrownBy( () -> TaxonArrayArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testPlatformArrayArg() {
        PlatformArrayArg arg = PlatformArrayArg.valueOf( "GPL570,1,GPL96" );
        assertThat( arg.getValue() ).containsExactly( "GPL570", "1", "GPL96" );
    }

    @Test
    public void testPlatformArrayArgEmptyRaises() {
        assertThatThrownBy( () -> PlatformArrayArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testDatabaseEntryArrayArg() {
        DatabaseEntryArrayArg arg = DatabaseEntryArrayArg.valueOf( "GSE1,100" );
        assertThat( arg.getValue() ).containsExactly( "GSE1", "100" );
    }

    @Test
    public void testCompositeSequenceArrayArg() {
        CompositeSequenceArrayArg arg = CompositeSequenceArrayArg.valueOf( "1,201234_at" );
        assertThat( arg.getValue() ).containsExactly( "1", "201234_at" );
    }
}

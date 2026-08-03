package ubic.gemma.core.ontology.lexical;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexicalOntologyIndexTest {

    private static final List<LexicalTerm> TERMS = List.of(
            new LexicalTerm( "u:1", "HeLa", List.of( "Hela", "HeLa-CCL2" ) ),
            new LexicalTerm( "u:2", "HEK293T", List.of( "293T" ) ),
            new LexicalTerm( "u:3", "MCF-7", List.of() )
    );

    @Test
    void buildAndSearchByLabel() throws Exception {
        try ( LexicalOntologyIndex idx = LexicalOntologyIndex.build( TERMS, Collections.emptySet() ) ) {
            assertEquals( "u:1", idx.search( "HeLa", 10 ).get( 0 ).uri() );
            assertEquals( "u:2", idx.search( "HEK293T", 10 ).get( 0 ).uri() );
            assertEquals( "u:3", idx.search( "MCF-7", 10 ).get( 0 ).uri() );
        }
    }

    @Test
    void matchesViaSynonym() throws Exception {
        try ( LexicalOntologyIndex idx = LexicalOntologyIndex.build( TERMS, Collections.emptySet() ) ) {
            assertEquals( "u:2", idx.search( "293T", 10 ).get( 0 ).uri() );
        }
    }

    @Test
    void noMatchReturnsEmpty() throws Exception {
        try ( LexicalOntologyIndex idx = LexicalOntologyIndex.build( TERMS, Collections.emptySet() ) ) {
            assertTrue( idx.search( "zzznotacellline", 10 ).isEmpty() );
        }
    }

    @Test
    void blankQueryRejected() throws Exception {
        try ( LexicalOntologyIndex idx = LexicalOntologyIndex.build( TERMS, Collections.emptySet() ) ) {
            assertThrows( IllegalArgumentException.class, () -> idx.search( "  ", 10 ) );
        }
    }
}

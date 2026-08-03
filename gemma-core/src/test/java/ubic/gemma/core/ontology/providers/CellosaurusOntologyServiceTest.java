package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.search.OntologySearchResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellosaurusOntologyServiceTest {

    /**
     * A tiny Cellosaurus OBO covering: the {@code name:} no-space quirk (HEK293T), synonyms, and an
     * obsolete term that must be dropped, plus a Typedef stanza that must be ignored.
     */
    private static final String OBO = String.join( "\n",
            "format-version: 1.2",
            "data-version: 99.0",
            "ontology: cellosaurus",
            "",
            "[Term]",
            "id: CVCL_0030",
            "name: HeLa",
            "synonym: \"Hela\" RELATED []",
            "synonym: \"HeLa-CCL2\" RELATED []",
            "xref: NCBI_TaxID:9606 ! Homo sapiens (Human)",
            "",
            "[Term]",
            "id: CVCL_0063",
            "name:HEK293T",
            "synonym: \"293T\" RELATED []",
            "",
            "[Term]",
            "id: CVCL_9999",
            "name: OldRetiredLine",
            "is_obsolete: true",
            "",
            "[Typedef]",
            "id: derived_from",
            "name: derived from",
            "" );

    private static final String HELA = "https://www.cellosaurus.org/CVCL_0030";
    private static final String HEK = "https://www.cellosaurus.org/CVCL_0063";
    private static final String OBSOLETE = "https://www.cellosaurus.org/CVCL_9999";

    private CellosaurusOntologyService load() {
        CellosaurusOntologyService s = new CellosaurusOntologyService();
        s.initialize( new ByteArrayInputStream( OBO.getBytes( StandardCharsets.UTF_8 ) ), true );
        return s;
    }

    @Test
    void parsesTermsAndDropsObsolete() {
        CellosaurusOntologyService s = load();
        assertTrue( s.isOntologyLoaded() );
        assertEquals( 2, s.getAllURIs().size() );
        assertTrue( s.getAllURIs().contains( HELA ) );
        assertFalse( s.getAllURIs().contains( OBSOLETE ) );
        assertEquals( "99.0", s.getVersion() );
    }

    @Test
    void getTermResolvesCanonicalUri() {
        CellosaurusOntologyService s = load();
        OntologyTerm hela = s.getTerm( HELA );
        assertNotNull( hela );
        assertEquals( "HeLa", hela.getLabel() );
        assertNull( s.getTerm( OBSOLETE ) );
    }

    @Test
    void findTermByLabelSynonymAndNoSpaceName() throws Exception {
        CellosaurusOntologyService s = load();
        assertTrue( containsUri( s.findTerm( "HeLa", 10 ), HELA ) );
        // synonym match
        assertTrue( containsUri( s.findTerm( "293T", 10 ), HEK ) );
        // the "name:HEK293T" no-space quirk must still be searchable
        assertTrue( containsUri( s.findTerm( "HEK293T", 10 ), HEK ) );
    }

    private static boolean containsUri( Collection<OntologySearchResult<OntologyTerm>> results, String uri ) {
        return results.stream().anyMatch( r -> uri.equals( r.getResult().getUri() ) );
    }
}

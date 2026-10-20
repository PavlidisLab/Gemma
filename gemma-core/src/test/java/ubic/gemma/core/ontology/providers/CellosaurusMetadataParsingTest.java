package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.lexical.LexicalTerm;
import ubic.gemma.core.ontology.lexical.LexicalTermMetadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the descriptive metadata the Cellosaurus OBO parser recovers — species, cell-line type, donor sex,
 * comment, and the problematic-entry flag. All of it used to be dropped at parse time, leaving a client
 * with a bare cell-line name and no way to tell a human line from a rat one.
 * <p>
 * The fixture is hand-written rather than a slice of the real 118 MB OBO: every stanza here exists to
 * pin one decision, and a real slice would not contain the awkward cases (a two-species hybridoma, the
 * two rats) except by luck.
 */
class CellosaurusMetadataParsingTest {

    /**
     * Stanzas, in order: a plain human line; a hybridoma deriving from TWO organisms; a misidentified
     * line; the two rats; and an obsolete entry that must not survive.
     */
    private static final String OBO = String.join( "\n",
            "format-version: 1.2",
            "data-version: 56.0",
            "",
            "[Term]",
            "id: CVCL_0030",
            "name: HeLa",
            "synonym: \"Hela\" RELATED []",
            "subset: Cancer_cell_line",
            "subset: Female",
            "xref: NCBI_TaxID:9606 ! Homo sapiens (Human)",
            "comment: \"Part of: some panel. Derived from sampling site: Uterus.\"",
            "",
            "[Term]",
            "id: CVCL_B0T8",
            "name: Mouse-human hybridoma",
            "subset: Hybridoma",
            "xref: NCBI_TaxID:10090 ! Mus musculus (Mouse)",
            "xref: NCBI_TaxID:9606 ! Homo sapiens (Human)",
            "",
            "[Term]",
            "id: CVCL_1234",
            "name: KB",
            "subset: Cancer_cell_line",
            "xref: NCBI_TaxID:9606 ! Homo sapiens (Human)",
            "comment: \"Problematic cell line: Misidentified/contaminated. Shown to be a HeLa derivative.\"",
            "",
            "[Term]",
            "id: CVCL_RN01",
            "name: Norway rat line",
            "xref: NCBI_TaxID:10116 ! Rattus norvegicus (Rat)",
            "",
            "[Term]",
            "id: CVCL_RR01",
            "name: Black rat line",
            "xref: NCBI_TaxID:10117 ! Rattus rattus (Black rat)",
            "",
            "[Term]",
            "id: CVCL_DEAD",
            "name: Retired line",
            "xref: NCBI_TaxID:9606 ! Homo sapiens (Human)",
            "is_obsolete: true",
            "" );

    private Map<String, LexicalTerm> parse() throws IOException {
        Collection<LexicalTerm> terms = new CellosaurusOntologyService()
                .parse( new ByteArrayInputStream( OBO.getBytes( StandardCharsets.UTF_8 ) ) );
        return terms.stream().collect( Collectors.toMap( LexicalTerm::uri, Function.identity() ) );
    }

    private LexicalTermMetadata meta( Map<String, LexicalTerm> byUri, String id ) {
        LexicalTerm t = byUri.get( CellosaurusOntologyService.URI_PREFIX + id );
        assertNotNull( t, id + " should have been parsed" );
        return t.metadata();
    }

    @Test
    @DisplayName("species, cell-line type, sex and comment are recovered; obsolete entries are still dropped")
    void basicMetadata() throws IOException {
        Map<String, LexicalTerm> byUri = parse();
        assertEquals( 5, byUri.size(), "the obsolete entry must not survive" );

        LexicalTermMetadata m = meta( byUri, "CVCL_0030" );
        assertEquals( 1, m.species().size() );
        assertEquals( 9606, m.species().get( 0 ).ncbiTaxonId() );
        assertEquals( "Homo sapiens (Human)", m.species().get( 0 ).label() );
        assertEquals( "Cancer cell line", m.cellLineType() );
        assertEquals( "Female", m.sex(), "sex must not be reported as a cell-line type" );
        assertNotNull( m.comment() );
        assertNull( m.problematic() );
    }

    /**
     * The reason species is a list. Collapsing a mouse-human hybridoma to one taxon would state a fact the
     * source does not.
     */
    @Test
    @DisplayName("a hybridoma keeps BOTH of the organisms it derives from")
    void hybridomaKeepsBothSpecies() throws IOException {
        LexicalTermMetadata m = meta( parse(), "CVCL_B0T8" );
        assertEquals( 2, m.species().size() );
        assertTrue( m.species().stream().anyMatch( s -> s.ncbiTaxonId() == 10090 ) );
        assertTrue( m.species().stream().anyMatch( s -> s.ncbiTaxonId() == 9606 ) );
        assertEquals( "Hybridoma", m.cellLineType() );
        assertNull( m.sex(), "the source says nothing about sex here; null means 'not stated', not 'unknown sex'" );
    }

    @Test
    @DisplayName("a misidentified line is flagged with its reason, read out of the free-text comment")
    void problematicFlag() throws IOException {
        LexicalTermMetadata m = meta( parse(), "CVCL_1234" );
        assertEquals( "Misidentified/contaminated", m.problematic() );
    }

    /**
     * The whole argument for reporting {@code ncbiTaxonId} rather than a name. Both of these read as "rat"
     * and their labels differ by one word; only the id separates them without ambiguity.
     */
    @Test
    @DisplayName("Rattus norvegicus and Rattus rattus stay distinguishable")
    void theTwoRats() throws IOException {
        Map<String, LexicalTerm> byUri = parse();
        assertEquals( 10116, meta( byUri, "CVCL_RN01" ).species().get( 0 ).ncbiTaxonId() );
        assertEquals( 10117, meta( byUri, "CVCL_RR01" ).species().get( 0 ).ncbiTaxonId() );
    }

    /**
     * Not a filter test — a no-filter test. Both rats, and every other species, must survive parsing;
     * scoping is the caller's job and a filter here would silently shrink the catalogue.
     */
    @Test
    @DisplayName("nothing is dropped on grounds of species")
    void noSpeciesFiltering() throws IOException {
        Map<String, LexicalTerm> byUri = parse();
        assertNotNull( byUri.get( CellosaurusOntologyService.URI_PREFIX + "CVCL_RR01" ),
                "Rattus rattus is not a species Gemma supports, and must be searchable anyway" );
    }
}

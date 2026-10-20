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

/**
 * Pins what the MGI strain parser reports.
 * <p>
 * MGI differs from Cellosaurus in a way worth stating: {@code MGI_Strain.rpt} has no species column at
 * all, so the species here is ASSERTED by Gemma from the fact that MGI ships mouse strains, not read from
 * the file. These tests pin the assertion so that if it is ever revisited, something fails loudly rather
 * than every strain quietly staying labelled as a mouse.
 */
class MgiStrainMetadataParsingTest {

    private static final String RPT = String.join( "\n",
            "MGI:2160170\t101\tinbred strain",
            "MGI:7571118\t101;C3HCat2<ns>/Hmgu\tNot Applicable",
            "MGI:4821385\t101/H-Tbob/H\tcoisogenic",
            "MGI:9999999\tSomeStrain\tNot Specified",
            "MGI:8888888\tNoTypeColumn",
            "" );

    private Map<String, LexicalTerm> parse() throws IOException {
        Collection<LexicalTerm> terms = new MgiStrainOntologyService()
                .parse( new ByteArrayInputStream( RPT.getBytes( StandardCharsets.UTF_8 ) ) );
        return terms.stream().collect( Collectors.toMap( LexicalTerm::uri, Function.identity() ) );
    }

    private LexicalTermMetadata meta( Map<String, LexicalTerm> byUri, String id ) {
        LexicalTerm t = byUri.get( MgiStrainOntologyService.URI_PREFIX + id );
        assertNotNull( t, id + " should have been parsed" );
        return t.metadata();
    }

    @Test
    @DisplayName("strain type is carried through")
    void strainType() throws IOException {
        Map<String, LexicalTerm> byUri = parse();
        assertEquals( "inbred strain", meta( byUri, "MGI:2160170" ).strainType() );
        assertEquals( "coisogenic", meta( byUri, "MGI:4821385" ).strainType() );
    }

    /**
     * The report writes these placeholders where it has nothing to say. Passing them through would dress
     * "unknown" up as a strain type, which reads as a fact on the wire.
     */
    @Test
    @DisplayName("'Not Applicable' / 'Not Specified' / a missing column all report null, not a type")
    void placeholdersBecomeNull() throws IOException {
        Map<String, LexicalTerm> byUri = parse();
        assertNull( meta( byUri, "MGI:7571118" ).strainType() );
        assertNull( meta( byUri, "MGI:9999999" ).strainType() );
        assertNull( meta( byUri, "MGI:8888888" ).strainType() );
    }

    /**
     * 🛑 This is an ASSUMPTION, not data — the source file has no species column. If MGI ever ships
     * non-mouse strains in this report, this test is the thing that should be revisited first.
     */
    @Test
    @DisplayName("every strain is asserted to be Mus musculus, because the source says nothing")
    void speciesIsAssertedMouse() throws IOException {
        Map<String, LexicalTerm> byUri = parse();
        for ( String id : new String[] { "MGI:2160170", "MGI:4821385", "MGI:8888888" } ) {
            LexicalTermMetadata m = meta( byUri, id );
            assertEquals( 1, m.species().size() );
            assertEquals( 10090, m.species().get( 0 ).ncbiTaxonId(),
                    "10090 is Mus musculus; note 10116/10117 are the two rats this must never become by accident" );
        }
    }

    @Test
    @DisplayName("cell-line fields stay empty — an MGI strain is not a cell line")
    void noCellLineFields() throws IOException {
        LexicalTermMetadata m = meta( parse(), "MGI:2160170" );
        assertNull( m.cellLineType() );
        assertNull( m.sex() );
        assertNull( m.problematic() );
    }
}

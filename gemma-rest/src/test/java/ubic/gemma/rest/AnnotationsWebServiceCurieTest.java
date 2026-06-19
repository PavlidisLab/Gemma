package ubic.gemma.rest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link AnnotationsWebService#expandTermQueryToUri}, the CURIE / term-URI
 * resolver that lets {@code GET /annotations/search?query=EFO:0600015} take the same exact-URI
 * lookup path as a full term URI. No Spring context — the resolver is a static function.
 *
 * @author curie
 */
class AnnotationsWebServiceCurieTest {

    @Test
    void efoCurieExpandsToEbiBase() {
        // EFO is the notable non-PURL exception.
        assertThat( AnnotationsWebService.expandTermQueryToUri( "EFO:0600015" ) )
                .isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0600015" );
    }

    @Test
    void oboPurlCuriesExpandToPurlBase() {
        assertThat( AnnotationsWebService.expandTermQueryToUri( "GO:0008150" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/GO_0008150" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "HP:0011438" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/HP_0011438" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "MONDO:0005148" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/MONDO_0005148" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "CL:0000236" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/CL_0000236" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "UBERON:0002107" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/UBERON_0002107" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "CHEBI:15377" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/CHEBI_15377" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "MP:0001262" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/MP_0001262" );
    }

    @Test
    void ncbiTaxonCurieKeepsMixedCaseIdSpace() {
        // The URI id-space casing for NCBITaxon is special-cased (not simply upper-cased).
        assertThat( AnnotationsWebService.expandTermQueryToUri( "NCBITaxon:9606" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/NCBITaxon_9606" );
    }

    @Test
    void prefixMatchIsCaseInsensitive() {
        // Known-id-space lookup is case-insensitive; the URI id-space is canonicalized to the
        // ontology's convention regardless of how the caller typed the prefix.
        assertThat( AnnotationsWebService.expandTermQueryToUri( "go:0008150" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/GO_0008150" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "efo:0600015" ) )
                .isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0600015" );
    }

    @Test
    void fullTermUriPassesThroughUnchanged() {
        assertThat( AnnotationsWebService.expandTermQueryToUri( "http://www.ebi.ac.uk/efo/EFO_0600015" ) )
                .isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0600015" );
        assertThat( AnnotationsWebService.expandTermQueryToUri( "http://purl.obolibrary.org/obo/GO_0008150" ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/GO_0008150" );
    }

    @Test
    void unknownPrefixIsNotExpanded() {
        // CURIE-shaped but the prefix is not a recognized ontology id space → free-text fallback.
        assertThat( AnnotationsWebService.expandTermQueryToUri( "FOO:0001" ) ).isNull();
        assertThat( AnnotationsWebService.expandTermQueryToUri( "bogus:12345" ) ).isNull();
    }

    @Test
    void plainFreeTextIsNotTreatedAsCurie() {
        assertThat( AnnotationsWebService.expandTermQueryToUri( "noise exposure" ) ).isNull();
        assertThat( AnnotationsWebService.expandTermQueryToUri( "tp53" ) ).isNull();
        // A bare word with no colon is obviously not a CURIE.
        assertThat( AnnotationsWebService.expandTermQueryToUri( "cancer" ) ).isNull();
        // Trailing free text after a colon must not be mistaken for a CURIE local id.
        assertThat( AnnotationsWebService.expandTermQueryToUri( "GO:cell cycle" ) ).isNull();
    }

    @Test
    void leadingAndTrailingWhitespaceIsTolerated() {
        assertThat( AnnotationsWebService.expandTermQueryToUri( "  GO:0008150  " ) )
                .isEqualTo( "http://purl.obolibrary.org/obo/GO_0008150" );
    }

    @Test
    void nullAndBlankReturnNull() {
        assertThat( AnnotationsWebService.expandTermQueryToUri( null ) ).isNull();
        assertThat( AnnotationsWebService.expandTermQueryToUri( "" ) ).isNull();
        assertThat( AnnotationsWebService.expandTermQueryToUri( "   " ) ).isNull();
    }
}

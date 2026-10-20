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
            // Verbatim shapes from the real OBO: the ATCC catalogue number is the ONLY place HTB-122
            // appears for BT-549 -- never a name, never a synonym -- alongside cross-references that
            // must not be indexed.
            "[Term]",
            "id: CVCL_1092",
            "name: BT-549",
            "synonym: \"BT 549\" RELATED []",
            "xref: ATCC:HTB-122",
            "xref: PubMed:9671407",
            "xref: Wikidata:Q54783197",
            "xref: CLO:CLO_0002181",
            "xref: CCRID:3111C0001CCC000418",
            "xref: CLDB:12345",
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
    private static final String BT549 = "https://www.cellosaurus.org/CVCL_1092";

    private CellosaurusOntologyService load() {
        CellosaurusOntologyService s = new CellosaurusOntologyService();
        s.initialize( new ByteArrayInputStream( OBO.getBytes( StandardCharsets.UTF_8 ) ), true );
        return s;
    }

    @Test
    void parsesTermsAndDropsObsolete() {
        CellosaurusOntologyService s = load();
        assertTrue( s.isOntologyLoaded() );
        assertEquals( 3, s.getAllURIs().size(), "HeLa, HEK293T and BT-549; the obsolete one is dropped" );
        assertTrue( s.getAllURIs().contains( HELA ) );
        assertTrue( s.getAllURIs().contains( BT549 ) );
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

    /**
     * The search fan-out ranks hits from supplementary sources below every conventional-ontology hit; the
     * lexical index's exact-name boost would otherwise displace ontology terms, since the two sets of scores
     * come from different indices and are not on a common scale.
     */
    @Test
    void isSupplementaryRatherThanAPeerOfTheConventionalOntologies() {
        assertTrue( new CellosaurusOntologyService().isSupplementary() );
        assertTrue( new MgiStrainOntologyService().isSupplementary() );
        assertFalse( new ChebiOntologyService().isSupplementary() );
    }

    private static boolean containsUri( Collection<OntologySearchResult<OntologyTerm>> results, String uri ) {
        return results.stream().anyMatch( r -> uri.equals( r.getResult().getUri() ) );
    }

    // ============================================================================================
    // Catalogue numbers — an ATCC/Coriell number is what a submitter writes, and Cellosaurus keeps
    // it only in xref:. Reported by Paul 2026-08-26: HTB-122 and CRL-7250 resolved to nothing.
    // ============================================================================================

    /** The reported bug: an ATCC catalogue number reaches the cell line it names. */
    @Test
    void anAtccCatalogueNumberResolvesToItsCellLine() throws Exception {
        assertTrue( containsUri( load().findTerm( "HTB-122", 10 ), BT549 ),
                "HTB-122 is BT-549's ATCC number and appears nowhere else in the entry" );
    }

    /**
     * Paul reported the identifiers UNHYPHENATED ({@code HTB122}), and the indexed value is ATCC's
     * hyphenated {@code HTB-122}. They meet because {@code OntologyAnalyzers.CODE_RUN} folds a
     * designation-shaped run at both index and query time with the separator optional — prefix of up to
     * five letters, then three or more digits. {@code SHORT_CODE_RUN}, the ≤3-letter/1–2-digit rule, is a
     * different case and does not reach these.
     * <p>
     * Asserted rather than reasoned about: indexing the catalogue number would be worth little if the
     * spelling people actually type did not reach it.
     */
    @Test
    void theUnhyphenatedSpellingReachesTheHyphenatedCatalogueNumber() throws Exception {
        CellosaurusOntologyService s = load();
        assertTrue( containsUri( s.findTerm( "HTB122", 10 ), BT549 ),
                "HTB122 is what a submitter writes; ATCC spells it HTB-122" );
        assertTrue( containsUri( s.findTerm( "htb-122", 10 ), BT549 ), "and case must not matter" );
    }

    /**
     * 🛑 The whitelist is the point. Most cross-references are not catalogue numbers, and indexing them
     * would put publication and taxon identifiers into a cell-line search.
     */
    @Test
    void nonCatalogueCrossReferencesAreNotIndexed() {
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: PubMed:9671407" ) );
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: Wikidata:Q54783197" ) );
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: NCBI_TaxID:9606 ! Homo sapiens (Human)" ) );
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: DOI:10.1000/xyz" ) );
    }

    /**
     * CLO / EFO / BTO are clean identifiers and still excluded: they are the conventional ontologies this
     * catalogue BACKS UP, so echoing their accessions would answer with a supplementary hit for a term the
     * real ontology already serves.
     */
    @Test
    void theOntologiesThisBacksUpAreNotEchoedBackAsCatalogueNumbers() {
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: CLO:CLO_0002181" ) );
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: EFO:EFO_0001086" ) );
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: BTO:BTO_0000567" ) );
    }

    /**
     * 🛑 Bare numbers are dropped. Measured over the whole OBO, 10,057 of the 40,484 otherwise-new
     * catalogue values are pure digits; nobody types "60053" meaning a cell line, and indexing them puts
     * ten thousand numeric tokens into an index shared with every other ontology.
     */
    @Test
    void purelyNumericCatalogueValuesAreDropped() {
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: CLDB:12345" ) );
        assertNull( CellosaurusOntologyService.catalogueNumber( "xref: CCRID:0000123" ) );
        assertNotNull( CellosaurusOntologyService.catalogueNumber( "xref: CCRID:3111C0001CCC000418" ),
                "alphanumeric registry codes are kept -- only bare digits go" );
    }

    /** The namespaces a curator actually writes, in the shapes the real file uses. */
    @Test
    void theBiobankNamespacesAreRecognised() {
        assertEquals( "HTB-122", CellosaurusOntologyService.catalogueNumber( "xref: ATCC:HTB-122" ) );
        assertEquals( "CRL-7250", CellosaurusOntologyService.catalogueNumber( "xref: ATCC:CRL-7250" ) );
        assertEquals( "AG25367", CellosaurusOntologyService.catalogueNumber( "xref: Coriell:AG25367" ) );
        assertEquals( "ACC 802", CellosaurusOntologyService.catalogueNumber( "xref: DSMZ:ACC 802" ) );
    }
}

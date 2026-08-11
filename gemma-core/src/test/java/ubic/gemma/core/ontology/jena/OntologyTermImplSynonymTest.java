package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the {@code /annotations/search?query=...&matchedVia=null} report against a
 * TGEMO-shaped OWL fragment loaded into a real Jena model. The handoff
 * {@code GEMBROW_UIB_HANDOFF_2026-06-12_ANNOTATION_SEARCH_MATCHEDVIA.md} says the matchedVia
 * field comes back null for TGEMO_00210 ("behavioural stress") even when the query EQUALS the
 * canonical label or an exact synonym in the OWL.
 *
 * <p>The endpoint's {@code computeMatchAttribution} probes the term's {@code rdfs:label} and
 * {@code oboInOwl:hasExactSynonym} values via {@link OntologyTermImpl#getLabel()} and
 * {@link OntologyTermImpl#getAnnotations(String)}. If either returns null/empty for a term
 * whose OWL clearly declares the value, matchedVia silently degrades to null and the curation
 * agent can't distinguish a real exact-synonym hit from a generic Lucene fallback. This test
 * pins both surfaces against a Jena model built from inlined OWL that mirrors the TGEMO
 * declaration shape (AnnotationProperty + xml:lang="en" label + multiple hasExactSynonym
 * literals).
 */
class OntologyTermImplSynonymTest {

    private static final String OBO_EXACT_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
    private static final String OBO_DB_XREF = "http://www.geneontology.org/formats/oboInOwl#hasDbXref";

    /**
     * Real Jena, real OWL parse — no mocks. Mirrors TGEMO_00210's shape exactly:
     * <ul>
     *     <li>hasExactSynonym declared as {@code owl:AnnotationProperty} (matches TGEMO.OWL line 162)</li>
     *     <li>label uses {@code xml:lang="en"} (lowercase — TGEMO uses lowercase)</li>
     *     <li>two distinct {@code hasExactSynonym} values, no other labels/synonyms</li>
     * </ul>
     */
    private static final String TGEMO_FRAGMENT =
            "<?xml version=\"1.0\"?>\n" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
            "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n" +
            "         xmlns:oboInOwl=\"http://www.geneontology.org/formats/oboInOwl#\">\n" +
            "  <owl:Ontology rdf:about=\"http://example.org/tgemo-fixture\"/>\n" +
            "  <owl:AnnotationProperty rdf:about=\"" + OBO_EXACT_SYNONYM + "\">\n" +
            "    <rdfs:label>has_exact_synonym</rdfs:label>\n" +
            "  </owl:AnnotationProperty>\n" +
            "  <owl:AnnotationProperty rdf:about=\"" + OBO_DB_XREF + "\">\n" +
            "    <rdfs:label>database_cross_reference</rdfs:label>\n" +
            "  </owl:AnnotationProperty>\n" +
            "  <owl:Class rdf:about=\"http://gemma.msl.ubc.ca/ont/TGEMO_00210\">\n" +
            "    <rdfs:label xml:lang=\"en\">behavioural stress</rdfs:label>\n" +
            "    <oboInOwl:hasExactSynonym>behavioral stress</oboInOwl:hasExactSynonym>\n" +
            "    <oboInOwl:hasExactSynonym>psychological stress</oboInOwl:hasExactSynonym>\n" +
            "    <oboInOwl:hasDbXref>MESH:D013315</oboInOwl:hasDbXref>\n" +
            "    <oboInOwl:hasDbXref>UMLS:C0038443</oboInOwl:hasDbXref>\n" +
            "  </owl:Class>\n" +
            "</rdf:RDF>\n";

    private OntologyTerm loadTgemoTerm() {
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        model.read( new ByteArrayInputStream( TGEMO_FRAGMENT.getBytes( StandardCharsets.UTF_8 ) ), null );
        OntClass cls = model.getOntClass( "http://gemma.msl.ubc.ca/ont/TGEMO_00210" );
        assertThat( cls ).as( "Jena must find the test class by URI" ).isNotNull();
        return new OntologyTermImpl( cls, Collections.emptySet() );
    }

    @Test
    void labelResolvesEvenWithXmlLangEnTag() {
        // computeMatchAttribution's canonical_label branch reads term.getLabel(). If this returns
        // null for an xml:lang="en" label, every TGEMO matchedVia for a canonical-label query is
        // silently null and the curation agent falls back to client-side spelling tables.
        OntologyTerm term = loadTgemoTerm();
        assertThat( term.getLabel() )
                .as( "label declared with xml:lang=\"en\" must resolve via the EN-then-null fallback" )
                .isEqualTo( "behavioural stress" );
    }

    @Test
    void getAnnotationsReturnsAllHasExactSynonymValues() {
        // computeMatchAttribution iterates the term's exact-synonym values to match against the
        // query (normalised equality). If getAnnotations returns empty for an OBO-style
        // hasExactSynonym property, every synonym-driven matchedVia degrades to null.
        OntologyTerm term = loadTgemoTerm();
        Collection<AnnotationProperty> synonyms = term.getAnnotations( OBO_EXACT_SYNONYM );
        assertThat( synonyms )
                .as( "TGEMO_00210 declares two hasExactSynonym literals; both must surface so the "
                        + "endpoint can attribute matchedVia=exact_synonym for either spelling" )
                .extracting( AnnotationProperty::getContents )
                .containsExactlyInAnyOrder( "behavioral stress", "psychological stress" );
    }

    /**
     * EFO shipped {@code "cancer cell line "} with a trailing space on EFO_0001639, and that one
     * character dropped a 50-use term below a zero-use exact-label duplicate in
     * {@code /annotations/search}: the relevance tiers decide "exact label" with an equals(), so a
     * label that differs from its own clean form scores as a weaker match than a worse term. The
     * space is gone upstream but survives in any Lucene index built before the fix. Labels are
     * third-party text; normalize where they enter Gemma rather than at each comparison.
     */
    @Test
    void labelWhitespaceIsNormalizedAtTheModelBoundary() {
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        String fragment = TGEMO_FRAGMENT.replace(
                "<rdfs:label xml:lang=\"en\">behavioural stress</rdfs:label>",
                "<rdfs:label xml:lang=\"en\">  behavioural   stress </rdfs:label>" );
        model.read( new ByteArrayInputStream( fragment.getBytes( StandardCharsets.UTF_8 ) ), null );
        OntClass cls = model.getOntClass( "http://gemma.msl.ubc.ca/ont/TGEMO_00210" );
        OntologyTerm term = new OntologyTermImpl( cls, Collections.emptySet() );

        // leading/trailing stripped AND the internal run collapsed -- both shapes occur in the
        // wild, and both break an equals()-based exact-label comparison identically.
        assertThat( term.getLabel() ).isEqualTo( "behavioural stress" );
    }

    @Test
    void aLabelThatIsOnlyWhitespaceDoesNotBecomeAnEmptyName() {
        // "no label" and "blank label" must stay distinguishable: getTerm() drops terms whose label
        // is null, and turning whitespace into "" would smuggle an unnamed term onto the wire with
        // an empty name instead.
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        String fragment = TGEMO_FRAGMENT.replace(
                "<rdfs:label xml:lang=\"en\">behavioural stress</rdfs:label>",
                "<rdfs:label xml:lang=\"en\">   </rdfs:label>" );
        model.read( new ByteArrayInputStream( fragment.getBytes( StandardCharsets.UTF_8 ) ), null );
        OntClass cls = model.getOntClass( "http://gemma.msl.ubc.ca/ont/TGEMO_00210" );
        OntologyTerm term = new OntologyTermImpl( cls, Collections.emptySet() );

        assertThat( term.getLabel() ).isEmpty();
    }

    @Test
    void getAnnotationsReturnsHasDbXrefValues() {
        // /annotations/term surfaces class-level oboInOwl:hasDbXref values as the term's dbXrefs. They
        // travel the same getAnnotations(uri) path as synonyms, which only returns a value when the
        // predicate is declared as an owl:AnnotationProperty. This pins that hasDbXref clears that filter.
        OntologyTerm term = loadTgemoTerm();
        Collection<AnnotationProperty> xrefs = term.getAnnotations( OBO_DB_XREF );
        assertThat( xrefs )
                .as( "class-level hasDbXref cross-references must surface for the term's dbXrefs field" )
                .extracting( AnnotationProperty::getContents )
                .containsExactlyInAnyOrder( "MESH:D013315", "UMLS:C0038443" );
    }
}

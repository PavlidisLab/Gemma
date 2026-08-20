package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyXref;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reading MONDO's cross-references off a loaded model, <b>with the mapping qualifier</b>.
 *
 * <p>The RDF below is the shape MONDO really emits, not a convenient simplification, because the whole
 * point is that the qualifier survives the OWL encoding. It does not sit on the assertion — it sits on a
 * separate {@code owl:Axiom} node that reifies the assertion, which is exactly why the flat list of
 * strings the API serves today has no qualifier in it.</p>
 *
 * <p>Reading it wrong is silent. A {@code narrowMatch} taken for an equivalence returns a real,
 * plausible, wrong disease, and nothing downstream can tell.</p>
 */
class CrossReferencesTest {

    private static final String BREAST_ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0004988";
    private static final String ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0004970";
    private static final String LUNG_ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0005061";
    private static final String RETIRED = "http://purl.obolibrary.org/obo/MONDO_0000001";

    private static final String RDF = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
            + "         xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\"\n"
            + "         xmlns:oboInOwl=\"http://www.geneontology.org/formats/oboInOwl#\">\n"
            + "  <owl:AnnotationProperty rdf:about=\"http://www.geneontology.org/formats/oboInOwl#hasDbXref\"/>\n"
            + "  <owl:AnnotationProperty rdf:about=\"http://www.geneontology.org/formats/oboInOwl#source\"/>\n"
            + "  <owl:Class rdf:about=\"" + BREAST_ADENOCARCINOMA + "\">\n"
            + "    <rdfs:label>breast adenocarcinoma</rdfs:label>\n"
            + "    <oboInOwl:hasDbXref>DOID:3458</oboInOwl:hasDbXref>\n"
            // prefix case genuinely varies inside one artifact, hence NCIt rather than NCIT here
            + "    <oboInOwl:hasDbXref>NCIt:C5214</oboInOwl:hasDbXref>\n"
            + "  </owl:Class>\n"
            + "  <owl:Class rdf:about=\"" + ADENOCARCINOMA + "\">\n"
            + "    <rdfs:label>adenocarcinoma</rdfs:label>\n"
            + "    <oboInOwl:hasDbXref>DOID:299</oboInOwl:hasDbXref>\n"
            + "  </owl:Class>\n"
            + "  <owl:Class rdf:about=\"" + LUNG_ADENOCARCINOMA + "\">\n"
            + "    <rdfs:label>lung adenocarcinoma</rdfs:label>\n"
            + "    <skos:narrowMatch rdf:resource=\"http://purl.obolibrary.org/obo/DOID_3910\"/>\n"
            + "  </owl:Class>\n"
            // a retired class keeps its label AND its cross-references, which is what makes it
            // dangerous: read naively it is indistinguishable from a live term
            + "  <owl:Class rdf:about=\"" + RETIRED + "\">\n"
            + "    <rdfs:label>obsolete adenocarcinoma of the breast</rdfs:label>\n"
            + "    <owl:deprecated rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:deprecated>\n"
            + "    <oboInOwl:hasDbXref>DOID:1612</oboInOwl:hasDbXref>\n"
            + "  </owl:Class>\n"
            // the qualifier: an owl:Axiom reifying the DOID:3458 assertion above
            + "  <owl:Axiom>\n"
            + "    <owl:annotatedSource rdf:resource=\"" + BREAST_ADENOCARCINOMA + "\"/>\n"
            + "    <owl:annotatedProperty rdf:resource=\"http://www.geneontology.org/formats/oboInOwl#hasDbXref\"/>\n"
            + "    <owl:annotatedTarget>DOID:3458</owl:annotatedTarget>\n"
            + "    <oboInOwl:source>MONDO:equivalentTo</oboInOwl:source>\n"
            + "  </owl:Axiom>\n"
            // several sources on one axiom, only one of which is a mapping predicate -- MONDO does this
            + "  <owl:Axiom>\n"
            + "    <owl:annotatedSource rdf:resource=\"" + ADENOCARCINOMA + "\"/>\n"
            + "    <owl:annotatedProperty rdf:resource=\"http://www.geneontology.org/formats/oboInOwl#hasDbXref\"/>\n"
            + "    <owl:annotatedTarget>DOID:299</owl:annotatedTarget>\n"
            + "    <oboInOwl:source>EFO:0002616</oboInOwl:source>\n"
            + "    <oboInOwl:source>MONDO:equivalentTo</oboInOwl:source>\n"
            + "  </owl:Axiom>\n"
            + "</rdf:RDF>\n";

    private Collection<OntologyXref> read() {
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        model.read( new ByteArrayInputStream( RDF.getBytes( StandardCharsets.UTF_8 ) ), null );
        return CrossReferences.list( model );
    }

    private Optional<OntologyXref> find( String termUri, String curie ) {
        return read().stream()
                .filter( x -> x.getTermUri().equals( termUri ) && x.getCurie().equals( curie ) )
                .findFirst();
    }

    @Test
    void theAxiomAnnotationBecomesTheMappingQualifier() {
        assertThat( find( BREAST_ADENOCARCINOMA, "DOID:3458" ) )
                .get()
                .extracting( OntologyXref::getStrength )
                .isEqualTo( OntologyXref.Strength.EXACT );
    }

    /**
     * One axiom can carry several {@code source} values where only one names a mapping predicate and the
     * rest are provenance. Reading the first and stopping would file this equivalence as unqualified.
     */
    @Test
    void aMappingPredicateIsFoundAmongProvenanceSources() {
        assertThat( find( ADENOCARCINOMA, "DOID:299" ) )
                .get()
                .extracting( OntologyXref::getStrength )
                .isEqualTo( OntologyXref.Strength.EXACT );
    }

    /**
     * A bare cross-reference with no axiom qualifying it is unspecified — and substitutable, because on
     * an OBO disease class that IS the equivalence claim.
     */
    @Test
    void anUnqualifiedCrossReferenceIsUnspecifiedAndStillSubstitutable() {
        Optional<OntologyXref> x = find( BREAST_ADENOCARCINOMA, "NCIT:C5214" );
        assertThat( x ).get().extracting( OntologyXref::getStrength )
                .isEqualTo( OntologyXref.Strength.UNSPECIFIED );
        assertThat( x.get().getStrength().isSubstitutable() ).isTrue();
    }

    /**
     * {@code NCIt:} and {@code NCIT:} are the same prefix and must key the same entry, or half the
     * index is unreachable depending on which spelling the caller happens to hold.
     */
    @Test
    void thePrefixCaseIsNormalizedButTheLocalIdIsNot() {
        assertThat( find( BREAST_ADENOCARCINOMA, "NCIT:C5214" ) ).isPresent();
        assertThat( find( BREAST_ADENOCARCINOMA, "NCIt:C5214" ) ).isEmpty();
        assertThat( find( BREAST_ADENOCARCINOMA, "NCIT:C5214" ).get().getPrefix() ).isEqualTo( "NCIT" );
    }

    /**
     * The SKOS form is resource-valued and carries its qualifier in the predicate. It has to end up in
     * the same index as the string form, keyed the same way, or a lookup finds one and not the other.
     */
    @Test
    void aSkosMappingIsReadAndKeepsItsDirection() {
        Optional<OntologyXref> x = find( LUNG_ADENOCARCINOMA, "DOID:3910" );
        assertThat( x ).isPresent();
        assertThat( x.get().getStrength() ).isEqualTo( OntologyXref.Strength.NARROW );
        // and a narrow mapping is NOT something to swap one term for another across
        assertThat( x.get().getStrength().isSubstitutable() ).isFalse();
    }

    @Test
    void everyCrossReferenceIsAttributedToTheTermThatDeclaredIt() {
        assertThat( read() )
                .extracting( OntologyXref::getTermUri )
                .containsOnly( BREAST_ADENOCARCINOMA, ADENOCARCINOMA, LUNG_ADENOCARCINOMA, RETIRED );
    }

    /**
     * 🛑 The declaring term's label rides along with the mapping, because a caller inverting these to
     * translate a foreign identifier has to <i>name</i> what it translated to, and the only other place
     * to ask is the loaded model — which may be a corpus-seeded slim that omits precisely the terms a
     * foreign identifier is being translated to reach. Read once, here, or the identifier resolves and
     * the term stays nameless.
     */
    @Test
    void theDeclaringTermsLabelRidesAlongWithTheMapping() {
        assertThat( find( BREAST_ADENOCARCINOMA, "DOID:3458" ) )
                .get()
                .extracting( OntologyXref::getTermLabel )
                .isEqualTo( "breast adenocarcinoma" );
        // the SKOS form is read in a separate pass and must be labelled the same way
        assertThat( find( LUNG_ADENOCARCINOMA, "DOID:3910" ) )
                .get()
                .extracting( OntologyXref::getTermLabel )
                .isEqualTo( "lung adenocarcinoma" );
    }

    /**
     * 🛑 A retired class keeps both its label and its cross-references, so handing back its label would
     * let a consumer store a term MONDO has withdrawn as the object of a relation, with nothing marking
     * it. The mapping itself is still reported — it is a real mapping, and a caller widening a query may
     * want it — but it arrives unnamed, which is what stops it being stored.
     */
    @Test
    void anObsoleteTermIsNotNamed() {
        assertThat( find( RETIRED, "DOID:1612" ) )
                .get()
                .extracting( OntologyXref::getTermLabel )
                .isNull();
    }
}

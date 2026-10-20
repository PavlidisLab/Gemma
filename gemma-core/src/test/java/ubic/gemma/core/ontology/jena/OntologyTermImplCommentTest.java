package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A term whose {@code rdfs:comment} is a resource rather than a literal must still be readable.
 * <p>
 * {@code GET /annotations/term?uri=…/CLO_0009922} answered 500 with the body
 * {@code "http://purl.obolibrary.org/obo/CLO_0009251"} — the term's successor, which made it look like
 * a failed successor lookup. It was the definition fallback: CLO writes
 * {@code <rdfs:comment rdf:resource=".../CLO_0009251"/>} on that tombstone, using the comment slot to
 * point at the replacement instead of to describe the term, and Jena's any-language
 * {@code getComment(null)} demands a literal and raises {@code LiteralRequiredException} carrying the
 * resource's URI. One malformed field cost the caller the whole term: the label, synonyms, xrefs and
 * obsolescence flags were all present and none of them shipped.
 * <p>
 * Both fixtures below are CLO classes verbatim (CLO 2026-06-19), because the failure depends on which
 * shapes co-occur — CLO_0037134 carries a resource comment AND a literal one and answered 200, which is
 * how a term-killing bug hid in an ontology loaded on every deployment.
 */
class OntologyTermImplCommentTest {

    /**
     * CLO_0009922 {@code obsolete T47D cell}: its ONLY {@code rdfs:comment} is a resource. Note it
     * declares no {@code IAO:0100001}, so this comment is the only place the successor appears at all.
     */
    private static final String RESOURCE_COMMENT_ONLY =
            "<?xml version=\"1.0\"?>\n" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
            "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n" +
            "  <owl:Ontology rdf:about=\"http://example.org/clo-fixture\"/>\n" +
            "  <owl:Class rdf:about=\"http://purl.obolibrary.org/obo/CLO_0009922\">\n" +
            "    <rdfs:comment rdf:resource=\"http://purl.obolibrary.org/obo/CLO_0009251\"/>\n" +
            "    <rdfs:label>obsolete T47D cell</rdfs:label>\n" +
            "    <owl:deprecated rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:deprecated>\n" +
            "  </owl:Class>\n" +
            "</rdf:RDF>\n";

    /**
     * CLO_0037134 {@code obsolete SKMEL28 cell}: a resource comment AND a literal one. This is the term
     * that answered 200 — Jena's {@code getRequiredProperty} happened to hand back the literal.
     */
    private static final String BOTH_COMMENTS =
            "<?xml version=\"1.0\"?>\n" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
            "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n" +
            "  <owl:Ontology rdf:about=\"http://example.org/clo-fixture\"/>\n" +
            "  <owl:Class rdf:about=\"http://purl.obolibrary.org/obo/CLO_0037134\">\n" +
            "    <rdfs:comment rdf:resource=\"http://purl.obolibrary.org/obo/CLO_0009043\"/>\n" +
            "    <rdfs:comment>disease: malignant melanoma</rdfs:comment>\n" +
            "    <rdfs:label>obsolete SKMEL28 cell</rdfs:label>\n" +
            "  </owl:Class>\n" +
            "</rdf:RDF>\n";

    private static OntologyTerm load( String owl, String uri ) {
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        model.read( new ByteArrayInputStream( owl.getBytes( StandardCharsets.UTF_8 ) ), null );
        OntClass cls = model.getOntClass( uri );
        assertThat( cls ).as( "Jena must find the test class by URI" ).isNotNull();
        return new OntologyTermImpl( cls, Collections.emptySet() );
    }

    @Test
    void aCommentPointingAtAResourceReadsAsNoComment() {
        // The pointer is not a description, so there is nothing to report as one -- but reporting
        // nothing is the whole requirement. Before the fix this threw LiteralRequiredException, which
        // OntologyServiceImpl.getDefinition propagated out of the endpoint as a 500.
        OntologyTerm term = load( RESOURCE_COMMENT_ONLY, "http://purl.obolibrary.org/obo/CLO_0009922" );
        assertThat( term.getComment() ).isEmpty();
        // the rest of the term was never the problem; pin that it still arrives
        assertThat( term.getLabel() ).isEqualTo( "obsolete T47D cell" );
        assertThat( term.isObsolete() ).isTrue();
    }

    @Test
    void aLiteralCommentIsFoundEvenWhenAResourceCommentIsDeclaredToo() {
        // Jena's any-language accessor takes ONE arbitrary statement, so a term declaring both got the
        // literal or the exception depending on statement order -- CLO_0037134 answered 200 and
        // CLO_0009922 did not, from the same malformed shape. Scanning for the literal removes the
        // coin flip as well as the throw.
        OntologyTerm term = load( BOTH_COMMENTS, "http://purl.obolibrary.org/obo/CLO_0037134" );
        assertThat( term.getComment() ).isEqualTo( "disease: malignant melanoma" );
    }

    @Test
    void aLabelPointingAtAResourceReadsAsNoLabel() {
        // Same strict any-language path, one property over: getLabel() falls back to it whenever a term
        // has no EN-tagged label, which for OBO ontologies is the usual case rather than the exception.
        String owl = RESOURCE_COMMENT_ONLY.replace(
                "<rdfs:label>obsolete T47D cell</rdfs:label>",
                "<rdfs:label rdf:resource=\"http://purl.obolibrary.org/obo/CLO_0009251\"/>" );
        OntologyTerm term = load( owl, "http://purl.obolibrary.org/obo/CLO_0009922" );
        assertThat( term.getLabel() ).isNull();
    }
}

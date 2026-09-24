package ubic.gemma.core.ontology.jena;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.OntologyService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that a {@code subClassOf} leaving the ontology's own namespace survives the allowed-URI-prefix filter.
 *
 * <h2>What this reproduces</h2>
 *
 * <p>Reported in {@code CAB_EVAL_TO_GEMBRO_2026_09_07_ANNOTATIONS_PARENTS_STOPS_AT_THE_ONTOLOGY_BOUNDARY.md}:
 * {@code /annotations/parents?uri=…/EFO_0001643} ("kidney derived cell line") answered zero ancestors, while
 * OLS gives it {@code CL:0000010} and Gemma knows {@code CL_0000010} perfectly well — 4 ancestors of its own,
 * usageCount 30. 14 of 38 EFO terms in a 500-experiment reference set behaved this way, and all 14 had a parent
 * outside EFO while none of the 24 that worked did.</p>
 *
 * <p>Every ontology bean is built with an allowed-URI prefix — EFO's is
 * {@code http://www.ebi.ac.uk/efo/EFO_} ({@code OntologyConfig}) — and
 * {@link AbstractOntologyService#getParents(java.util.Collection, boolean, boolean, boolean)} applied it to the
 * walk's RESULTS. A parent in another vocabulary therefore could not be returned by the ontology that asserts
 * the edge, and no other ontology holds the child, so the answer was empty. The prefix set says which terms an
 * ontology SERVES, which is the right question for {@code getTerm} and for search hits and the wrong one for
 * the object of an axiom.</p>
 *
 * <p>{@code OntologyServiceImpl.getParentsOrChildren} is already built for foreign parents: it re-queries new
 * results against every ontology "i.e. a term was inferred from a different ontology", and re-fetches labels for
 * results that come back without one because they "might be referring to another ontology". The filter defeated
 * both.</p>
 *
 * <p>Real Jena parse of inlined OWL, no network.</p>
 */
class AbstractOntologyServiceCrossOntologyParentTest {

    private static final String EFO_URI = "http://www.ebi.ac.uk/efo/EFO_9999001";
    private static final String EFO_CHILD_URI = "http://www.ebi.ac.uk/efo/EFO_9999002";
    private static final String FOREIGN_PARENT_URI = "http://purl.obolibrary.org/obo/CL_9999010";

    /**
     * The EFO_0001643 shape: an EFO class whose only named superclass is an OBO-PURL class from another
     * vocabulary, alongside one that stays inside EFO as a control.
     */
    private static final String OWL = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n"
            + "  <owl:Ontology rdf:about=\"http://example.org/test\"/>\n"
            + "  <owl:Class rdf:about=\"" + EFO_URI + "\">\n"
            + "    <rdfs:label>kidney derived cell line</rdfs:label>\n"
            + "    <rdfs:subClassOf rdf:resource=\"" + FOREIGN_PARENT_URI + "\"/>\n"
            + "  </owl:Class>\n"
            + "  <owl:Class rdf:about=\"" + EFO_CHILD_URI + "\">\n"
            + "    <rdfs:label>a narrower cell line</rdfs:label>\n"
            + "    <rdfs:subClassOf rdf:resource=\"" + EFO_URI + "\"/>\n"
            + "  </owl:Class>\n"
            + "</rdf:RDF>\n";

    private static OntologyService load() {
        UrlOntologyService svc = new UrlOntologyService( "test", "http://example.org/test", true, null );
        svc.setSearchEnabled( false );
        svc.setProcessImports( false );
        // exactly what OntologyConfig gives the EFO bean
        svc.setAllowedUriPrefixes( "http://www.ebi.ac.uk/efo/EFO_" );
        svc.initialize( new ByteArrayInputStream( OWL.getBytes( StandardCharsets.UTF_8 ) ), false );
        return svc;
    }

    private static Set<String> parentUris( OntologyService svc, String uri ) {
        OntologyTerm term = svc.getTerm( uri );
        assertThat( term ).isNotNull();
        return svc.getParents( Collections.singleton( term ), true, false, true ).stream()
                .map( OntologyTerm::getUri )
                .collect( Collectors.toSet() );
    }

    /** The reported case: the edge is asserted here, so this ontology is the only one that can report it. */
    @Test
    void aSubClassOfLeavingTheNamespaceIsStillReported() {
        assertThat( parentUris( load(), EFO_URI ) ).contains( FOREIGN_PARENT_URI );
    }

    /** Control: the 24 of 38 that already worked. */
    @Test
    void aSubClassOfInsideTheNamespaceIsUnaffected() {
        assertThat( parentUris( load(), EFO_CHILD_URI ) ).contains( EFO_URI );
    }

    /**
     * The prefix set keeps its real job. An ontology serves its own terms, so asking EFO for a CL URI answers
     * nothing even though the class is mentioned in EFO's file — otherwise every ontology referencing a
     * vocabulary would start claiming to serve it.
     */
    @Test
    void theOntologyStillDoesNotServeAForeignUri() {
        assertThat( load().getTerm( FOREIGN_PARENT_URI ) ).isNull();
    }
}

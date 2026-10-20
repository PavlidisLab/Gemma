package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyClassRestriction;
import ubic.gemma.core.ontology.model.OntologyRestriction;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every restriction a class carries has to survive being read, and they were collapsing to one.
 *
 * <p>🛑 A restriction is a BLANK NODE: no URI, no label. {@code AbstractOntologyResource} hashes on
 * {@code getUri()} — a constant for every restriction — and its {@code equals} falls back to comparing
 * {@code getLabel()} when both URIs are null, which is null on both. So any two restrictions are
 * "equal" and land in the same bucket, and the {@code new HashSet<>()} these methods collect into keeps
 * exactly ONE per class: whichever {@code listSuperClasses} happened to yield first.</p>
 *
 * <p>That is also why the counts moved between runs. Which restriction survives depends on iteration
 * order, so the producer read CHEBI at 11,413 / 11,393 / 11,478 across three runs of one unchanged
 * artifact, and CLO's {@code CLO_0000179} at 441 / 1,000 / 1,899 before that. The closure-walk fix
 * addressed the wrong half: {@code getRestrictions()} and {@code getDirectRestrictions()} collect into
 * the same broken set.</p>
 *
 * <p>The fixture is the real shape rather than a convenient one — a CHEBI chemical bearing three roles
 * on the SAME property, which is what {@code RO_0000087 has role} looks like on any real compound.</p>
 */
class OntologyTermRestrictionsTest {

    private static final String OBO = "http://purl.obolibrary.org/obo/";
    private static final String IMATINIB = OBO + "CHEBI_45783";
    private static final String HAS_ROLE = OBO + "RO_0000087";
    private static final String ANTINEOPLASTIC = OBO + "CHEBI_35610";
    private static final String KINASE_INHIBITOR = OBO + "CHEBI_38637";
    private static final String ANTIVIRAL = OBO + "CHEBI_22587";

    private static String restriction( String filler ) {
        return "    <rdfs:subClassOf>\n"
                + "      <owl:Restriction>\n"
                + "        <owl:onProperty rdf:resource=\"" + HAS_ROLE + "\"/>\n"
                + "        <owl:someValuesFrom rdf:resource=\"" + filler + "\"/>\n"
                + "      </owl:Restriction>\n"
                + "    </rdfs:subClassOf>\n";
    }

    private static final String RDF = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n"
            + "  <owl:ObjectProperty rdf:about=\"" + HAS_ROLE + "\"/>\n"
            + "  <owl:Class rdf:about=\"" + ANTINEOPLASTIC + "\"><rdfs:label>antineoplastic agent</rdfs:label></owl:Class>\n"
            + "  <owl:Class rdf:about=\"" + KINASE_INHIBITOR + "\"><rdfs:label>tyrosine kinase inhibitor</rdfs:label></owl:Class>\n"
            + "  <owl:Class rdf:about=\"" + ANTIVIRAL + "\"><rdfs:label>antiviral agent</rdfs:label></owl:Class>\n"
            + "  <owl:Class rdf:about=\"" + IMATINIB + "\">\n"
            + "    <rdfs:label>imatinib</rdfs:label>\n"
            + restriction( ANTINEOPLASTIC )
            + restriction( KINASE_INHIBITOR )
            + restriction( ANTIVIRAL )
            + "  </owl:Class>\n"
            + "</rdf:RDF>\n";

    private OntologyTermImpl imatinib() {
        // OWL_MEM, no inference: the mode CHEBI actually loads under
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        model.read( new ByteArrayInputStream( RDF.getBytes( StandardCharsets.UTF_8 ) ), null );
        OntClass c = model.getOntClass( IMATINIB );
        assertThat( c ).isNotNull();
        return new OntologyTermImpl( c, null );
    }

    @Test
    void everyRestrictionOnAClassSurvivesTheRead() {
        Collection<OntologyRestriction> direct = imatinib().getDirectRestrictions();

        assertThat( direct )
                .as( "three roles were asserted; keeping one is the whole bug" )
                .hasSize( 3 );
        assertThat( direct )
                .extracting( r -> ( ( OntologyClassRestriction ) r ).getRestrictedTo().getUri() )
                .containsExactlyInAnyOrder( ANTINEOPLASTIC, KINASE_INHIBITOR, ANTIVIRAL );
    }

    /**
     * The closure-walking variant collects into the same set and had the same defect.
     */
    @Test
    void theClosureWalkKeepsThemToo() {
        assertThat( imatinib().getRestrictions() ).hasSize( 3 );
    }

    /**
     * 🛑 The contract underneath it: two restrictions differing in their filler are not the same
     * restriction, and a hash set can only be trusted to hold them apart if hashCode agrees with
     * equals. Asserted directly so a future change to AbstractOntologyResource cannot quietly
     * reintroduce the collapse through the base class.
     */
    @Test
    void twoRestrictionsWithDifferentFillersAreNotEqual() {
        Collection<OntologyRestriction> rs = imatinib().getDirectRestrictions();
        OntologyRestriction[] arr = rs.toArray( new OntologyRestriction[0] );
        assertThat( arr[0] ).isNotEqualTo( arr[1] );
        assertThat( arr[0].hashCode() )
                .as( "equal-vs-hash contract: distinct restrictions must not all share one bucket" )
                .isNotEqualTo( arr[1].hashCode() );
    }
}

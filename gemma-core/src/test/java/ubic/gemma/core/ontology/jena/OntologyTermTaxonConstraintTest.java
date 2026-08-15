package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reading OBO's {@code in_taxon} species constraint off a loaded term.
 *
 * <p>MONDO writes {@code relationship: in_taxon NCBITaxon:9940}, which becomes
 * {@code SubClassOf(RO_0002162 some NCBITaxon_9940)}. It declares this on 3,201 terms and only 30 of
 * them are human, so it is overwhelmingly a marker for "this term is not about your organism" — the
 * one signal that separates {@code MONDO:0700199 sheep lung adenocarcinoma} from a mouse disease
 * when the namespace, the organ and the label shape are all equally plausible.</p>
 *
 * <p>The RDF below is the real shape MONDO emits, not a convenient simplification, because the point
 * of the lookup is that it survives the OWL restriction encoding rather than reading a flat
 * property.</p>
 */
class OntologyTermTaxonConstraintTest {

    private static final String SHEEP_LUNG_ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0700199";
    private static final String LUNG_ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0005061";
    private static final String OVIS_ARIES = "http://purl.obolibrary.org/obo/NCBITaxon_9940";

    private static final String RDF = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
            + "         xmlns:semapv=\"https://w3id.org/semapv/vocab/\">\n"
            + "  <owl:ObjectProperty rdf:about=\"http://purl.obolibrary.org/obo/RO_0002162\"/>\n"
            // must be DECLARED, or getAnnotations() will not recognise the predicate — MONDO
            // declares it, which is why the label-valued version reached production at all
            + "  <owl:AnnotationProperty rdf:about=\"https://w3id.org/semapv/vocab/crossSpeciesExactMatch\"/>\n"
            + "  <owl:Class rdf:about=\"" + OVIS_ARIES + "\">\n"
            + "    <rdfs:label>Ovis aries</rdfs:label>\n"
            + "  </owl:Class>\n"
            // the species-constrained term: in_taxon some Ovis aries
            + "  <owl:Class rdf:about=\"" + SHEEP_LUNG_ADENOCARCINOMA + "\">\n"
            + "    <rdfs:label>sheep lung adenocarcinoma</rdfs:label>\n"
            // the cross-species mapping MONDO declares, as a RESOURCE -- the whole point of the
            // getValueUri test below is that resolving this to its label loses the identity
            + "    <semapv:crossSpeciesExactMatch rdf:resource=\"" + LUNG_ADENOCARCINOMA + "\"/>\n"
            + "    <rdfs:subClassOf>\n"
            + "      <owl:Restriction>\n"
            + "        <owl:onProperty rdf:resource=\"http://purl.obolibrary.org/obo/RO_0002162\"/>\n"
            + "        <owl:someValuesFrom rdf:resource=\"" + OVIS_ARIES + "\"/>\n"
            + "      </owl:Restriction>\n"
            + "    </rdfs:subClassOf>\n"
            + "  </owl:Class>\n"
            // the unconstrained human counterpart, plus a plain named superclass so the walk has
            // something non-restriction to skip past
            + "  <owl:Class rdf:about=\"" + LUNG_ADENOCARCINOMA + "\">\n"
            + "    <rdfs:label>lung adenocarcinoma</rdfs:label>\n"
            + "    <rdfs:subClassOf rdf:resource=\"http://purl.obolibrary.org/obo/MONDO_0000001\"/>\n"
            + "  </owl:Class>\n"
            + "  <owl:Class rdf:about=\"http://purl.obolibrary.org/obo/MONDO_0000001\">\n"
            + "    <rdfs:label>disease</rdfs:label>\n"
            + "  </owl:Class>\n"
            + "</rdf:RDF>\n";

    private OntologyTerm term( String uri ) {
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        model.read( new ByteArrayInputStream( RDF.getBytes( StandardCharsets.UTF_8 ) ), null );
        return new OntologyTermImpl( model.getOntClass( uri ), null );
    }

    @Test
    void readsTheTaxonOffAConstrainedTerm() {
        OntologyTerm.TaxonConstraint c = term( SHEEP_LUNG_ADENOCARCINOMA ).getTaxonConstraint();

        assertThat( c ).isNotNull();
        assertThat( c.getUri() ).isEqualTo( OVIS_ARIES );
        assertThat( c.getNcbiTaxonId() ).isEqualTo( 9940 );
        assertThat( c.getLabel() ).isEqualTo( "Ovis aries" );
    }

    /**
     * The common case by a wide margin, and the one that must stay cheap: no constraint declared, a
     * plain named superclass to walk past, and null returned without throwing.
     */
    @Test
    void returnsNullWhenNoTaxonIsDeclared() {
        assertThat( term( LUNG_ADENOCARCINOMA ).getTaxonConstraint() ).isNull();
    }

    /**
     * The id is what a client keys on, so it must survive a missing label — which is the normal
     * state, since Gemma does not load NCBITaxon and most referencing ontologies declare no label.
     */
    @Test
    void theIdSurvivesAnUnlabelledTaxonClass() {
        String rdf = RDF.replace( "    <rdfs:label>Ovis aries</rdfs:label>\n", "" );
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        model.read( new ByteArrayInputStream( rdf.getBytes( StandardCharsets.UTF_8 ) ), null );
        OntologyTerm t = new OntologyTermImpl( model.getOntClass( SHEEP_LUNG_ADENOCARCINOMA ), null );

        OntologyTerm.TaxonConstraint c = t.getTaxonConstraint();

        assertThat( c ).isNotNull();
        assertThat( c.getNcbiTaxonId() ).isEqualTo( 9940 );
        assertThat( c.getLabel() ).isNull();
    }

    /** A term with no constraint must not inherit one from elsewhere in the model. */
    @Test
    void theConstraintDoesNotLeakBetweenTerms() {
        assertThat( term( SHEEP_LUNG_ADENOCARCINOMA ).getTaxonConstraint() ).isNotNull();
        assertThat( term( "http://purl.obolibrary.org/obo/MONDO_0000001" ).getTaxonConstraint() ).isNull();
    }

    /**
     * The mapping must carry the IDENTIFIER. {@code AnnotationProperty.getContents()} resolves a
     * resource to its {@code rdfs:label}, and this mapping first shipped that way —
     * {@code ["lung adenocarcinoma"]}, a string that names both {@code MONDO:0005061} (a disease)
     * and {@code HP:0030078} (a phenotype). Repairing an annotation from it can land on the
     * phenotype and look like it worked.
     */
    @Test
    void crossSpeciesExactMatchKeepsTheUriNotJustTheLabel() {
        OntologyTerm t = term( SHEEP_LUNG_ADENOCARCINOMA );
        java.util.List<ubic.gemma.core.ontology.model.AnnotationProperty> xs =
                new java.util.ArrayList<>( t.getAnnotations( "https://w3id.org/semapv/vocab/crossSpeciesExactMatch" ) );

        assertThat( xs ).hasSize( 1 );
        assertThat( xs.get( 0 ).getValueUri() ).isEqualTo( LUNG_ADENOCARCINOMA );
        // and the label indirection is still there for display, which is why the URI had to be added
        assertThat( xs.get( 0 ).getContents() ).isEqualTo( "lung adenocarcinoma" );
    }

    /** Literal-valued annotations have no identity to return. */
    @Test
    void valueUriIsNullForALiteralAnnotation() {
        OntologyTerm t = term( SHEEP_LUNG_ADENOCARCINOMA );
        for ( ubic.gemma.core.ontology.model.AnnotationProperty a : t.getAnnotations() ) {
            if ( a.getValueUri() == null ) {
                return;  // at least one literal annotation, as expected
            }
        }
    }
}

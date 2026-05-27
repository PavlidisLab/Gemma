package ubic.gemma.core.ontology.providers;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OntologySlimExtractorTest {

    /**
     * Regression: the extractor shipped without {@code @Component} on 2026-05-26.
     * OntologyConfig autowires it as required=false; missing annotation meant
     * slimExtractor stayed null in production and the slim path was silently disabled,
     * so first-boot CHEBI loaded the full source on every restart. Pin the stereotype.
     */
    @Test
    void hasSpringComponentStereotype() {
        assertTrue( OntologySlimExtractor.class.isAnnotationPresent( Component.class ),
                "OntologySlimExtractor must be @Component so OntologyConfig.chebiOntologyService / "
                        + "mondoOntologyService can autowire it; without it the slim path is silently disabled." );
    }

    private static final String SORAFENIB = "http://purl.obolibrary.org/obo/CHEBI_50924";
    private static final String ESTRADIOL = "http://purl.obolibrary.org/obo/CHEBI_23965";
    private static final String WATER = "http://purl.obolibrary.org/obo/CHEBI_15377";
    private static final String CHEMICAL_ENTITY = "http://purl.obolibrary.org/obo/CHEBI_24431";
    private static final String ROLE = "http://purl.obolibrary.org/obo/CHEBI_50906";
    private static final String DRUG = "http://purl.obolibrary.org/obo/CHEBI_50300";
    private static final String KINASE_INHIBITOR = "http://purl.obolibrary.org/obo/CHEBI_35222";
    private static final String HORMONE = "http://purl.obolibrary.org/obo/CHEBI_61120";

    @Test
    void seedSorafenibPullsAncestorsAndRoleClosure( @TempDir Path tempDir ) throws Exception {
        File source = copyFixture( tempDir, "chebi-mini.test.owl.xml" );
        File slim = tempDir.resolve( "slim.owl" ).toFile();

        OntologySlimExtractor.ExtractResult result = new OntologySlimExtractor()
                .extract( source, List.of( SORAFENIB ), slim );

        assertEquals( 1, result.getCoveredSeedUris().size() );
        assertEquals( 0, result.getMissingSeedCount() );
        assertTrue( slim.isFile(), "slim file must exist" );
        assertTrue( slim.length() > 0, "slim file must not be empty" );

        Set<String> retainedClasses = loadClassUris( slim );

        // Seed itself
        assertTrue( retainedClasses.contains( SORAFENIB ), "seed retained: sorafenib" );
        // subClassOf ancestor
        assertTrue( retainedClasses.contains( CHEMICAL_ENTITY ),
                "subClassOf ancestor retained: chemical entity" );
        // has_role target (locality-preserving extraction follows the restriction)
        assertTrue( retainedClasses.contains( KINASE_INHIBITOR ),
                "has_role target retained: kinase inhibitor" );
        // Ancestor of has_role target
        assertTrue( retainedClasses.contains( DRUG ), "ancestor of has_role retained: drug" );
        assertTrue( retainedClasses.contains( ROLE ), "ancestor chain retained: role" );

        // Unrelated branch — STAR module extraction should NOT pull in estradiol/hormone/water
        assertTrue( !retainedClasses.contains( ESTRADIOL ), "unrelated chemical excluded: estradiol" );
        assertTrue( !retainedClasses.contains( HORMONE ), "unrelated role excluded: hormone" );
        assertTrue( !retainedClasses.contains( WATER ), "unrelated chemical excluded: water" );
    }

    @Test
    void missingSeedsAreCountedNotFatal( @TempDir Path tempDir ) throws Exception {
        File source = copyFixture( tempDir, "chebi-mini.test.owl.xml" );
        File slim = tempDir.resolve( "slim.owl" ).toFile();

        OntologySlimExtractor.ExtractResult result = new OntologySlimExtractor()
                .extract( source,
                        List.of( SORAFENIB, "http://purl.obolibrary.org/obo/CHEBI_99999999" ),
                        slim );

        assertEquals( 1, result.getCoveredSeedUris().size() );
        assertEquals( 1, result.getMissingSeedCount() );
        assertTrue( slim.isFile() );
    }

    @Test
    void slimRoundTripsThroughJena( @TempDir Path tempDir ) throws Exception {
        File source = copyFixture( tempDir, "chebi-mini.test.owl.xml" );
        File slim = tempDir.resolve( "slim.owl" ).toFile();

        new OntologySlimExtractor().extract( source, List.of( SORAFENIB ), slim );

        // Load the extractor's output via Jena — this is the runtime read path that
        // ChebiOntologyService.loadModel will use. Confirms the slim is consumable
        // by the downstream Jena ontology service without further translation.
        OntModel jenaModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        try ( FileInputStream in = new FileInputStream( slim ) ) {
            jenaModel.read( in, null );
        }

        Set<String> classUris = new HashSet<>();
        ExtendedIterator<OntClass> it = jenaModel.listClasses();
        while ( it.hasNext() ) {
            OntClass c = it.next();
            if ( c.getURI() != null ) {
                classUris.add( c.getURI() );
            }
        }
        it.close();

        assertTrue( classUris.contains( SORAFENIB ), "Jena reads sorafenib from slim" );
        assertTrue( classUris.contains( KINASE_INHIBITOR ),
                "Jena reads kinase inhibitor from slim" );
        assertTrue( classUris.contains( DRUG ), "Jena reads drug ancestor from slim" );

        // Spot-check that rdfs:label survived the round trip
        OntClass sorafenib = jenaModel.getOntClass( SORAFENIB );
        assertNotNull( sorafenib, "sorafenib OntClass present" );
        assertEquals( "sorafenib", sorafenib.getLabel( null ),
                "rdfs:label preserved by STAR + Jena round trip" );
    }

    @Test
    void seedOfBothChemicalsPullsBothRoleSubtrees( @TempDir Path tempDir ) throws Exception {
        File source = copyFixture( tempDir, "chebi-mini.test.owl.xml" );
        File slim = tempDir.resolve( "slim.owl" ).toFile();

        new OntologySlimExtractor().extract( source, List.of( SORAFENIB, ESTRADIOL ), slim );

        Set<String> retainedClasses = loadClassUris( slim );
        assertTrue( retainedClasses.contains( SORAFENIB ) );
        assertTrue( retainedClasses.contains( ESTRADIOL ) );
        assertTrue( retainedClasses.contains( KINASE_INHIBITOR ) );
        assertTrue( retainedClasses.contains( HORMONE ) );
        assertTrue( retainedClasses.contains( ROLE ) );
        // water is unrelated to any role
        assertTrue( !retainedClasses.contains( WATER ) );
    }

    private File copyFixture( Path tempDir, String name ) throws IOException {
        File out = tempDir.resolve( name ).toFile();
        try ( InputStream in = getClass().getResourceAsStream(
                "/data/loader/ontology/" + name ) ) {
            assertNotNull( in, "fixture not found on classpath: " + name );
            Files.copy( in, out.toPath() );
        }
        return out;
    }

    private Set<String> loadClassUris( File owl ) throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ont = manager.loadOntologyFromOntologyDocument( owl );
        return ont.getClassesInSignature( Imports.INCLUDED ).stream()
                .map( OWLClass::getIRI )
                .map( IRI::toString )
                .collect( Collectors.toSet() );
    }
}

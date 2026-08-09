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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /**
     * The slim must carry the names a term is SEARCHED by, not just its preferred label.
     *
     * <p>Regression for the 2026-08-09 CHEBI outage: every CHEBI term was reachable by its
     * preferred label and by no other name, so {@code acetylsalicylic acid} resolved and
     * {@code aspirin} — a synonym — found nothing, as did every drug abbreviation. The immediate
     * cause was the source file, but a module extractor that drops annotation assertions would
     * reintroduce it silently the moment the slim became the serving path, and the failure looks
     * like a ranking problem rather than a missing-data one.
     */
    @Test
    void slimCarriesSynonymsNotJustPreferredLabels( @TempDir Path tempDir ) throws Exception {
        File source = copyFixture( tempDir, "chebi-mini.test.owl.xml" );
        File slim = tempDir.resolve( "slim.owl" ).toFile();

        new OntologySlimExtractor().extract( source, List.of( SORAFENIB ), slim );

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology extracted = manager.loadOntologyFromOntologyDocument( slim );
        OWLClass sorafenib = manager.getOWLDataFactory().getOWLClass( IRI.create( SORAFENIB ) );

        Set<String> annotationValues = extracted.getAnnotationAssertionAxioms( sorafenib.getIRI() ).stream()
                .map( ax -> ax.getValue().asLiteral().isPresent()
                        ? ax.getValue().asLiteral().get().getLiteral()
                        : ax.getValue().toString() )
                .collect( Collectors.toSet() );

        assertTrue( annotationValues.contains( "sorafenib" ),
                "preferred label retained; got " + annotationValues );
        assertTrue( annotationValues.contains( "Nexavar" ),
                "hasExactSynonym retained — searching the brand name must find the compound; got " + annotationValues );
    }

    /**
     * Seeding a role pulls in its bearers even when nothing in the corpus uses them.
     *
     * <p>A slim seeded only from corpus usage can return only what was already annotated, so it
     * cannot help a curator annotate a compound for the first time — the case that matters most in
     * a picker. Seeding CHEBI's {@code drug} role fixes that. Here water is the only corpus seed;
     * sorafenib must arrive purely because it bears {@code kinase inhibitor}, which is-a
     * {@code drug}.
     */
    @Test
    void seedingARolePullsInBearersThatAreNotCorpusSeeds( @TempDir Path tempDir ) throws Exception {
        File source = copyFixture( tempDir, "chebi-mini.test.owl.xml" );
        File slim = tempDir.resolve( "slim.owl" ).toFile();

        new OntologySlimExtractor().extract( source, List.of( WATER ), List.of( DRUG ), slim );

        Set<String> classUris = classUrisOf( slim );
        assertTrue( classUris.contains( SORAFENIB ),
                "sorafenib bears kinase inhibitor, which is-a drug, so the drug role must reach it; got " + classUris );
        assertTrue( classUris.contains( WATER ), "the corpus seed itself is still present" );
        // The closure descends the role hierarchy but must not escape it: estradiol bears hormone,
        // which is a sibling role under `role`, NOT a drug. Seeding `drug` that dragged in every
        // role bearer would defeat the point of a slim.
        assertFalse( classUris.contains( ESTRADIOL ),
                "estradiol bears hormone, which is not under drug, so it must NOT be pulled in; got " + classUris );
    }

    /** Without a role seed, the same call keeps today's corpus-only behaviour. */
    @Test
    void withoutARoleSeedOnlyTheCorpusSeedsArrive( @TempDir Path tempDir ) throws Exception {
        File source = copyFixture( tempDir, "chebi-mini.test.owl.xml" );
        File slim = tempDir.resolve( "slim.owl" ).toFile();

        new OntologySlimExtractor().extract( source, List.of( WATER ), slim );

        Set<String> classUris = classUrisOf( slim );
        assertTrue( classUris.contains( WATER ) );
        assertFalse( classUris.contains( SORAFENIB ),
                "no role seed means no pharmacopoeia; got " + classUris );
    }

    private static Set<String> classUrisOf( File slim ) throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology extracted = manager.loadOntologyFromOntologyDocument( slim );
        return extracted.getClassesInSignature( Imports.INCLUDED ).stream()
                .map( c -> c.getIRI().toString() )
                .collect( Collectors.toSet() );
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

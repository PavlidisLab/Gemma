package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.config.Configuration;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.chebi.ChebiSlimExtractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the slim-cache-hit path on {@link ChebiOntologyService}: writes a pre-extracted
 * slim under a temp cache dir, configures the service to look there, and confirms initialize()
 * loads from disk without touching the configured (intentionally invalid) URL. Phase 4b
 * regression guard; richer end-to-end (slim-build-on-miss) integration test is deferred to
 * Phase 4c where the seed resolver gets a gemdtest backing.
 */
class ChebiOntologyServiceSlimTest {

    private static final String CHEBI_FIXTURE = "/data/loader/ontology/chebi-mini.test.owl.xml";
    private static final String SORAFENIB = "http://purl.obolibrary.org/obo/CHEBI_50924";
    private static final String KINASE_INHIBITOR = "http://purl.obolibrary.org/obo/CHEBI_35222";

    @BeforeEach
    void setupConfiguration() {
        // The no-arg constructor requires url.chebiOntology and reads load.chebiOntology.
        // Point at an unreachable host: if the slim path works the URL is never touched.
        Configuration.setString( "url.chebiOntology", "http://chebi.test.invalid/chebi.owl" );
        Configuration.setString( "load.chebiOntology", "true" );
    }

    @AfterEach
    void cleanup() {
        Configuration.reset( "url.chebiOntology" );
        Configuration.reset( "load.chebiOntology" );
    }

    @Test
    void initializeLoadsFromSlimWithoutTouchingUrl( @TempDir Path tempDir ) throws Exception {
        // Stage a freshly-extracted slim at the cache location ChebiOntologyService expects.
        File slimFile = stageSlimFromFixture( tempDir );
        assertTrue( slimFile.isFile(), "slim file must be on disk before service inits" );

        ChebiOntologyService service = new ChebiOntologyService();
        service.setSlimCacheDir( tempDir.toFile() );
        // slimExtractor + seedResolver intentionally null — slim is already on disk, no
        // rebuild required. The override should consume the slim and skip the URL fetch.

        try {
            service.initialize( false, false );
            assertTrue( service.isOntologyLoaded(),
                    "ontology must report loaded after slim consumption" );

            OntologyTerm kinase = service.getTerm( KINASE_INHIBITOR );
            assertNotNull( kinase, "kinase inhibitor reachable via slim" );
            OntologyTerm sorafenib = service.getTerm( SORAFENIB );
            assertNotNull( sorafenib, "sorafenib reachable via slim" );

            // Reverse-traverse the has_role axiom: kinase inhibitor's children with
            // includeAdditionalProperties should contain sorafenib.
            Set<String> kinaseChildrenUris = service.getChildren( List.of( kinase ), false, true ).stream()
                    .map( OntologyTerm::getUri )
                    .collect( Collectors.toSet() );
            assertTrue( kinaseChildrenUris.contains( SORAFENIB ),
                    "has_role traversal works on slim: kinase inhibitor -> sorafenib" );
        } finally {
            service.close();
        }
    }

    /**
     * Run the extractor on the mini fixture and place the resulting slim at the path
     * {@code ChebiOntologyService} will look at ({@code <slimCacheDir>/chebiOntology-slim.owl}).
     */
    private File stageSlimFromFixture( Path tempDir ) throws Exception {
        Path source = tempDir.resolve( "chebi-source.owl" );
        try ( InputStream in = getClass().getResourceAsStream( CHEBI_FIXTURE ) ) {
            assertNotNull( in, "fixture not on classpath: " + CHEBI_FIXTURE );
            Files.copy( in, source );
        }
        File slim = tempDir.resolve( "chebiOntology-slim.owl" ).toFile();
        new ChebiSlimExtractor().extract( source.toFile(), List.of( SORAFENIB ), slim );
        return slim;
    }
}

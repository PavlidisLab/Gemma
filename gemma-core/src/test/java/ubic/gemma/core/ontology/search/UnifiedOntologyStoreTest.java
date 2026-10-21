package ubic.gemma.core.ontology.search;

import org.apache.jena.tdb.base.file.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.ontology.jena.TdbOntologyService;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The unified ontology store as {@code gemma-cli updateUnifiedOntology} builds it, opened by both of its readers.
 * <p>
 * The search service used to open the store with TDB2 while the CLI wrote it and the loader read it with TDB1. Every
 * store failed with {@code StoreParams key not recognized: tdb.index_node2id}, and because the search service is a
 * singleton bean, the whole Spring context failed with it. Nothing caught this: no test opened a store the CLI had
 * built.
 */
class UnifiedOntologyStoreTest {

    private static final String HIPPOCAMPUS = "http://purl.obolibrary.org/obo/UBERON_0001954";

    private static final String OWL = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
            + "         xmlns:oboInOwl=\"http://www.geneontology.org/formats/oboInOwl#\">\n"
            + "  <owl:Ontology rdf:about=\"http://example.org/unified-fixture\"/>\n"
            + "  <owl:Class rdf:about=\"" + HIPPOCAMPUS + "\">\n"
            + "    <rdfs:label>Ammon's horn</rdfs:label>\n"
            + "    <oboInOwl:hasExactSynonym>hippocampus proper</oboInOwl:hasExactSynonym>\n"
            + "  </owl:Class>\n"
            + "  <owl:Class rdf:about=\"http://purl.obolibrary.org/obo/UBERON_0000955\">\n"
            + "    <rdfs:label>brain</rdfs:label>\n"
            + "  </owl:Class>\n"
            + "</rdf:RDF>\n";

    @Test
    void storeBuiltLikeTheCliIsReadByTheLoaderAndTheSearchService( @TempDir Path dir ) throws Exception {
        Path tdbDir = buildStore( dir );

        try ( TdbOntologyService loader = new TdbOntologyService( "Test", tdbDir, null, true, null, true ) ) {
            loader.initialize( true, false );
            OntologyTerm term = loader.getTerm( HIPPOCAMPUS );
            assertThat( term ).isNotNull();
            assertThat( term.getLabel() ).isEqualTo( "Ammon's horn" );
        }

        JenaTextOntologySearchService search = new JenaTextOntologySearchService( tdbDir, true );
        try {
            assertThat( search.isReady() ).isTrue();
            // The URI alone comes from the Lucene index and is found even over an empty store. The label is read
            // from the TDB, so it is what shows the search service opened the store the CLI wrote rather than a new
            // one beside it: TDB2 does exactly that, creating Data-0001/ over the TDB1 files.
            assertThat( search.search( "horn", 10 ) )
                    .extracting( OntologySearchService.Result::getUri, OntologySearchService.Result::getLabel )
                    .containsExactly( tuple( HIPPOCAMPUS, "Ammon's horn" ) );
            assertThat( tdbDir.resolve( "Data-0001" ) ).doesNotExist();
        } finally {
            search.close();
        }
    }

    /**
     * Without {@link JenaTextOntologySearchService#buildIndex(Path)} the store opens and every search comes back
     * empty, since jena-text indexes only what is written through the text dataset.
     */
    @Test
    void storeWithoutABuiltIndexFindsNothing( @TempDir Path dir ) throws Exception {
        Path sources = Files.write( dir.resolve( "fixture.owl" ), OWL.getBytes( StandardCharsets.UTF_8 ) );
        Path tdbDir = dir.resolve( "tdb" );
        TdbOntologyService.createDataset( tdbDir, Collections.singletonList( sources ) );

        JenaTextOntologySearchService search = new JenaTextOntologySearchService( tdbDir, true );
        try {
            assertThat( search.search( "horn", 10 ) ).isEmpty();
        } finally {
            search.close();
        }
    }

    /**
     * In Tomcat both readers are open at once. The loader takes the TDB lock to copy the store, and releasing it
     * deleted the lock file the search service's dataset was holding.
     */
    @Test
    void loaderLeavesTheSearchServiceLockInPlace( @TempDir Path dir ) throws Exception {
        Path tdbDir = buildStore( dir );
        JenaTextOntologySearchService search = new JenaTextOntologySearchService( tdbDir, true );
        try {
            assertThat( Location.create( tdbDir.toString() ).getLock().isOwned() ).isTrue();
            try ( TdbOntologyService loader = new TdbOntologyService( "Test", tdbDir, null, true, null, true ) ) {
                loader.initialize( true, false );
            }
            assertThat( Location.create( tdbDir.toString() ).getLock().isOwned() ).isTrue();
            assertThat( search.search( "horn", 10 ) ).hasSize( 1 );
        } finally {
            search.close();
        }
    }

    /**
     * {@code gemma.ontology.unified.tdb.tempDir} outlives the JVM. Copying the store into it again on the next boot
     * failed with {@code FileAlreadyExistsException} on the first file, as it did on dev.
     */
    @Test
    void readOnlyCopyIgnoresALeftoverTempDir( @TempDir Path dir ) throws Exception {
        Path tdbDir = buildStore( dir );
        Path tempDir = Files.createDirectory( dir.resolve( "tdbReadOnly" ) );
        Path leftover = Files.write( tempDir.resolve( "prefix2id.dat" ), new byte[] { 1 } );

        try ( TdbOntologyService loader = new TdbOntologyService( "Test", tdbDir, null, true, null, true ) ) {
            loader.setTempDir( tempDir );
            loader.initialize( true, false );
            assertThat( loader.getTerm( HIPPOCAMPUS ) ).isNotNull();
        }

        // only the copy this service made is removed
        assertThat( listDir( tempDir ) ).containsExactly( leftover );
    }

    /**
     * An unreadable store disables search; it must not throw from the constructor, which fails the Spring context and
     * turns every endpoint into a 404.
     */
    @Test
    void storeThatCannotBeOpenedDisablesSearch( @TempDir Path dir ) throws Exception {
        Path notADirectory = Files.write( dir.resolve( "tdb" ), new byte[] { 1 } );

        JenaTextOntologySearchService search = new JenaTextOntologySearchService( notADirectory, true );
        assertThat( search.isReady() ).isFalse();
        assertThat( search.search( "horn", 10 ) ).isEmpty();
        search.close();
    }

    private static Path buildStore( Path dir ) throws Exception {
        Path sources = Files.write( dir.resolve( "fixture.owl" ), OWL.getBytes( StandardCharsets.UTF_8 ) );
        Path tdbDir = dir.resolve( "tdb" );
        TdbOntologyService.createDataset( tdbDir, Collections.singletonList( sources ) );
        assertThat( JenaTextOntologySearchService.buildIndex( tdbDir ) ).isEqualTo( 3 );
        return tdbDir;
    }

    private static List<Path> listDir( Path dir ) throws Exception {
        try ( Stream<Path> s = Files.list( dir ) ) {
            return s.collect( Collectors.toList() );
        }
    }
}

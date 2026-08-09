package ubic.gemma.core.ontology.jena;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the source marker that ties an ontology's on-disk Lucene index to the URL it was
 * built from.
 *
 * <p>Regression for the 2026-08-09 CHEBI outage: the index is keyed by cache name, not by source,
 * so re-pointing {@code url.chebiOntology} from {@code chebi_lite.owl} to {@code chebi.owl} left
 * the synonym-less index in place and looking valid. {@code /annotations/term} showed a term's
 * synonyms while {@code /annotations/search} could not find it by any of them — across restarts,
 * because a restart re-evaluates the same condition and skips indexing again.
 */
class OntologyLoaderSourceMarkerTest {

    private static final String CACHE_NAME = "sourceMarkerTestOntology";
    private static final String LITE = "http://purl.obolibrary.org/obo/chebi/chebi_lite.owl";
    private static final String FULL = "http://purl.obolibrary.org/obo/chebi/chebi.owl";

    @AfterEach
    void cleanUp() {
        File marker = OntologyLoader.getSourceMarkerPath( CACHE_NAME );
        //noinspection ResultOfMethodCallIgnored
        marker.delete();
    }

    @Test
    void aDifferentSourceIsReportedAsChanged() {
        OntologyLoader.recordSource( CACHE_NAME, LITE );
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, FULL ) )
                .withFailMessage( "re-pointing the URL must force a reindex; this is the CHEBI outage" )
                .isTrue();
    }

    @Test
    void theSameSourceIsNotReportedAsChanged() {
        OntologyLoader.recordSource( CACHE_NAME, FULL );
        // Every boot hits this path. Reindexing 237k terms on each restart is exactly the cost the
        // "indexing is slow, don't do it if we don't have to" guard exists to avoid.
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, FULL ) ).isFalse();
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, "  " + FULL + "  " ) )
                .withFailMessage( "surrounding whitespace is not a source change" )
                .isFalse();
    }

    @Test
    void anAbsentMarkerIsNotReportedAsChanged() {
        // Deliberate: every deployment predating the marker lacks one, and treating absence as a
        // mismatch would reindex every ontology on the next boot to answer a question we cannot
        // actually answer for those indexes. Protection starts one load later.
        assertThat( OntologyLoader.getSourceMarkerPath( CACHE_NAME ) ).doesNotExist();
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, FULL ) ).isFalse();
    }

    @Test
    void blankInputsDisableTheCheck() {
        assertThat( OntologyLoader.hasSourceChanged( "", FULL ) ).isFalse();
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, null ) ).isFalse();
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, "" ) ).isFalse();
    }

    @Test
    void recordingOverwritesTheEarlierSource() throws Exception {
        OntologyLoader.recordSource( CACHE_NAME, LITE );
        OntologyLoader.recordSource( CACHE_NAME, FULL );
        assertThat( new String( Files.readAllBytes( OntologyLoader.getSourceMarkerPath( CACHE_NAME ).toPath() ),
                StandardCharsets.UTF_8 ) ).isEqualTo( FULL );
        // ...so the reindex fires once per change, not on every subsequent load.
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, FULL ) ).isFalse();
    }

    @Test
    void anEmptyMarkerIsTreatedAsUnknownRatherThanAsAChange() throws Exception {
        File marker = OntologyLoader.getSourceMarkerPath( CACHE_NAME );
        Files.createDirectories( marker.getParentFile().toPath() );
        Files.write( marker.toPath(), new byte[0] );
        assertThat( OntologyLoader.hasSourceChanged( CACHE_NAME, FULL ) ).isFalse();
    }
}

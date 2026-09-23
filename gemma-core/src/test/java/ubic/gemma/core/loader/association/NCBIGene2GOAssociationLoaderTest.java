package ubic.gemma.core.loader.association;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.RelationshipPersister;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * The parser and loader threads and how the load ends. The fixture holds 61 human associations.
 * <p>
 * A queue size of 1 makes the parser block after the first association, which is how a failure more than 60,000
 * associations before the end of a real file used to hang the run forever.
 */
class NCBIGene2GOAssociationLoaderTest {

    private static final String GENE2GO = "/data/loader/association/gene2go.gz";

    private final RelationshipPersister relationshipPersister = mock();
    private NCBIGene2GOAssociationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new NCBIGene2GOAssociationLoader();
        loader.setRelationshipPersister( relationshipPersister );
        loader.setParser( new NCBIGene2GOAssociationParser(
                Collections.singleton( Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true ) ) ) );
    }

    @Test
    void aPersistFailureEndsTheLoad() {
        loader.setQueueSize( 1 );
        loader.setBatchSize( 1 );
        when( relationshipPersister.persistGene2GOAssociations( anyCollection() ) )
                .thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> load( fixture() ) )
                        .hasMessageContaining( "Failed to persist GO associations" )
                        .hasRootCauseMessage( "database went away" ) );
    }

    /**
     * When the queue could hold what was left, the parser finished, the failed loader had set its done flag, and the
     * load returned as though it had succeeded.
     */
    @Test
    void aPersistFailureNearTheEndIsNotASuccess() {
        loader.setQueueSize( 100 );
        loader.setBatchSize( 10 );
        when( relationshipPersister.persistGene2GOAssociations( anyCollection() ) )
                .thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> load( fixture() ) )
                        .hasRootCauseMessage( "database went away" ) );
    }

    /**
     * The final, partial batch is persisted after the loop, where a failure used to leave the done flag unset.
     */
    @Test
    void aPersistFailureInTheFinalBatchEndsTheLoad() {
        when( relationshipPersister.persistGene2GOAssociations( anyCollection() ) )
                .thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> load( fixture() ) )
                        .hasRootCauseMessage( "database went away" ) );
    }

    @Test
    void anErrorInTheLoaderEndsTheLoad() {
        loader.setQueueSize( 1 );
        loader.setBatchSize( 1 );
        when( relationshipPersister.persistGene2GOAssociations( anyCollection() ) )
                .thenThrow( new AssertionError( "broken invariant" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> load( fixture() ) )
                        .hasRootCauseInstanceOf( AssertionError.class ) );
    }

    @Test
    void aParserFailureEndsTheLoad() {
        byte[] badLine = "9606\tnot-a-gene-id\tGO:0005794\tIDA\t-\tGolgi apparatus\t10900002\tComponent\n"
                .getBytes( StandardCharsets.UTF_8 );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> load( new ByteArrayInputStream( badLine ) ) )
                        .hasMessageContaining( "Failed to parse gene2go" )
                        .hasRootCauseInstanceOf( NumberFormatException.class ) );
    }

    @Test
    void aCompleteLoadPersistsEveryAssociation() {
        loader.setQueueSize( 1 );
        loader.setBatchSize( 10 );
        when( relationshipPersister.persistGene2GOAssociations( anyCollection() ) ).thenAnswer( a -> a.getArgument( 0 ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () -> load( fixture() ) );

        assertThat( loader.getCount() ).isEqualTo( 61 );
        // six full batches and the remainder
        verify( relationshipPersister, times( 7 ) ).persistGene2GOAssociations( any() );
    }

    private InputStream fixture() throws Exception {
        return new GZIPInputStream( new ClassPathResource( GENE2GO ).getInputStream() );
    }

    private void load( InputStream in ) throws Exception {
        try ( InputStream is = in ) {
            loader.load( is );
        }
    }
}

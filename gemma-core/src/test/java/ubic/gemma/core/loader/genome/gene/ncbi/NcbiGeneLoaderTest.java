package ubic.gemma.core.loader.genome.gene.ncbi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.FileTools;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.gene.GeneProductChange;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The three loader threads and how the run ends. The fixture holds four human genes with products.
 * <p>
 * A queue size of 1 makes the producers block after the first gene, which is how a failure 1,000 genes before the end
 * of a real file used to hang the run forever.
 */
class NcbiGeneLoaderTest {

    private static final String GENE_INFO = "/data/loader/genome/gene/gene_info.human.sample";
    private static final String GENE2ACCESSION = "/data/loader/genome/gene/gene2accession.human.sample";
    private static final String GENE_HISTORY = "/data/loader/genome/gene/gene_history.human.sample";

    private final GeneWriteService geneWriteService = mock();
    private final TaxonService taxonService = mock();
    private final Taxon human = Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true );
    private NcbiGeneLoader loader;

    @BeforeEach
    void setUp() {
        loader = new NcbiGeneLoader();
        loader.setGeneWriteService( geneWriteService );
        loader.setTaxonService( taxonService );
        loader.setQueueSize( 1 );
    }

    @Test
    void aFailedUpsertEndsTheRunWithTheGeneToRestartFrom() {
        when( geneWriteService.upsert( any(), anyBoolean(), any() ) ).thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::load )
                        .hasMessageContaining( "resume with -restart" )
                        .hasRootCauseMessage( "database went away" ) );
        verify( geneWriteService, times( 1 ) ).upsert( any(), anyBoolean(), any() );
        verifyNoInteractions( taxonService );
    }

    /**
     * The loader caught {@link Exception}, so an {@link Error} ended its thread with nothing recorded and the main
     * thread waited forever.
     */
    @Test
    void anErrorInTheLoaderEndsTheRun() {
        when( geneWriteService.upsert( any(), anyBoolean(), any() ) ).thenThrow( new AssertionError( "broken invariant" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::load )
                        .hasMessageContaining( "resume with -restart" )
                        .hasRootCauseInstanceOf( AssertionError.class ) );
        verifyNoInteractions( taxonService );
    }

    /**
     * Same for the converter thread: an {@link Error} must be recorded where the loader looks for failures.
     */
    @Test
    void anErrorInTheConverterIsRecorded() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean sourceDone = new AtomicBoolean( false );
        AtomicBoolean converterDone = new AtomicBoolean( false );
        NcbiGeneConverter converter = new NcbiGeneConverter() {
            @Override
            public Gene convert( NcbiGeneData data ) {
                throw new AssertionError( "broken invariant" );
            }
        };
        converter.setFailure( failure );
        converter.setSourceDoneFlag( sourceDone );
        converter.setProducerDoneFlag( converterDone );
        BlockingQueue<NcbiGeneData> in = new ArrayBlockingQueue<>( 1 );
        in.put( new NcbiGeneData() );
        sourceDone.set( true );

        converter.convert( in, new ArrayBlockingQueue<>( 1 ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 30 ), () -> {
            while ( failure.get() == null ) {
                Thread.sleep( 50 );
            }
        } );
        assertThat( failure.get() ).hasRootCauseInstanceOf( AssertionError.class );
        assertThat( converterDone ).isFalse();
    }

    @Test
    void aRestartIdThatIsNotInTheFileFails() {
        loader.setStartingNcbiId( 999999999 );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::load )
                        .rootCause()
                        .hasMessageContaining( "999999999 was not found in gene2accession" ) );
        verifyNoInteractions( geneWriteService );
    }

    @Test
    void theLimitStopsTheRunAfterThatManyGenes() {
        when( geneWriteService.upsert( any(), anyBoolean(), any() ) ).thenAnswer( a -> a.getArgument( 0, Gene.class ) );
        loader.setLimit( 2 );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), this::load );

        verify( geneWriteService, times( 2 ) ).upsert( any(), anyBoolean(), any() );
        assertThat( loader.getLoadedGeneCount() ).isEqualTo( 2 );
        verifyNoInteractions( taxonService );
    }

    @Test
    void aCompleteRunLoadsEveryGene() {
        when( geneWriteService.upsert( any(), anyBoolean(), any() ) ).thenAnswer( a -> a.getArgument( 0, Gene.class ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), this::load );

        verify( geneWriteService, times( 4 ) ).upsert( any(), anyBoolean(), any() );
    }

    @Test
    void withRemovalOffEachKeptProductReachesTheSink() {
        when( geneWriteService.upsert( any(), anyBoolean(), any() ) ).thenAnswer( a -> {
            Gene gene = a.getArgument( 0, Gene.class );
            a.<Consumer<GeneProductChange>>getArgument( 2 ).accept( keptRemoval( gene ) );
            return gene;
        } );
        List<GeneProductChange> received = new ArrayList<>();
        loader.setRemoveProducts( false );
        loader.setChangeSink( received::add );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), this::load );

        verify( geneWriteService, times( 4 ) ).upsert( any(), eq( false ), any() );
        assertThat( received ).hasSize( 4 ).allSatisfy( c -> assertThat( c.applied() ).isFalse() );
        assertThat( loader.getKeptGeneProducts() ).isEqualTo( 4 );
    }

    /**
     * The upsert's transaction is rolled back, so the changes it reported did not happen.
     */
    @Test
    void aFailedGeneSendsNothingToTheSink() {
        when( geneWriteService.upsert( any(), anyBoolean(), any() ) ).thenAnswer( a -> {
            a.<Consumer<GeneProductChange>>getArgument( 2 ).accept( keptRemoval( a.getArgument( 0, Gene.class ) ) );
            throw new IllegalStateException( "flush failed" );
        } );
        List<GeneProductChange> received = new ArrayList<>();
        loader.setChangeSink( received::add );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::load ).hasRootCauseMessage( "flush failed" ) );

        assertThat( received ).isEmpty();
    }

    private static GeneProductChange keptRemoval( Gene gene ) {
        return new GeneProductChange( GeneProductChange.Kind.REMOVE, false, "human", gene.getNcbiGeneId(),
                gene.getOfficialSymbol(), null, null, 1L, "NM_1", "1", 1, 0, List.of( "GPL1" ) );
    }

    private void load() throws Exception {
        loader.load( FileTools.resourceToPath( GENE_INFO ), FileTools.resourceToPath( GENE2ACCESSION ),
                FileTools.resourceToPath( GENE_HISTORY ), null, human );
    }
}

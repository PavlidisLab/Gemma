package ubic.gemma.core.loader.genome.gene.ncbi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.FileTools;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.time.Duration;

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
        when( geneWriteService.upsert( any() ) ).thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::load )
                        .hasMessageContaining( "resume with -restart" )
                        .hasRootCauseMessage( "database went away" ) );
        verify( geneWriteService, times( 1 ) ).upsert( any() );
        verifyNoInteractions( taxonService );
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
        when( geneWriteService.upsert( any() ) ).thenAnswer( a -> a.getArgument( 0, Gene.class ) );
        loader.setLimit( 2 );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), this::load );

        verify( geneWriteService, times( 2 ) ).upsert( any() );
        assertThat( loader.getLoadedGeneCount() ).isEqualTo( 2 );
        verifyNoInteractions( taxonService );
    }

    @Test
    void aCompleteRunLoadsEveryGene() {
        when( geneWriteService.upsert( any() ) ).thenAnswer( a -> a.getArgument( 0, Gene.class ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), this::load );

        verify( geneWriteService, times( 4 ) ).upsert( any() );
    }

    private void load() throws Exception {
        loader.load( FileTools.resourceToPath( GENE_INFO ), FileTools.resourceToPath( GENE2ACCESSION ),
                FileTools.resourceToPath( GENE_HISTORY ), null, human );
    }
}

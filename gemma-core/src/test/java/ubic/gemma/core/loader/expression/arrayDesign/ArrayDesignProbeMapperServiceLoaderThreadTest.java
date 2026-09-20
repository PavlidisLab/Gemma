package ubic.gemma.core.loader.expression.arrayDesign;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.ProbeMapper;
import ubic.gemma.core.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysis;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysisFactory;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.persister.GenomePersister;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultReadService;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * mapPlatformToGenes checks the GoldenPath database, deletes the platform's alignment-based associations, then maps
 * each probe on this thread and saves the associations on another. A failure on either side must end the mapping as a
 * failure and stop the other.
 */
class ArrayDesignProbeMapperServiceLoaderThreadTest {

    private static final int PROBES = 5;

    private final ArrayDesignService arrayDesignService = mock();
    private final ArrayDesignReportService arrayDesignReportService = mock();
    private final BlatResultReadService blatResultService = mock();
    private final GenomePersister genomePersister = mock();
    private final GoldenPathSequenceAnalysisFactory goldenPathSequenceAnalysisFactory = mock();
    private final ProbeMapper probeMapper = mock();

    private final List<Thread> loaderThreads = Collections.synchronizedList( new ArrayList<>() );
    private final TaskExecutor taskExecutor = r -> {
        Thread t = new Thread( r, "probe mapper loader" );
        t.setDaemon( true );
        loaderThreads.add( t );
        t.start();
    };

    private final GoldenPathSequenceAnalysis goldenPathDb = mock();

    private ArrayDesignProbeMapperServiceImpl service;
    private ArrayDesign arrayDesign;

    @BeforeEach
    void setUp() {
        service = new ArrayDesignProbeMapperServiceImpl( mock(), mock( ArrayDesignAnnotationService.class ),
                arrayDesignReportService, arrayDesignService, probeMapper, mock(), blatResultService, mock(),
                mock( ExpressionDataFileService.class ), mock(), mock(), genomePersister,
                goldenPathSequenceAnalysisFactory, taskExecutor );

        Taxon human = Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true );
        arrayDesign = ArrayDesign.Factory.newInstance( "GPL0", human );
        arrayDesign.setId( 1L );
        arrayDesign.setTechnologyType( TechnologyType.ONECOLOR );
        for ( int i = 0; i < PROBES; i++ ) {
            arrayDesign.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "probe" + i, arrayDesign,
                    BioSequence.Factory.newInstance( "seq" + i, human ) ) );
        }

        when( arrayDesignService.getTaxaFromBioSequences( arrayDesign ) ).thenReturn( Collections.singleton( human ) );
        when( goldenPathSequenceAnalysisFactory.create( human ) ).thenReturn( goldenPathDb );
        when( blatResultService.findByBioSequence( any() ) )
                .thenAnswer( a -> new ArrayList<>( Collections.singleton( BlatResult.Factory.newInstance() ) ) );
        when( probeMapper.processBlatResults( any(), anyCollection(), any() ) ).thenAnswer( a -> {
            GeneProduct gp = GeneProduct.Factory.newInstance();
            gp.setId( 1L );
            BlatAssociation ba = BlatAssociation.Factory.newInstance();
            ba.setGeneProduct( gp );
            Map<String, Collection<BlatAssociation>> result = new HashMap<>();
            result.put( "seq", new ArrayList<>( Collections.singleton( ba ) ) );
            return result;
        } );
    }

    @AfterEach
    void stopLeftoverLoaders() {
        loaderThreads.forEach( Thread::interrupt );
    }

    /**
     * The loader logged its error, set its done flag and died; with room in the queue for what was left, the mapping
     * then finished as a success with most associations missing.
     */
    @Test
    void aLoaderFailureFailsTheMapping() {
        when( genomePersister.persistBlatAssociation( any() ) ).thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::map )
                        .hasMessageContaining( "old alignment-based associations were already deleted" )
                        .hasRootCauseMessage( "database went away" ) );
        verify( arrayDesignService ).deleteGeneProductAlignmentAssociations( arrayDesign );
        verifyNoInteractions( arrayDesignReportService );
    }

    /**
     * With a full queue, {@code queue.add} threw "Queue full", which named neither the loader nor its failure.
     */
    @Test
    void aLoaderFailureWithAFullQueueNamesTheLoaderFailure() {
        service.setQueueSize( 1 );
        when( genomePersister.persistBlatAssociation( any() ) ).thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::map )
                        .hasMessageContaining( "old alignment-based associations were already deleted" )
                        .hasRootCauseMessage( "database went away" ) );
    }

    /**
     * The loader caught {@link Exception}; an {@link Error} ended it without setting its done flag, and the mapping
     * waited forever.
     */
    @Test
    void anErrorInTheLoaderFailsTheMapping() {
        when( genomePersister.persistBlatAssociation( any() ) ).thenThrow( new AssertionError( "broken invariant" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( this::map )
                        .hasRootCauseInstanceOf( AssertionError.class ) );
    }

    /**
     * A failure while mapping left the loader polling its empty queue forever.
     */
    @Test
    void aMappingFailureStopsTheLoader() throws InterruptedException {
        when( blatResultService.findByBioSequence( any() ) ).thenThrow( new IllegalStateException( "goldenpath went away" ) );

        assertThatThrownBy( this::map ).hasMessage( "goldenpath went away" );

        assertThat( loaderThreads ).hasSize( 1 );
        loaderThreads.get( 0 ).join( 10_000 );
        assertThat( loaderThreads.get( 0 ).isAlive() ).as( "the loader thread has stopped" ).isFalse();
    }

    /**
     * A platform whose mapping aborts after the delete keeps only what the loader had saved by then, and the caller
     * used to get a stack trace that said nothing about it: GPL6887 went from 35,376 mapped probes to 1,604 on a
     * transient GoldenPath connection fault, and only a driver that counted beforehand noticed.
     */
    @Test
    void aFailureAfterTheDeleteSaysThePlatformIsLeftIncomplete() {
        when( blatResultService.findByBioSequence( any() ) )
                .thenThrow( new IllegalStateException( "No database selected" ) );

        assertThatThrownBy( this::map ).hasMessage( "No database selected" );

        verify( arrayDesignService ).deleteGeneProductAlignmentAssociations( arrayDesign );
        verify( arrayDesignService ).countCompositeSequencesWithGenes( arrayDesign, false );
    }

    /**
     * The GoldenPath connection was opened after the delete, so a database without the tables the mapping reads
     * failed on the first probe with the platform's associations already gone.
     */
    @Test
    void aGoldenPathDatabaseThatCannotMapFailsBeforeAnythingIsDeleted() throws InterruptedException {
        ProbeMapperConfig config = new ProbeMapperConfig();
        doThrow( new IllegalStateException( "GoldenPath database hg38 has no ncbiRefSeqCurated table" ) )
                .when( goldenPathDb ).checkTablesForProbeMapping( config );

        assertThatThrownBy( () -> service.processArrayDesign( arrayDesign, config, true ) )
                .hasMessageContaining( "has no ncbiRefSeqCurated table" );

        verify( arrayDesignService, never() ).deleteGeneProductAlignmentAssociations( any() );
        verifyNoInteractions( probeMapper, genomePersister, arrayDesignReportService );
        verify( goldenPathDb ).close();
        loaderThreads.get( 0 ).join( 10_000 );
        assertThat( loaderThreads.get( 0 ).isAlive() ).as( "the loader thread has stopped" ).isFalse();
    }

    @Test
    void aCompleteMappingSavesEveryAssociation() throws InterruptedException {
        service.setQueueSize( 1 );
        when( genomePersister.persistBlatAssociation( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), this::map );

        verify( genomePersister, times( PROBES ) ).persistBlatAssociation( any() );
        verify( arrayDesignReportService ).generateArrayDesignReport( 1L );
        // no "left incomplete" alarm on a mapping that finished
        verify( arrayDesignService, never() ).countCompositeSequencesWithGenes( any(), anyBoolean() );
        loaderThreads.get( 0 ).join( 10_000 );
        assertThat( loaderThreads.get( 0 ).isAlive() ).isFalse();
    }

    private void map() {
        service.processArrayDesign( arrayDesign, new ProbeMapperConfig(), true );
    }
}

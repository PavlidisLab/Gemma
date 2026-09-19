package ubic.gemma.core.loader.expression.arrayDesign;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.Blat;
import ubic.gemma.core.goldenpath.GoldenPathQuery;
import ubic.gemma.core.goldenpath.GoldenPathQueryFactory;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.persister.GenomePersister;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * blatPlatform deletes a platform's alignments before saving new ones. It must not delete them when there is nothing
 * to replace them with, and it must not report such a run as a success.
 */
class ArrayDesignSequenceAlignmentServiceDeletionTest {

    private final ArrayDesignReportService arrayDesignReportService = mock();
    private final ArrayDesignService arrayDesignService = mock();
    private final BioSequenceService bioSequenceService = mock();
    private final GenomePersister genomePersister = mock();
    private final GoldenPathQueryFactory goldenPathQueryFactory = mock();
    private final Blat blat = mock();

    private final Taxon human = Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true );
    private ArrayDesignSequenceAlignmentServiceImpl service;
    private ArrayDesign arrayDesign;
    private BioSequence sequence;

    @BeforeEach
    void setUp() {
        service = new ArrayDesignSequenceAlignmentServiceImpl( arrayDesignReportService, arrayDesignService,
                bioSequenceService, genomePersister, goldenPathQueryFactory );
        arrayDesign = ArrayDesign.Factory.newInstance( "GPL0", human );
        arrayDesign.setId( 1L );
        sequence = BioSequence.Factory.newInstance( "seq0", human );
        sequence.setId( 10L );
        sequence.setSequence( "ACGTACGTACGTACGTACGT" );
        sequence.setLength( 20L );
        arrayDesign.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "probe0", arrayDesign, sequence ) );

        when( arrayDesignService.getTaxaFromBioSequences( arrayDesign ) ).thenReturn( Collections.singleton( human ) );
        when( arrayDesignService.thaw( arrayDesign ) ).thenReturn( arrayDesign );
        when( bioSequenceService.thaw( anyCollection() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( goldenPathQueryFactory.create( any() ) ).thenReturn( mock( GoldenPathQuery.class ) );
        when( genomePersister.persistBlatResults( anyCollection() ) ).thenAnswer( a -> a.getArgument( 0 ) );
    }

    /**
     * The old alignments were deleted as soon as the first taxon was reached, whether or not BLAT found anything, and
     * the run returned normally.
     */
    @Test
    void aBlatRunWithNoResultsKeepsTheOldAlignmentsAndFails() throws IOException {
        when( blat.blatQuery( anyCollection(), anyBoolean(), any() ) ).thenReturn( new HashMap<>() );

        assertThatThrownBy( () -> service.processArrayDesign( arrayDesign, blat ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "No alignments were found for any of the 1 sequences" )
                .hasMessageContaining( "existing alignments were kept" );

        verify( arrayDesignService, never() ).deleteAlignmentData( any() );
        verifyNoInteractions( genomePersister, arrayDesignReportService );
    }

    @Test
    void aBlatRunWithResultsReplacesTheOldAlignments() throws IOException {
        Map<BioSequence, List<BlatResult>> results = new HashMap<>();
        results.put( sequence, new ArrayList<>( Collections.singleton( blatResult( sequence ) ) ) );
        when( blat.blatQuery( anyCollection(), anyBoolean(), any() ) ).thenReturn( results );

        service.processArrayDesign( arrayDesign, blat );

        verify( arrayDesignService ).deleteAlignmentData( arrayDesign );
        verify( genomePersister ).persistBlatResults( argThat( c -> c.size() == 1 ) );
        verify( arrayDesignReportService ).generateArrayDesignReport( 1L );
    }

    /**
     * From a file: the old alignments were deleted before the results were matched to the platform's sequences by
     * name. A result for a sequence not on the platform then threw a NullPointerException ("Cannot hash a transient
     * entity") when it was set aside, so the run failed with every alignment deleted and none saved.
     */
    @Test
    void aBlatFileMatchingNoSequenceKeepsTheOldAlignmentsAndFails() {
        BlatResult unmatched = blatResult( BioSequence.Factory.newInstance( "not-on-this-platform", human ) );

        assertThatThrownBy( () -> service.processArrayDesign( arrayDesign, human,
                new ArrayList<>( Collections.singleton( unmatched ) ) ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "existing alignments were kept" );

        verify( arrayDesignService, never() ).deleteAlignmentData( any() );
        verifyNoInteractions( genomePersister );
    }

    private BlatResult blatResult( BioSequence query ) {
        BioSequence chromosomeSequence = BioSequence.Factory.newInstance( "chr1", human );
        chromosomeSequence.setId( 20L );
        Chromosome chromosome = Chromosome.Factory.newInstance( "1", null, chromosomeSequence, human );
        chromosome.setId( 30L );
        BlatResult br = BlatResult.Factory.newInstance();
        br.setQuerySequence( query );
        br.setTargetChromosome( chromosome );
        br.setTargetStart( 100L );
        br.setTargetEnd( 120L );
        br.setStrand( "+" );
        return br;
    }
}

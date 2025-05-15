package ubic.gemma.core.loader.expression.arrayDesign;

import ubic.gemma.core.analysis.sequence.Blat;
import ubic.gemma.core.analysis.sequence.ShellDelegatingBlat.BlattableGenome;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.io.InputStream;
import java.util.*;

class MockBlat implements Blat {

    private static final Random RANDOM = new Random();
    private final Taxon taxon;

    MockBlat( Taxon t ) {
        this.taxon = t;
    }

    @Override
    public List<BlatResult> blatQuery( BioSequence b ) {
        List<BlatResult> result = new ArrayList<>();
        BioSequence chromseq = new PersistentDummyObjectHelper().getTestNonPersistentBioSequence( taxon );
        chromseq.setLength( ( long ) 1e7 );
        BlatResult br = BlatResult.Factory.newInstance();

        Chromosome chromosome = new Chromosome( "XXX", null, chromseq, taxon );
        br.setTargetChromosome( chromosome );
        assert br.getTargetChromosome().getSequence() != null;
        long targetStart = MockBlat.RANDOM.nextInt( chromseq.getLength().intValue() );
        br.setQuerySequence( b );
        br.setTargetStart( targetStart );
        br.setTargetEnd( targetStart + b.getLength() );
        br.setMatches( ( int ) ( b.getLength() - 1 ) );
        br.setMismatches( 1 );
        br.setRepMatches( 0 );
        br.setQueryGapCount( 0 );
        br.setQueryGapBases( 0 );
        br.setQueryStart( 0 );
        br.setQueryEnd( b.getLength().intValue() );
        br.setTargetGapBases( 0 );
        br.setTargetGapCount( 0 );
        PhysicalLocation targetAlignedRegion = PhysicalLocation.Factory.newInstance();
        targetAlignedRegion.setChromosome( br.getTargetChromosome() );
        targetAlignedRegion.setNucleotide( targetStart );
        targetAlignedRegion.setNucleotideLength( b.getLength().intValue() );
        targetAlignedRegion.setStrand( "+" );

        result.add( br );

        return result;
    }

    @Override
    public List<BlatResult> blatQuery( BioSequence b, Taxon t, boolean sensitive ) {
        return this.blatQuery( b );
    }

    @Override
    public Map<BioSequence, List<BlatResult>> blatQuery( Collection<BioSequence> sequences, boolean sensitive,
            Taxon t ) {
        Map<BioSequence, List<BlatResult>> results = new HashMap<>();
        for ( BioSequence bioSequence : sequences ) {
            results.put( bioSequence, this.blatQuery( bioSequence ) );
        }
        return results;
    }

    @Override
    public Map<BioSequence, List<BlatResult>> blatQuery( Collection<BioSequence> sequences, Taxon t ) {
        return this.blatQuery( sequences, false, t );
    }

    @Override
    public double getBlatScoreThreshold() {
        return 0;
    }

    @Override
    public void setBlatScoreThreshold( double blatScoreThreshold ) {

    }

    @Override
    public String getGfClientExe() {
        return null;
    }

    @Override
    public String getGfServerExe() {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public int getHumanServerPort() {
        return 0;
    }

    @Override
    public int getMouseServerPort() {
        return 0;
    }

    @Override
    public int getRatServerPort() {
        return 0;
    }

    @Override
    public String getSeqDir() {
        return null;
    }

    @Override
    public String getSeqFiles( BlattableGenome genome ) {
        return null;
    }

    @Override
    public List<BlatResult> processPsl( InputStream inputStream, Taxon t ) {
        return null;
    }

    @Override
    public void startServer( BlattableGenome genome, int port ) {

    }

    @Override
    public void stopServer( int port ) {

    }

}
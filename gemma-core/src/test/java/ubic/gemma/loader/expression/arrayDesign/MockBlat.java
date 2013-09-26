package ubic.gemma.loader.expression.arrayDesign;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import ubic.gemma.apps.Blat;
import ubic.gemma.apps.ShellDelegatingBlat.BlattableGenome;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.testing.PersistentDummyObjectHelper;

class MockBlat implements Blat {

    private static final Random RANDOM = new Random();
    private Taxon taxon;

    public MockBlat( Taxon t ) {
        this.taxon = t;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.Blat#blatQuery(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public Collection<BlatResult> blatQuery( BioSequence b ) throws IOException {
        Collection<BlatResult> result = new HashSet<>();
        BioSequence chromseq = PersistentDummyObjectHelper.getTestNonPersistentBioSequence( taxon );
        chromseq.setLength( ( long ) 1e7 );
        BlatResult br = BlatResult.Factory.newInstance();

        Chromosome chromosome = Chromosome.Factory.newInstance( "XXX", null, chromseq, taxon );
        br.setTargetChromosome( chromosome );
        assert br.getTargetChromosome().getSequence() != null;
        long targetstart = RANDOM.nextInt( chromseq.getLength().intValue() );
        br.setQuerySequence( b );
        br.setTargetStart( targetstart );
        br.setTargetEnd( targetstart + b.getLength() );
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
        targetAlignedRegion.setNucleotide( targetstart );
        targetAlignedRegion.setNucleotideLength( b.getLength().intValue() );
        targetAlignedRegion.setStrand( "+" );

        result.add( br );

        return result;
    }

    @Override
    public Collection<BlatResult> blatQuery( BioSequence b, Taxon t, boolean sensitive ) throws IOException {
        return blatQuery( b );
    }

    @Override
    public Map<BioSequence, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences, boolean sensitive,
            Taxon t ) throws IOException {
        Map<BioSequence, Collection<BlatResult>> results = new HashMap<BioSequence, Collection<BlatResult>>();
        for ( BioSequence bioSequence : sequences ) {
            results.put( bioSequence, blatQuery( bioSequence ) );
        }
        return results;
    }

    @Override
    public Map<BioSequence, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences, Taxon t )
            throws IOException {
        return blatQuery( sequences, false, t );
    }

    @Override
    public double getBlatScoreThreshold() {
        return 0;
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
    public Collection<BlatResult> processPsl( InputStream inputStream, Taxon t ) throws IOException {
        return null;
    }

    @Override
    public void setBlatScoreThreshold( double blatScoreThreshold ) {

    }

    @Override
    public void startServer( BlattableGenome genome, int port ) throws IOException {

    }

    @Override
    public void stopServer( int port ) {

    }

}
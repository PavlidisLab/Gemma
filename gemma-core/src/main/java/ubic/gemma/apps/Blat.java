package ubic.gemma.apps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import ubic.gemma.apps.ShellDelegatingBlat.BlattableGenome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

public interface Blat {

    /**
     * This value is basically a threshold fraction of aligned bases in the query. Hits below this score are simply not
     * reported. {@link BlatResult} has implementation of score computation.
     * 
     * @see BlatResult
     */
    public static final double DEFAULT_BLAT_SCORE_THRESHOLD = 0.7;
    public static final double STEPSIZE = 7;

    /**
     * Run a BLAT search using the gfClient.
     * 
     * @param b. The genome is inferred from the Taxon held by the sequence.
     * @return Collection of BlatResult objects.
     * @throws IOException
     */
    public abstract Collection<BlatResult> blatQuery( BioSequence b ) throws IOException;

    /**
     * Run a BLAT search using the gfClient.
     * 
     * @param b
     * @param genome
     * @param sensitive if true use the more sensitive gfServer, if available.
     * @return Collection of BlatResult objects.
     * @throws IOException
     */
    public abstract Collection<BlatResult> blatQuery( BioSequence b, Taxon taxon, boolean sensitive )
            throws IOException;

    /**
     * @param sequences
     * @param taxon The taxon whose database will be searched.
     * @return map of the input sequences to a corresponding collection of blat result(s)
     * @throws IOException
     */
    public abstract Map<BioSequence, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences,
            boolean sensitive, Taxon taxon ) throws IOException;

    public abstract Map<BioSequence, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences, Taxon taxon )
            throws IOException;

    /**
     * @return the blatScoreThreshold
     */
    public abstract double getBlatScoreThreshold();

    /**
     * @return Returns the gfClientExe.
     */
    public abstract String getGfClientExe();

    /**
     * @return Returns the gfServerExe.
     */
    public abstract String getGfServerExe();

    /**
     * @return Returns the host.
     */
    public abstract String getHost();

    /**
     * @return Returns the humanServerPort.
     */
    public abstract int getHumanServerPort();

    /**
     * @return Returns the mouseServerPort.
     */
    public abstract int getMouseServerPort();

    /**
     * @return Returns the ratServerPort.
     */
    public abstract int getRatServerPort();

    /**
     * @return Returns the seqDir.
     */
    public abstract String getSeqDir();

    /**
     * @return Returns the seqFiles.
     */
    public abstract String getSeqFiles( BlattableGenome genome );

    /**
     * @param inputStream to the Blat output file in psl format
     * @return processed results.
     */
    public abstract Collection<BlatResult> processPsl( InputStream inputStream, Taxon taxon ) throws IOException;

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    public abstract void setBlatScoreThreshold( double blatScoreThreshold );

    /**
     * Start the server, if the port isn't already being used. If the port is in use, we assume it is a gfServer.
     */
    public abstract void startServer( BlattableGenome genome, int port ) throws IOException;

    /**
     * Stop the gfServer, if it was started by this.
     */
    public abstract void stopServer( int port );

}
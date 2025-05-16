package ubic.gemma.core.analysis.sequence;

import ubic.gemma.core.analysis.sequence.ShellDelegatingBlat.BlattableGenome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused") // Possible external use
public interface Blat {

    /**
     * This value is basically a threshold fraction of aligned bases in the query. Hits below this score are simply not
     * reported. {@link BlatResult} has implementation of score computation.
     *
     * @see BlatResult
     */
    double DEFAULT_BLAT_SCORE_THRESHOLD = 0.7;
    double STEPSIZE = 7;

    /**
     * Run a BLAT search using the gfClient.
     *
     * @param b The genome is inferred from the Taxon held by the sequence.
     * @return Collection of BlatResult objects.
     * @throws IOException when there are IO problems.
     */
    List<BlatResult> blatQuery( BioSequence b ) throws IOException;

    /**
     * Run a BLAT search using the gfClient.
     *
     * @param b         The genome is inferred from the Taxon held by the sequence.
     * @param sensitive if true use the more sensitive gfServer, if available.
     * @param taxon taxon
     * @return Collection of BlatResult objects.
     * @throws IOException when there are IO problems.
     */
    List<BlatResult> blatQuery( BioSequence b, Taxon taxon, boolean sensitive ) throws IOException;

    /**
     * @param sequences The genome is inferred from the Taxon held by the sequence.
     * @param taxon     The taxon whose database will be searched.
     * @param sensitive if true use the more sensitive gfServer, if available.
     * @return map of the input sequences to a corresponding collection of blat result(s)
     * @throws IOException when there are IO problems.
     */
    Map<BioSequence, List<BlatResult>> blatQuery( Collection<BioSequence> sequences, boolean sensitive,
            Taxon taxon ) throws IOException;

    Map<BioSequence, List<BlatResult>> blatQuery( Collection<BioSequence> sequences, Taxon taxon )
            throws IOException;

    /**
     * @return the blatScoreThreshold
     */
    double getBlatScoreThreshold();

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    void setBlatScoreThreshold( double blatScoreThreshold );

    /**
     * @return Returns the gfClientExe.
     */
    String getGfClientExe();

    /**
     * @return Returns the gfServerExe.
     */
    String getGfServerExe();

    /**
     * @return Returns the host.
     */
    String getHost();

    /**
     * @return Returns the humanServerPort.
     */
    int getHumanServerPort();

    /**
     * @return Returns the mouseServerPort.
     */
    int getMouseServerPort();

    /**
     * @return Returns the ratServerPort.
     */
    int getRatServerPort();

    /**
     * @return Returns the seqDir.
     */
    String getSeqDir();

    /**
     * @param genome genome
     * @return Returns the seqFiles.
     */
    String getSeqFiles( BlattableGenome genome );

    /**
     * @param inputStream to the Blat output file in psl format
     * @param taxon       taxon
     * @return processed results.
     * @throws IOException when there are IO problems.
     */
    List<BlatResult> processPsl( InputStream inputStream, Taxon taxon ) throws IOException;

    /**
     * Start the server, if the port isn't already being used. If the port is in use, we assume it is a gfServer.
     *
     * @param genome genome
     * @param port   port
     * @throws IOException when there are IO problems.
     */
    void startServer( BlattableGenome genome, int port ) throws IOException;

    /**
     * Stop the gfServer, if it was started by this.
     *
     * @param port port
     */
    void stopServer( int port );

}
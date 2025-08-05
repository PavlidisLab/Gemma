package ubic.gemma.core.analysis.sequence;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Blat {

    /**
     * This value is basically a threshold fraction of aligned bases in the query. Hits below this score are simply not
     * reported. {@link BlatResult} has implementation of score computation.
     *
     * @see BlatResult
     */
    double DEFAULT_BLAT_SCORE_THRESHOLD = 0.7;

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
     * Set the blat score threshold to use.
     * <p>
     * Defaults to {@link #DEFAULT_BLAT_SCORE_THRESHOLD}.
     */
    void setBlatScoreThreshold( double blatScoreThreshold );
}
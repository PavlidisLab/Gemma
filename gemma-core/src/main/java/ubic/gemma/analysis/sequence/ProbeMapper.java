package ubic.gemma.analysis.sequence;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

public interface ProbeMapper {

    /**
     * Given some blat results (possibly for multiple sequences) determine which if any gene products they should be
     * associated with; if there are multiple results for a single sequence, these are further analyzed for specificity
     * and redundancy, so that there is a single BlatAssociation between any sequence andy andy gene product.
     * <p>
     * This is a major entrypoint for this API.
     * 
     * @param goldenPathDb
     * @param blatResults
     * @param config
     * @return A map of sequence names to collections of blat associations for each sequence.
     */
    public abstract Map<String, Collection<BlatAssociation>> processBlatResults(
            GoldenPathSequenceAnalysis goldenPathDb, Collection<BlatResult> blatResults, ProbeMapperConfig config );

    /**
     * Given some blat results (possibly for multiple sequences) determine which if any gene products they should be
     * associatd with; if there are multiple results for a single sequence, these are further analyzed for specificity
     * and redundancy, so that there is a single BlatAssociation between any sequence andy andy gene product. Default
     * settings (ProbeMapperConfig) are used.
     * <p>
     * This is a major entrypoint for this API.
     * 
     * @param goldenPathDb
     * @param blatResults
     * @return A map of sequence names to collections of blat associations for each sequence.
     * @see ProbeMapperConfig
     */
    public abstract Map<String, Collection<BlatAssociation>> processBlatResults(
            GoldenPathSequenceAnalysis goldenPathDb, Collection<BlatResult> blatResults );

    /**
     * Given a genbank accession (for a mRNA or EST), find alignment data from GoldenPath.
     * 
     * @param goldenPathDb
     * @param genbankId
     * @param map of sequence names to BLAT associations.
     */
    public abstract Map<String, Collection<BlatAssociation>> processGbId( GoldenPathSequenceAnalysis goldenPathDb,
            String genbankId );

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankIds
     * @return
     */
    public abstract Map<String, Collection<BlatAssociation>> processGbIds( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<String[]> genbankIds );

    /**
     * Get BlatAssociation results for a single sequence. If you have multiple sequences to run it is always better to
     * use processSequences();
     * 
     * @param goldenPath
     * @param sequence
     * @return
     * @see processSequences
     */
    public abstract Collection<BlatAssociation> processSequence( GoldenPathSequenceAnalysis goldenPath,
            BioSequence sequence );

    /**
     * Given a collection of sequences, blat them against the selected genome.
     * 
     * @param goldenpath for the genome to be used.
     * @param sequences
     * @return
     */
    public abstract Map<String, Collection<BlatAssociation>> processSequences( GoldenPathSequenceAnalysis goldenpath,
            Collection<BioSequence> sequences, ProbeMapperConfig config );

}
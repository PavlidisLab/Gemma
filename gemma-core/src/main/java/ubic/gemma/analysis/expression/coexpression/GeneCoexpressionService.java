package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;

public interface GeneCoexpressionService {

    /**
     * @param inputEeIds
     * @param genes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @return
     */
    public abstract Collection<CoexpressionValueObjectExt> coexpressionSearchQuick( Collection<Long> inputEeIds,
            Collection<Gene> genes, int stringency, int maxResults, boolean queryGenesOnly, boolean skipDetails );

    /**
     * Main entry point. Note that if possible, the query will be done using results from an ExpressionExperimentSet.
     * 
     * @param inputEeIds Expression experiments ids to consider
     * @param genes Genes to find coexpression for
     * @param stringency Minimum support level
     * @param queryGenesOnly Whether to return only coexpression among the query genes (assuming there are more than
     *        one). Otherwise, coexpression with genes 'external' to the queries will be returned.
     * @param forceProbeLevelSearch If a probe-level search should always be done. This is primarily a testing and
     *        debugging feature. If false, searches will be done using 'canned' results if possible.
     * @return
     */
    public abstract CoexpressionMetaValueObject coexpressionSearch( Collection<Long> inputEeIds,
            Collection<Gene> genes, int stringency, int maxResults, boolean queryGenesOnly,
            boolean forceProbeLevelSearch );

    /**
     * Skips some of the postprocessing steps, use in situations where raw speed is more important than details.
     * 
     * @param eeSetId
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @return
     */
    public abstract Collection<CoexpressionValueObjectExt> coexpressionSearchQuick( Long eeSetId,
            Collection<Gene> queryGenes, int stringency, int maxResults, boolean queryGenesOnly, boolean skipDetails );

}
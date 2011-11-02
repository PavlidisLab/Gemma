package ubic.gemma.model.analysis.expression.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import ubic.gemma.model.genome.Gene;

/**
 * @author luke The MultipleCoexpressionCollectionValueObject is used for storing the results of a multiple coexpression
 *         search.
 */
public class MultipleCoexpressionCollectionValueObject {

    private int minimumCommonQueryGenes;
    // private Map<Gene, CoexpressionCollectionValueObject> queries;
    private Collection<Gene> queryGenes;
    private MultipleCoexpressionTypeValueObject geneCoexpressionData;
//    private MultipleCoexpressionTypeValueObject predictedCoexpressionData;
//    private MultipleCoexpressionTypeValueObject alignedCoexpressionData;
    private double elapsedWallSeconds;

    public MultipleCoexpressionCollectionValueObject() {
        minimumCommonQueryGenes = 2;
        // queries = Collections.synchronizedMap( new HashMap<Gene, CoexpressionCollectionValueObject>() );
        queryGenes = Collections.synchronizedCollection( new ArrayList<Gene>() );
        geneCoexpressionData = new MultipleCoexpressionTypeValueObject();
//        predictedCoexpressionData = new MultipleCoexpressionTypeValueObject();
//        alignedCoexpressionData = new MultipleCoexpressionTypeValueObject();
    }

    public void addCoexpressionCollection( CoexpressionCollectionValueObject coexpressionCollection ) {
        synchronized ( this ) {
            // queries.put( coexpressionCollection.getQueryGene(), coexpressionCollection );
            queryGenes.add( coexpressionCollection.getQueryGene() );
            geneCoexpressionData.addCoexpressionCollection( coexpressionCollection.getQueryGene(),
                    coexpressionCollection.getKnownGeneCoexpression() );
//            predictedCoexpressionData.addCoexpressionCollection( coexpressionCollection.getQueryGene(),
//                    coexpressionCollection.getPredictedGeneCoexpression() );
//            alignedCoexpressionData.addCoexpressionCollection( coexpressionCollection.getQueryGene(),
//                    coexpressionCollection.getProbeAlignedRegionCoexpression() );
        }
    }

    /**
     * @return those coexpressed genes that are common to multiple query genes
     */
    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedGenes() {
        return this.geneCoexpressionData.getCommonCoexpressedGenes( minimumCommonQueryGenes );
    }

//    /**
//     * @return those coexpressed predicted genes that are common to multiple query genes
//     */
//    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedPredictedGenes() {
//        return this.predictedCoexpressionData.getCommonCoexpressedGenes( minimumCommonQueryGenes );
//    }
//
//    /**
//     * @return those coexpressed probe-aligned regions that are common to multiple query genes
//     */
//    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedProbeAlignedRegions() {
//        return this.alignedCoexpressionData.getCommonCoexpressedGenes( minimumCommonQueryGenes );
//    }

    /**
     * This gives the amount of time we had to wait for the queries (which can be less than the time per query because
     * of threading)
     * 
     * @return
     */
    public double getElapsedWallSeconds() {
        return elapsedWallSeconds;
    }

    /**
     * @return the MultipleCoexpressonTypeValueObject for standard genes
     */
    public MultipleCoexpressionTypeValueObject getGeneCoexpressionType() {
        return this.geneCoexpressionData;
    }

    /**
     * @return the minimum number of query genes with which a result gene must exhibit coexpression to be displayed
     */
    public int getMinimumCommonQueries() {
        return minimumCommonQueryGenes;
    }

    /**
     * @return the number of Genes
     */
    public int getNumGenes() {
        return this.geneCoexpressionData.getNumberOfGenes();
    }
//
//    /**
//     * @return the numPredictedGenes
//     */
//    public int getNumPredictedGenes() {
//        return this.predictedCoexpressionData.getNumberOfGenes();
//    }

    // /**
    // * @return the CoexpressionCollectionValueObjects for all query genes
    // */
    // public Collection<CoexpressionCollectionValueObject> getQueryResults() {
    // return queries.values();
    // }
//
//    /**
//     * @return the numProbeAlignedRegions
//     */
//    public int getNumProbeAlignedRegions() {
//        return this.alignedCoexpressionData.getNumberOfGenes();
//    }
//
//    /**
//     * @return the MultipleCoexpressionTypeValueObject for predicted Genes
//     */
//    public MultipleCoexpressionTypeValueObject getPredictedCoexpressionType() {
//        return this.predictedCoexpressionData;
//    }
//
//    /**
//     * @return the MultipleCoexpressonTypeValueObject for probe aligned regions
//     */
//    public MultipleCoexpressionTypeValueObject getProbeAlignedCoexpressionType() {
//        return this.alignedCoexpressionData;
//    }

    /**
     * @param gene the Gene of interest
     * @return a subset of the geneCommonCoexpressionValueObjects that exhibit coexpression with the specified Gene
     */
    public CommonCoexpressionValueObject getQueriesForGene( Gene gene ) {
        return this.geneCoexpressionData.getQueriesForGene( gene );
    }
//
//    /**
//     * @param gene the Gene of interest
//     * @return a subset of the predicted gene CommonCoexpressionValueObjects that exhibit coexpression with the
//     *         specified Gene
//     */
//    public CommonCoexpressionValueObject getQueriesForPredictedGene( Gene gene ) {
//        return this.predictedCoexpressionData.getQueriesForGene( gene );
//    }

//    /**
//     * @param gene the Gene of interest
//     * @return a subset of the probe-aligned region CommonCoexpressionValueObjects that exhibit coexpression with the
//     *         specified Gene
//     */
//    public CommonCoexpressionValueObject getQueriesForProbeAlignedRegion( Gene gene ) {
//        return this.alignedCoexpressionData.getQueriesForGene( gene );
//    }

    /**
     * @return the query genes
     */
    public Collection<Gene> getQueryGenes() {
        // return queries.keySet();
        return queryGenes;
    }

    /**
     * Set the amount of time we had to wait for the queries (which can be less than the time per query because
     * 
     * @param elapsedWallTime (in milliseconds)
     */
    public void setElapsedWallTimeElapsed( double elapsedWallMillisSeconds ) {
        this.elapsedWallSeconds = elapsedWallMillisSeconds / 1000.0;
    }

    /**
     * @param minimumCommonQueryGenes the minimum number of query genes with which a result gene must exhibit
     *        coexpression to be displayed
     */
    public void setMinimumCommonQueries( int minimumCommonQueryGenes ) {
        this.minimumCommonQueryGenes = minimumCommonQueryGenes;
    }
}

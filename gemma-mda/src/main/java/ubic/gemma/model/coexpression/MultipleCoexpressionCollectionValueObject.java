package ubic.gemma.model.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;

/**
 * @author luke The MultipleCoexpressionCollectionValueObject is used for storing the results of a multiple coexpression search.
 */
public class MultipleCoexpressionCollectionValueObject  {

    public static final String PREDICTED_GENE_IMPL = "PredictedGeneImpl";
    public static final String PROBE_ALIGNED_REGION_IMPL = "ProbeAlignedRegionImpl";
    public static final String GENE_IMPL = "GeneImpl";
    
    private static Log log = LogFactory.getLog( MultipleCoexpressionCollectionValueObject.class.getName() );
    
    private int minimumCommonQueryGenes;
    private Map<Gene, CoexpressionCollectionValueObject> queries;
    private Map<Gene, CommonCoexpressionValueObject> geneToQueries;
    private Map<Gene, CommonCoexpressionValueObject> predictedToQueries;
    private Map<Gene, CommonCoexpressionValueObject> alignedToQueries;
    private Map<Long, Gene> geneLookup;
    
    public MultipleCoexpressionCollectionValueObject() {
        minimumCommonQueryGenes = 2;
        queries = Collections.synchronizedMap( new HashMap<Gene, CoexpressionCollectionValueObject>() );
        geneToQueries = Collections.synchronizedMap( new HashMap<Gene, CommonCoexpressionValueObject>() );
        predictedToQueries = Collections.synchronizedMap( new HashMap<Gene, CommonCoexpressionValueObject>() );
        alignedToQueries = Collections.synchronizedMap( new HashMap<Gene, CommonCoexpressionValueObject>() );
        geneLookup = Collections.synchronizedMap( new HashMap<Long, Gene>() );
    }
    
    public void addCoexpressionCollection(CoexpressionCollectionValueObject coexpressionCollection) {
        synchronized (this) {
            queries.put( coexpressionCollection.getQueryGene(), coexpressionCollection );
            for ( CoexpressionValueObject coexpressed : coexpressionCollection.getCoexpressionData() ) {
                getQueriesForGene( getGene(coexpressed) ).add( new QueryGeneCoexpressionDataPair( coexpressionCollection.getQueryGene(), coexpressed ) );
            }
            for ( CoexpressionValueObject coexpressed : coexpressionCollection.getPredictedCoexpressionData() ) {
                getQueriesForPredictedGene( getGene(coexpressed) ).add( new QueryGeneCoexpressionDataPair( coexpressionCollection.getQueryGene(), coexpressed ) );
            }
            for ( CoexpressionValueObject coexpressed : coexpressionCollection.getProbeAlignedCoexpressionData() ) {
                getQueriesForProbeAlignedRegion( getGene(coexpressed) ).add( new QueryGeneCoexpressionDataPair( coexpressionCollection.getQueryGene(), coexpressed ) );
            }
        }
    }
    
    /**
     * @return those coexpressed genes that are common to multiple query genes 
     */
    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedGenes() {
        return getCommonCoexpressedHelper(geneToQueries);
    }
    
    /**
     * @return those coexpressed predicted genes that are common to multiple query genes
     */
    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedPredictedGenes() {
        return getCommonCoexpressedHelper(predictedToQueries);
    }
    
    /**
     * @return those coexpressed probe-aligned regions that are common to multiple query genes
     */
    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedProbeAlignedRegions() {
        return getCommonCoexpressedHelper(alignedToQueries);
    }
    
    private Collection<CommonCoexpressionValueObject> getCommonCoexpressedHelper(Map<Gene, CommonCoexpressionValueObject> map) {
        Collection<CommonCoexpressionValueObject> coexpressedGenes = new ArrayList<CommonCoexpressionValueObject>();
        for ( CommonCoexpressionValueObject candidate : map.values() ) {
            if ( candidate.getCoexpressedQueryGenes().size() >= getMinimumCommonQueries() ) {
                coexpressedGenes.add( candidate );
            }
        }
        return coexpressedGenes;
    }

    /**
     * @return the minimum number of query genes with which a result gene must exhibit coexpression to be displayed
     */
    public int getMinimumCommonQueries() {
        return minimumCommonQueryGenes;
    }

    /**
     * @param minimumCommonQueryGenes the minimum number of query genes with which a result gene must exhibit coexpression to be displayed
     */
    public void setStringency( int minimumCommonQueryGenes ) {
        this.minimumCommonQueryGenes = minimumCommonQueryGenes;
    }

    /**
     * @return the CoexpressionCollectionValueObjects for all query genes
     */
    public Collection<CoexpressionCollectionValueObject> getQueryResults() {
        return queries.values();
    }
    
    /**
     * @return the query genes
     */
    public Collection<Gene> getQueryGenes() {
        return queries.keySet();
    }
    
    /**
     * @param gene the Gene of interest
     * @return a subset of the gene CommonCoexpressionValueObjects that exhibit coexpression with the specified Gene
     */
    public CommonCoexpressionValueObject getQueriesForGene(Gene gene) {
        return getQueriesForHelper(gene, geneToQueries);
    }
    
    /**
     * @param gene the Gene of interest
     * @return a subset of the predicted gene CommonCoexpressionValueObjects that exhibit coexpression with the specified Gene
     */
    public CommonCoexpressionValueObject getQueriesForPredictedGene(Gene gene) {
        return getQueriesForHelper(gene, predictedToQueries);
    }
    
    /**
     * @param gene the Gene of interest
     * @return a subset of the probe-aligned region CommonCoexpressionValueObjects that exhibit coexpression with the specified Gene
     */
    public CommonCoexpressionValueObject getQueriesForProbeAlignedRegion(Gene gene) {
        return getQueriesForHelper(gene, alignedToQueries);
    }
    
    private CommonCoexpressionValueObject getQueriesForHelper(Gene gene, Map<Gene, CommonCoexpressionValueObject> map) {
        synchronized (this) {
            CommonCoexpressionValueObject queries = geneToQueries.get( gene );
            if ( queries == null ) {
                queries = new CommonCoexpressionValueObject(gene);
                geneToQueries.put( gene, queries );
            }
            return queries;
        }
    }
    
    private Gene getGene(CoexpressionValueObject coexpressed) {
        // lookup might be unnecessary optimization; whole thing might be unnecessary if we can use load...
        // another option is to just maintain the 4 things we have in a local object until we create the CommonCoexpressionValueObject...
        Gene gene = geneLookup.get( coexpressed.getGeneId() );
        if (gene == null) {
            if ( coexpressed.getGeneType().equalsIgnoreCase( GENE_IMPL ) ) {
                gene = Gene.Factory.newInstance();
            } else if (coexpressed.getGeneType().equalsIgnoreCase( PREDICTED_GENE_IMPL ) ) {
                gene = PredictedGene.Factory.newInstance();
            } else if (coexpressed.getGeneType().equals( PROBE_ALIGNED_REGION_IMPL ) ) {
                gene = ProbeAlignedRegion.Factory.newInstance();
            } else {
                return null;
            }
            gene.setId( coexpressed.getGeneId() );
            gene.setName( coexpressed.getGeneName() );
            gene.setOfficialName( coexpressed.getGeneOfficialName() );
            geneLookup.put( coexpressed.getGeneId(), gene );
        }
        return gene;
    }
}

/**
 * 
 */
package ubic.gemma.analysis.sequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * This is a convenience object to hold a BlatResult and its associated gene products and genes
 * @author jsantos
 *
 */
public class BlatResultGeneSummary {
    private BlatResult blatResult;
    private HashMap<GeneProduct,Collection<Gene>> geneProductMap;

    public BlatResultGeneSummary() {
        geneProductMap = new HashMap<GeneProduct,Collection<Gene>>();
    }

    /**
     * @return the blatResult
     */
    public BlatResult getBlatResult() {
        return blatResult;
    }

    /**
     * @param blatResult the blatResult to set
     */
    public void setBlatResult( BlatResult blatResult ) {
        this.blatResult = blatResult;
    }

    /**
     * @return the geneProductMap
     */
    public HashMap<GeneProduct, Collection<Gene>> getGeneProductMap() {
        return geneProductMap;
    }

    /**
     * @param geneProductMap the geneProductMap to set
     */
    public void setGeneProductMap( HashMap<GeneProduct, Collection<Gene>> geneProductMap ) {
        this.geneProductMap = geneProductMap;
    }
    
    
    public Collection<GeneProduct> getGeneProducts() {
        return this.geneProductMap.keySet();
    }
    
    public Collection<Gene> getGenes(GeneProduct geneProduct) {
        return this.geneProductMap.get( geneProduct );
    }
    
    public void addGene(GeneProduct geneProduct, Gene gene) {
        if (geneProductMap.containsKey( geneProduct )) {
            geneProductMap.get( geneProduct ).add( gene );
        }
        else {
            Collection<Gene> genes = new HashSet<Gene>();
            genes.add( gene );
            geneProductMap.put( geneProduct, genes );
        }
    }
}

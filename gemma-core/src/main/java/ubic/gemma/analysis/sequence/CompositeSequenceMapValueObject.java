/**
 * 
 */
package ubic.gemma.analysis.sequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * @author jsantos
 *
 */
public class CompositeSequenceMapValueObject {
    private String compositeSequenceId = null;
    private String compositeSequenceName = null;
    private String bioSequenceId = null;
    private String bioSequenceName = null;
    private String bioSequenceNcbiId = null;
    private Long numBlatHits = null;

    private Set<GeneProductValueObject> geneProducts = new HashSet<GeneProductValueObject>();;
    private Set<GeneValueObject> genes = new HashSet<GeneValueObject>();
    
    /**
     * @return the bioSequenceId
     */
    public String getBioSequenceId() {
        return bioSequenceId;
    }
    /**
     * @param bioSequenceId the bioSequenceId to set
     */
    public void setBioSequenceId( String bioSequenceId ) {
        this.bioSequenceId = bioSequenceId;
    }
    /**
     * @return the bioSequenceNcbiId
     */
    public String getBioSequenceNcbiId() {
        return bioSequenceNcbiId;
    }
    /**
     * @param bioSequenceNcbiId the bioSequenceNcbiId to set
     */
    public void setBioSequenceNcbiId( String bioSequenceNcbiId ) {
        this.bioSequenceNcbiId = bioSequenceNcbiId;
    }
    /**
     * @return the compositeSequenceId
     */
    public String getCompositeSequenceId() {
        return compositeSequenceId;
    }
    /**
     * @param compositeSequenceId the compositeSequenceId to set
     */
    public void setCompositeSequenceId( String compositeSequenceId ) {
        this.compositeSequenceId = compositeSequenceId;
    }
    /**
     * @return the compositeSequenceName
     */
    public String getCompositeSequenceName() {
        return compositeSequenceName;
    }
    /**
     * @param compositeSequenceName the compositeSequenceName to set
     */
    public void setCompositeSequenceName( String compositeSequenceName ) {
        this.compositeSequenceName = compositeSequenceName;
    }

    /**
     * @return the numBlatHits
     */
    public Long getNumBlatHits() {
        return numBlatHits;
    }
    /**
     * @param numBlatHits the numBlatHits to set
     */
    public void setNumBlatHits( long numBlatHits ) {
        this.numBlatHits = numBlatHits;
    }
    /**
     * @return the bioSequenceName
     */
    public String getBioSequenceName() {
        return bioSequenceName;
    }
    /**
     * @param bioSequenceName the bioSequenceName to set
     */
    public void setBioSequenceName( String bioSequenceName ) {
        this.bioSequenceName = bioSequenceName;
    }
    /**
     * @return the geneProducts
     */
    public Set<GeneProductValueObject> getGeneProducts() {
        return geneProducts;
    }
    /**
     * @param geneProducts the geneProducts to set
     */
    public void setGeneProducts( Set<GeneProductValueObject> geneProducts ) {
        this.geneProducts = geneProducts;
    }
    /**
     * @return the genes
     */
    public Set<GeneValueObject> getGenes() {
        return genes;
    }
    /**
     * @param genes the genes to set
     */
    public void setGenes( Set<GeneValueObject> genes ) {
        this.genes = genes;
    }
    

}

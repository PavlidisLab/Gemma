package ubic.gemma.model.genome.sequenceAnalysis;

import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.model.genome.biosequence.BioSequence;

public class BioSequenceValueObject {

    private java.lang.Long length;
    private java.lang.String sequence;
    private java.lang.Boolean isApproximateLength;
    private java.lang.Boolean isCircular;
    private ubic.gemma.model.genome.biosequence.PolymerType polymerType;
    private ubic.gemma.model.genome.biosequence.SequenceType type;
    private java.lang.Double fractionRepeats;
    private ubic.gemma.model.common.description.DatabaseEntry sequenceDatabaseEntry;
    private TaxonValueObject taxon;
    private java.util.Collection<ubic.gemma.model.association.BioSequence2GeneProduct> bioSequence2GeneProduct = new java.util.HashSet<ubic.gemma.model.association.BioSequence2GeneProduct>();
    
    public java.lang.Long getLength()
    {
        return this.length;
    }

    public void setLength(java.lang.Long length)
    {
        this.length = length;
    }

    public java.lang.String getSequence()
    {
        return this.sequence;
    }

    public void setSequence(java.lang.String sequence)
    {
        this.sequence = sequence;
    }

    public java.lang.Boolean getIsApproximateLength()
    {
        return this.isApproximateLength;
    }

    public void setIsApproximateLength(java.lang.Boolean isApproximateLength)
    {
        this.isApproximateLength = isApproximateLength;
    }

    public java.lang.Boolean getIsCircular()
    {
        return this.isCircular;
    }

    public void setIsCircular(java.lang.Boolean isCircular)
    {
        this.isCircular = isCircular;
    }

    public ubic.gemma.model.genome.biosequence.PolymerType getPolymerType()
    {
        return this.polymerType;
    }

    public void setPolymerType(ubic.gemma.model.genome.biosequence.PolymerType polymerType)
    {
        this.polymerType = polymerType;
    }
    
    public ubic.gemma.model.genome.biosequence.SequenceType getType()
    {
        return this.type;
    }

    public void setType(ubic.gemma.model.genome.biosequence.SequenceType type)
    {
        this.type = type;
    }

    /**
     * <p>
     * The fraction of the sequences determined to be made up of
     * repeats (e.g., via repeatmasker)
     * </p>
     */
    public java.lang.Double getFractionRepeats()
    {
        return this.fractionRepeats;
    }

    public void setFractionRepeats(java.lang.Double fractionRepeats)
    {
        this.fractionRepeats = fractionRepeats;
    }

//    public ubic.gemma.model.common.description.DatabaseEntry getSequenceDatabaseEntry()
//    {
//        return this.sequenceDatabaseEntry;
//    }
//
//    public void setSequenceDatabaseEntry(ubic.gemma.model.common.description.DatabaseEntry sequenceDatabaseEntry)
//    {
//        this.sequenceDatabaseEntry = sequenceDatabaseEntry;
//    }
//
    public TaxonValueObject getTaxon()
    {
        return this.taxon;
    }

    public void setTaxon(TaxonValueObject taxon)
    {
        this.taxon = taxon;
    }
//
//    public java.util.Collection<ubic.gemma.model.association.BioSequence2GeneProduct> getBioSequence2GeneProduct()
//    {
//        return this.bioSequence2GeneProduct;
//    }
//
//    public void setBioSequence2GeneProduct(java.util.Collection<ubic.gemma.model.association.BioSequence2GeneProduct> bioSequence2GeneProduct)
//    {
//        this.bioSequence2GeneProduct = bioSequence2GeneProduct;
//    }
    
    public static BioSequenceValueObject fromEntity(BioSequence bs) {
    	BioSequenceValueObject vo = new BioSequenceValueObject();
    	vo.setFractionRepeats( bs.getFractionRepeats() );
    	vo.setIsApproximateLength( bs.getIsApproximateLength() );
    	vo.setIsCircular( bs.getIsCircular() );
    	vo.setLength( bs.getLength() );
    	vo.setPolymerType( bs.getPolymerType() );
    	vo.setSequence( bs.getSequence() );
    	vo.setType( bs.getType() );
    	
    	//bs.getDescription()    	
    	//bs.getId()    	    	
    	//bs.getName()    	
    	return vo;
    }        
	
}

package ubic.gemma.model.genome.sequenceAnalysis;

import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.model.genome.biosequence.BioSequence;

public class BioSequenceValueObject {

	private String name;
	private String description;
	private Long id;
    private java.lang.String sequence;
    private DatabaseEntryValueObject sequenceDatabaseEntry;
	private java.lang.Long length;
    private ubic.gemma.model.genome.biosequence.SequenceType type;
    private java.lang.Double fractionRepeats;
    private TaxonValueObject taxon;
    
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
    
    public ubic.gemma.model.genome.biosequence.SequenceType getType()
    {
        return this.type;
    }

    public void setType(ubic.gemma.model.genome.biosequence.SequenceType type)
    {
        this.type = type;
    }

    public java.lang.Double getFractionRepeats()
    {
        return this.fractionRepeats;
    }

    public void setFractionRepeats(java.lang.Double fractionRepeats)
    {
        this.fractionRepeats = fractionRepeats;
    }

    public DatabaseEntryValueObject getSequenceDatabaseEntry()
    {
        return this.sequenceDatabaseEntry;
    }

    public void setSequenceDatabaseEntry(DatabaseEntryValueObject sequenceDatabaseEntry)
    {
        this.sequenceDatabaseEntry = sequenceDatabaseEntry;
    }

    public TaxonValueObject getTaxon()
    {
        return this.taxon;
    }

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setTaxon(TaxonValueObject taxon)
    {
        this.taxon = taxon;
    }
    
    public static BioSequenceValueObject fromEntity(BioSequence bs) {
    	BioSequenceValueObject vo = new BioSequenceValueObject();
    	vo.setName( bs.getName() );
    	vo.setDescription( bs.getDescription() );
    	vo.setId( bs.getId() );
    	vo.setSequence( bs.getSequence() );
    	vo.setSequenceDatabaseEntry(DatabaseEntryValueObject.fromEntity(bs.getSequenceDatabaseEntry()));
    	vo.setLength( bs.getLength() );
    	vo.setType( bs.getType() );
    	vo.setFractionRepeats( bs.getFractionRepeats() );
    	vo.setTaxon(TaxonValueObject.fromEntity( bs.getTaxon() ));
    	return vo;
    }        
	
}

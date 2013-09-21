package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.biosequence.BioSequence;

public class BioSequenceValueObject {

    public static Collection<BioSequenceValueObject> fromEntities( Collection<BioSequence> bsList ) {
        Collection<BioSequenceValueObject> result = new ArrayList<BioSequenceValueObject>();
        for ( BioSequence bs : bsList ) {
            result.add( BioSequenceValueObject.fromEntity( bs ) );
        }
        return result;
    }

    public static BioSequenceValueObject fromEntity( BioSequence bs ) {
        BioSequenceValueObject vo = new BioSequenceValueObject();
        vo.setName( bs.getName() );
        vo.setDescription( bs.getDescription() );
        vo.setId( bs.getId() );
        vo.setSequence( bs.getSequence() );
        vo.setSequenceDatabaseEntry( DatabaseEntryValueObject.fromEntity( bs.getSequenceDatabaseEntry() ) );
        vo.setLength( bs.getLength() );
        vo.setType( bs.getType() );
        vo.setFractionRepeats( bs.getFractionRepeats() );
        vo.setTaxon( TaxonValueObject.fromEntity( bs.getTaxon() ) );
        return vo;
    }

    private String name;
    private String description;
    private Long id;
    private String sequence;
    private DatabaseEntryValueObject sequenceDatabaseEntry;
    private Long length;
    private ubic.gemma.model.genome.biosequence.SequenceType type;

    private Double fractionRepeats;

    private TaxonValueObject taxon;

    public String getDescription() {
        return description;
    }

    public Double getFractionRepeats() {
        return this.fractionRepeats;
    }

    public Long getId() {
        return id;
    }

    public Long getLength() {
        return this.length;
    }

    public String getName() {
        return name;
    }

    public String getSequence() {
        return this.sequence;
    }

    public DatabaseEntryValueObject getSequenceDatabaseEntry() {
        return this.sequenceDatabaseEntry;
    }

    public TaxonValueObject getTaxon() {
        return this.taxon;
    }

    public ubic.gemma.model.genome.biosequence.SequenceType getType() {
        return this.type;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setFractionRepeats( Double fractionRepeats ) {
        this.fractionRepeats = fractionRepeats;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setLength( Long length ) {
        this.length = length;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setSequence( String sequence ) {
        this.sequence = sequence;
    }

    public void setSequenceDatabaseEntry( DatabaseEntryValueObject sequenceDatabaseEntry ) {
        this.sequenceDatabaseEntry = sequenceDatabaseEntry;
    }

    public void setTaxon( TaxonValueObject taxon ) {
        this.taxon = taxon;
    }

    public void setType( ubic.gemma.model.genome.biosequence.SequenceType type ) {
        this.type = type;
    }

}

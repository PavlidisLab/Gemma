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

    private String description;

    private Double fractionRepeats;

    private Long id;
    private Long length;
    private String name;
    private String sequence;
    private DatabaseEntryValueObject sequenceDatabaseEntry;
    private TaxonValueObject taxon;
    private ubic.gemma.model.genome.biosequence.SequenceType type;

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        BioSequenceValueObject other = ( BioSequenceValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;

        if ( sequenceDatabaseEntry == null ) {
            if ( other.sequenceDatabaseEntry != null ) return false;
        } else if ( !sequenceDatabaseEntry.equals( other.sequenceDatabaseEntry ) ) return false;

        if ( name == null ) {
            if ( other.name != null ) return false;
        } else if ( !name.equals( other.name ) ) return false;

        if ( type == null ) {
            if ( other.type != null ) return false;
        } else if ( !type.equals( other.type ) ) return false;
        return true;
    }

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

    @Override
    public int hashCode() {

        if ( id != null ) return id.hashCode();
        final int prime = 31;
        int result = 1;

        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( sequenceDatabaseEntry == null ) ? 0 : sequenceDatabaseEntry.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
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

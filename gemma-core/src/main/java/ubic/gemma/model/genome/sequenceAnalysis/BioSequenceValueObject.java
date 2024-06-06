package ubic.gemma.model.genome.sequenceAnalysis;

import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class BioSequenceValueObject extends IdentifiableValueObject<BioSequence> {

    private String description;
    private Double fractionRepeats;
    private Long length;
    private String name;
    private String sequence;
    private DatabaseEntryValueObject sequenceDatabaseEntry;
    private TaxonValueObject taxon;
    private SequenceTypeValueObject type;

    /**
     * Required when using the class as a spring bean.
     */
    public BioSequenceValueObject() {
        super();
    }

    private BioSequenceValueObject( Long id ) {
        super( id );
    }

    public static Collection<BioSequenceValueObject> fromEntities( Collection<BioSequence> bsList ) {
        Collection<BioSequenceValueObject> result = new ArrayList<>();
        for ( BioSequence bs : bsList ) {
            result.add( BioSequenceValueObject.fromEntity( bs ) );
        }
        return result;
    }

    public static BioSequenceValueObject fromEntity( BioSequence bs ) {
        BioSequenceValueObject vo = new BioSequenceValueObject( bs.getId() );
        vo.setName( bs.getName() );
        vo.setDescription( bs.getDescription() );
        vo.setSequence( bs.getSequence() );
        if ( bs.getSequenceDatabaseEntry() != null ) {
            vo.setSequenceDatabaseEntry( new DatabaseEntryValueObject( bs.getSequenceDatabaseEntry() ) );
        }
        vo.setLength( bs.getLength() );
        if ( bs.getType() != null ) {
            vo.setType( new SequenceTypeValueObject( bs.getType() ) );
        }
        vo.setFractionRepeats( bs.getFractionRepeats() );
        // FIXME: BioSequence returned by the SearchService might have a null taxon
        if ( bs.getTaxon() != null ) {
            vo.setTaxon( TaxonValueObject.fromEntity( bs.getTaxon() ) );
        }
        return vo;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        BioSequenceValueObject other = ( BioSequenceValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else if ( !id.equals( other.id ) )
            return false;

        if ( sequenceDatabaseEntry == null ) {
            if ( other.sequenceDatabaseEntry != null )
                return false;
        } else if ( !sequenceDatabaseEntry.equals( other.sequenceDatabaseEntry ) )
            return false;

        if ( name == null ) {
            if ( other.name != null )
                return false;
        } else if ( !name.equals( other.name ) )
            return false;

        if ( type == null ) {
            return other.type == null;
        } else
            return type.equals( other.type );
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public Double getFractionRepeats() {
        return this.fractionRepeats;
    }

    public void setFractionRepeats( Double fractionRepeats ) {
        this.fractionRepeats = fractionRepeats;
    }

    public Long getLength() {
        return this.length;
    }

    public void setLength( Long length ) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getSequence() {
        return this.sequence;
    }

    public void setSequence( String sequence ) {
        this.sequence = sequence;
    }

    public DatabaseEntryValueObject getSequenceDatabaseEntry() {
        return this.sequenceDatabaseEntry;
    }

    public void setSequenceDatabaseEntry( DatabaseEntryValueObject sequenceDatabaseEntry ) {
        this.sequenceDatabaseEntry = sequenceDatabaseEntry;
    }

    public TaxonValueObject getTaxon() {
        return this.taxon;
    }

    public void setTaxon( TaxonValueObject taxon ) {
        this.taxon = taxon;
    }

    public SequenceTypeValueObject getType() {
        return this.type;
    }

    public void setType( SequenceTypeValueObject type ) {
        this.type = type;
    }

    @Override
    public int hashCode() {

        if ( id != null )
            return id.hashCode();
        final int prime = 31;
        int result = 1;

        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( sequenceDatabaseEntry == null ) ? 0 : sequenceDatabaseEntry.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

}

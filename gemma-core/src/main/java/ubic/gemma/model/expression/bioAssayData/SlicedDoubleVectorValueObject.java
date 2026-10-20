package ubic.gemma.model.expression.bioAssayData;

import lombok.Data;

import java.util.Objects;

@Data
public class SlicedDoubleVectorValueObject extends DoubleVectorValueObject {

    /**
     * Obtain the ID of the vector from which this slice is derived from.
     */
    private Long sourceVectorId;

    public SlicedDoubleVectorValueObject() {

    }

    private SlicedDoubleVectorValueObject( SlicedDoubleVectorValueObject other ) {
        super( other );
        this.sourceVectorId = other.sourceVectorId;
    }

    @Override
    public DoubleVectorValueObject copy() {
        return new SlicedDoubleVectorValueObject( this );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof SlicedDoubleVectorValueObject ) ) {
            return false;
        }
        SlicedDoubleVectorValueObject other = ( SlicedDoubleVectorValueObject ) obj;
        return Objects.equals( sourceVectorId, other.sourceVectorId ) && super.equals( obj );
    }

    @Override
    public int hashCode() {
        return Objects.hash( super.hashCode(), sourceVectorId );
    }

    @Override
    public String toString() {
        return super.toString() + " Source Vector Id=" + sourceVectorId;
    }
}

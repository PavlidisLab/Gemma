package ubic.gemma.model.expression.bioAssayData;

import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubsetValueObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@Data
public class SlicedDoubleVectorValueObject extends DoubleVectorValueObject {

    /**
     * Obtain the ID of the vector from which this slice is derived from.
     */
    private Long sourceVectorId;

    /**
     * Constructor for creating a slice.
     */
    public SlicedDoubleVectorValueObject( DoubleVectorValueObject vec, ExpressionExperimentSubSet bioassaySet, BioAssayDimensionValueObject slicedBad ) {
        super( vec );
        // because this is a 'slice', not a persistent one,
        setId( null );
        setExpressionExperiment( new ExpressionExperimentSubsetValueObject( bioassaySet ) );
        setBioAssayDimension( slicedBad );
        this.sourceVectorId = vec.getId(); // so we can track this!
        Collection<Double> values = new ArrayList<>();
        int i = 0;
        for ( BioAssayValueObject ba : vec.getBioAssays() ) {
            if ( this.getBioAssays().contains( ba ) ) {
                values.add( vec.getData()[i] );
            }
            i++;
        }
        setData( ArrayUtils.toPrimitive( values.toArray( new Double[] {} ) ) );
    }

    protected SlicedDoubleVectorValueObject( SlicedDoubleVectorValueObject other ) {
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

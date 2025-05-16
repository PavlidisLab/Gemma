package ubic.gemma.model.expression.bioAssayData;

import lombok.Data;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubsetValueObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public SlicedDoubleVectorValueObject( DoubleVectorValueObject vec, ExpressionExperimentSubsetValueObject bioassaySet, BioAssayDimensionValueObject slicedBad ) {
        super( vec );
        Assert.isTrue( bioassaySet.getSourceExperimentId().equals( vec.getExpressionExperiment().getId() ),
                "The subset must belong to " + vec.getExpressionExperiment() + "." );
        Map<BioAssayValueObject, Integer> bioAssayIndexMap = new HashMap<>( vec.getBioAssays().size() );
        for ( int i = 0; i < vec.getBioAssays().size(); i++ ) {
            bioAssayIndexMap.put( vec.getBioAssays().get( i ), i );
        }
        Assert.isTrue( bioAssayIndexMap.keySet().containsAll( slicedBad.getBioAssays() ),
                "The sliced BAD must be a subset of its original BAD." );
        // because this is a 'slice', not a persistent one,
        setId( null );
        setExpressionExperiment( bioassaySet );
        setBioAssayDimension( slicedBad );
        this.sourceVectorId = vec.getId(); // so we can track this!
        double[] slicedData = new double[slicedBad.getBioAssays().size()];
        List<BioAssayValueObject> bioAssays = slicedBad.getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            slicedData[i] = vec.getData()[bioAssayIndexMap.get( bioAssays.get( i ) )];
        }
        setData( slicedData );
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

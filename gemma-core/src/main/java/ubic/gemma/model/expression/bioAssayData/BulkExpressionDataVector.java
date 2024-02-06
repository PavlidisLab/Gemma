package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * A data vector storing bulk expression data.
 * @author poirigui
 */
@Getter
@Setter
public abstract class BulkExpressionDataVector extends DesignElementDataVector {

    /**
     * A dimension of {@link ubic.gemma.model.expression.bioAssay.BioAssay} the elements of this vector apply to.
     */
    private BioAssayDimension bioAssayDimension;

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BulkExpressionDataVector ) )
            return false;
        BulkExpressionDataVector other = ( BulkExpressionDataVector ) object;
        if ( getId() != null && other.getId() != null ) {
            return Objects.equals( getId(), other.getId() );
        }
        return super.equals( object )
                && Objects.equals( bioAssayDimension, other.bioAssayDimension );
    }

    @Override
    public int hashCode() {
        return Objects.hash( super.hashCode(), bioAssayDimension );
    }

    @Override
    public String toString() {
        return String.format( "%s%s%s%s%s%s%s", this.getClass().getSimpleName(),
                this.getId() != null ? " Id=" + this.getId() : "",
                this.getDesignElement() != null ? " DE=" + this.getDesignElement().getName() : "",
                this.getExpressionExperiment() != null ? " EE=" + this.getExpressionExperiment().getName() : "",
                this.getQuantitationType() != null ? " QT=" + this.getQuantitationType().getName() : "",
                this.getBioAssayDimension() != null ? " BAD=" + this.getBioAssayDimension().getName() : "",
                this.getData() != null ? ", " + this.getData().length + " bytes" : "" );
    }
}

package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

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

    /**
     * Bulk data vectors are never mapped from an external source, so this is always null.
     */
    @Nullable
    @Override
    public String getOriginalDesignElement() {
        return null;
    }

    @Override
    public String toString() {
        return String.format( "%s%s%s%s%s%s%s", this.getClass().getSimpleName(),
                this.getId() != null ? " Id=" + this.getId() : "",
                this.getDesignElement() != null ? " DE=" + this.getDesignElement().getName() : "",
                // the EE is lazily initialized, so only the ID is safe to use
                this.getExpressionExperiment() != null ? " EE=" + this.getExpressionExperiment().getId() : "",
                this.getQuantitationType() != null ? " QT=" + this.getQuantitationType().getName() : "",
                this.getBioAssayDimension() != null ? " BAD=" + this.getBioAssayDimension().getId() : "",
                this.getData() != null ? ", " + this.getData().length + " bytes" : "" );
    }
}

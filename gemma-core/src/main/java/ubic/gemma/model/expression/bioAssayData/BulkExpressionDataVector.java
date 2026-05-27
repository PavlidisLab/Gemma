package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import org.springframework.lang.Nullable;

/**
 * A data vector storing bulk expression data.
 *
 * @author poirigui
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BulkExpressionDataVector extends DesignElementDataVector {

    /**
     * A dimension of {@link ubic.gemma.model.expression.bioAssay.BioAssay} the elements of this vector apply to.
     */
    // Flipped to LAZY in the hbm (lazy="proxy") to dodge an N+1 on bulk vector loads. Hot loaders
    // (RawExpressionDataVectorDaoImpl, ProcessedExpressionDataVectorDaoImpl) JOIN FETCH the BAD
    // where the caller needs it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BIO_ASSAY_DIMENSION_FK", nullable = false, columnDefinition = "BIGINT")
    private BioAssayDimension bioAssayDimension;

    /**
     * Obtain the number of cells that were used to compute each value in this data vector, or {@code null} if now
     * known/available.
     */
    @Nullable
    public abstract int[] getNumberOfCells();

    public abstract void setNumberOfCells( @Nullable int[] numberOfCells );

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

package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.persistence.hibernate.ByteArrayType;

import java.util.Arrays;
import java.util.Objects;

/**
 * An expression data vector that contains data at the resolution of a single cell.
 * <p>
 * This is achieved by storing cell metadata such as IDs and cell types in a {@link SingleCellDimension} that is shared
 * among all vectors of a given {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} and individual
 * non-zero cell expression in a sparse data structure similar to the rows of a CSR matrix.
 * @author poirigui
 */
@Getter
@Setter
public class SingleCellExpressionDataVector extends DesignElementDataVector {

    /**
     * The dimension of the single cell data which is shared among all the vectors.
     * <p>
     * This is shared among all the single-cell vectors of the associated {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}.
     */
    private SingleCellDimension singleCellDimension;

    /**
     * Positions of the non-zero data in the {@link #getData()} vector.
     * <p>
     * This is mapped in the database using {@link ByteArrayType}.
     */
    private int[] dataIndices;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SingleCellExpressionDataVector ) ) {
            return false;
        }
        SingleCellExpressionDataVector other = ( SingleCellExpressionDataVector ) object;
        if ( getId() != null && other.getId() != null ) {
            return Objects.equals( getId(), other.getId() );
        }
        return super.equals( object )
                && Objects.equals( singleCellDimension, other.singleCellDimension )
                && Arrays.equals( dataIndices, other.dataIndices );
    }

    @Override
    public int hashCode() {
        return Objects.hash( super.hashCode(), singleCellDimension );
    }

    @Override
    public String toString() {
        return String.format( "%s%s%s%s%s%s%s", this.getClass().getSimpleName(),
                this.getId() != null ? " Id=" + this.getId() : "",
                this.getDesignElement() != null ? " DE=" + this.getDesignElement().getName() : "",
                this.getExpressionExperiment() != null ? " EE=" + this.getExpressionExperiment().getName() : "",
                this.getQuantitationType() != null ? " QT=" + this.getQuantitationType().getName() : "",
                this.getSingleCellDimension() != null ? " SCD=" + this.getSingleCellDimension().getId() : "",
                this.getData() != null ? ", " + this.getData().length + " bytes" : "" );
    }
}

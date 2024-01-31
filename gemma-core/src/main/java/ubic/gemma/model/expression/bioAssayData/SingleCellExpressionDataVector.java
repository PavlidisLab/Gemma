package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.persistence.hibernate.IntArrayType;

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
     * This is mapped in the database using {@link IntArrayType}.
     */
    private int[] dataIndices;
}

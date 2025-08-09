package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base interface for design matrices used in experiments.
 * @author poirigui
 */
public interface DesignMatrix {

    /**
     * Return the number of columns (i.e. factors) in the design matrix.
     */
    int columns();

    /**
     * Obtain a list of factors in the design matrix.
     */
    List<ExperimentalFactor> getFactors();

    /**
     * Obtain the factor for a given column.
     * @throws IndexOutOfBoundsException if the column index is out of bounds.
     */
    ExperimentalFactor getFactorForColumn( int column );

    /**
     * Obtain the factor values for a given column.
     * @throws IndexOutOfBoundsException if the column index is out of bounds.
     */
    List<FactorValue> getColumn( int column );

    /**
     * Obtain the factor values for a given experimental factor.
     * @return the factor values, or null if the factor is not present in the design matrix.
     */
    @Nullable
    List<FactorValue> getColumn( ExperimentalFactor factor );

    /**
     * Obtain the index of a given factor in the design matrix.
     */
    int getColumnIndex( ExperimentalFactor factor );

    /**
     * Return the number of rows (i.e. samples) in the design matrix.
     */
    int rows();

    List<BioAssay> getBioAssays();

    /**
     * Obtain the factor values for a given row (sample).
     * @throws IndexOutOfBoundsException if the row index is out of bounds.
     */
    List<FactorValue> getRow( int row );

    /**
     * Obtain the assay for a given row.
     * @throws IndexOutOfBoundsException if the row index is out of bounds.
     */
    BioAssay getBioAssayForRow( int row );

    /**
     * Obtain the sample for a given row.
     * @throws IndexOutOfBoundsException if the row index is out of bounds.
     */
    BioMaterial getBioMaterialForRow( int row );
}

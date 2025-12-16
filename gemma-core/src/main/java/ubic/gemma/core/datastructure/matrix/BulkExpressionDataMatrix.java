package ubic.gemma.core.datastructure.matrix;

import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Interface for bulk expression data matrices.
 * <p>
 * In a bulk expression data matrix, each column represents a sample.
 * <p>
 * Expression data is rather complex, so we have to handle some messy cases.
 * <p>
 * The key problem is how to unambiguously identify rows and columns in the matrix. This is greatly complicated by the
 * fact that experiments can combine data from multiple array designs in various ways.
 * <p>
 * Put it together, and the result is that there can be more than one {@link BioAssay} per column; the same {@link BioMaterial}
 * can be used in multiple columns (supported implicitly). There can also be more than on BioMaterial in one column
 * (we don't support this yet either). The same {@link BioSequence} can be found in multiple rows. A row can contain
 * data from more than one {@link CompositeSequence}. These cases are handled by the {@link MultiAssayBulkExpressionDataMatrix}
 * interface and their corresponding implementations. This interface assumes the simplest case where each column is
 * represented by a {@link BioAssay} and each row is represented by a {@link CompositeSequence}.
 * <p>
 * There are a few constraints: a particular {@link CompositeSequence} can only be used once, in a single row. At the
 * moment we do not directly support technical replicates, though this should be possible. A {@link BioAssay} can only
 * appear in a single column.
 * <p>
 * For some operations a {@link ExpressionDataMatrixRowElement} object is offered, which encapsulates a combination of
 * {@link CompositeSequence}, a {@link BioSequence}, and an index. The list of these can be useful for iterating over
 * the rows of the matrix.
 *
 * @author pavlidis
 * @author keshav
 * @see BioAssayDimension
 * @see BulkExpressionDataVector
 * @see MultiAssayBulkExpressionDataMatrix
 */
public interface BulkExpressionDataMatrix<T> extends ExpressionDataMatrix<T> {

    /**
     * Create a bulk expression data matrix from a collection of vectors. All vectors must share the same {@link QuantitationType}.
     */
    static BulkExpressionDataMatrix<?> getMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        Assert.isTrue( !vectors.isEmpty(), "There must be at least one vector." );
        PrimitiveType repr = vectors.iterator().next().getQuantitationType().getRepresentation();
        switch ( repr ) {
            case DOUBLE:
                return new BulkExpressionDataDoubleMatrix( new ArrayList<>( vectors ) );
            case INT:
                return new BulkExpressionDataIntMatrix( new ArrayList<>( vectors ) );
            default:
                throw new UnsupportedOperationException( "Bulk data matrix of of " + repr + " is not supported." );
        }
    }

    /**
     * Obtain the dimension for the columns of this matrix.
     */
    BioAssayDimension getBioAssayDimension();

    /**
     * @return true if any values are null or NaN (for doubles and floats); any other value that is considered missing.
     */
    boolean hasMissingValues();

    /**
     * Access a single value of the matrix. Note that because there can be multiple bioassays per column and multiple
     * design elements per row, it is possible for this method to retrieve a data that does not come from the bioassay
     * and/or designelement arguments.
     *
     * @param designElement de
     * @param bioAssay      ba
     * @return the value at the given design element and bioassay, or {@code null} if the value is missing
     */
    @Nullable
    T get( CompositeSequence designElement, BioAssay bioAssay );

    /**
     * Access the entire matrix.
     *
     * @return T[][]
     */
    T[][] getMatrix();

    List<BioMaterial> getBioMaterials();

    /**
     * Access a single column of the matrix.
     *
     * @return a vector for the given column, or null if the column is not present
     */
    @Nullable
    T[] getColumn( BioAssay bioAssay );

    /**
     * @return the index of the column for the data for the bioAssay, or -1 if missing
     */
    int getColumnIndex( BioAssay bioAssay );

    int getColumnIndex( BioMaterial bioMaterial );

    /**
     * Slice the requested samples (columns) from this matrix.
     * <p>
     * Dimensions will be altered to reflect only the selected samples.
     *
     * @param bioMaterials samples to select from the matrix
     * @throws IllegalArgumentException if any of the requested biomaterial are not found in the matrix
     */
    BulkExpressionDataMatrix<T> sliceColumns( List<BioMaterial> bioMaterials );

    /**
     * Slice the requested samples (columns) from this matrix.
     * <p>
     * This also allows specifying a new dimension for the columns that will be used for every design element (rows).
     *
     * @param bioMaterials samples to select from the matrix
     * @param dimension    the dimension to use
     * @throws IllegalArgumentException if any of the requested biomaterial are not found in the matrix
     */
    BulkExpressionDataMatrix<T> sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension );

    /**
     * Obtain an assay corresponding to a given column.
     */
    BioAssay getBioAssayForColumn( int index );

    /**
     * Obtain a biomaterial corresponding to a column.
     */
    BioMaterial getBioMaterialForColumn( int index );
}

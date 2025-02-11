package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

import javax.annotation.Nullable;
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
 * data from more than one {@link CompositeSequence}.
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
 */
public interface BulkExpressionDataMatrix<T> extends ExpressionDataMatrix<T> {

    /**
     * The experiment this matrix is associated with, if known.
     */
    @Nullable
    @Override
    ExpressionExperiment getExpressionExperiment();

    /**
     * Return the quantitation types for this matrix. Often (usually) there will be just one.
     *
     * @return qts
     */
    Collection<QuantitationType> getQuantitationTypes();

    /**
     * Obtain the single quantitation type for this matrix.
     * @throws IllegalStateException if there is more than one quantitation type.
     */
    QuantitationType getQuantitationType();

    /**
     * @return a {@link BioAssayDimension} that covers all the biomaterials in this matrix.
     * @throws IllegalStateException if there isn't a single bioassaydimension that encapsulates all the biomaterials
     *                               used in the experiment.
     */
    BioAssayDimension getBestBioAssayDimension();

    /**
     * @return true if any values are null or NaN (for Doubles); all other values are considered non-missing.
     */
    boolean hasMissingValues();

    /**
     * Access a single value of the matrix. Note that because there can be multiple bioassays per column and multiple
     * designelements per row, it is possible for this method to retrieve a data that does not come from the bioassay
     * and/or designelement arguments.
     *
     * @param designElement de
     * @param bioAssay      ba
     * @return T t
     */
    T get( CompositeSequence designElement, BioAssay bioAssay );

    /**
     * Access a submatrix
     *
     * @param designElements de
     * @param bioAssays      bas
     * @return T[][]
     */
    T[][] get( List<CompositeSequence> designElements, List<BioAssay> bioAssays );

    /**
     * Access the entire matrix.
     *
     * @return T[][]
     */
    T[][] getRawMatrix();

    /**
     * Access a single column of the matrix.
     *
     * @param bioAssay i
     * @return T[]
     */
    T[] getColumn( BioAssay bioAssay );

    /**
     * Access a submatrix slice by columns
     *
     * @param bioAssays ba
     * @return t[][]
     */
    T[][] getColumns( List<BioAssay> bioAssays );

    /**
     * Number of columns that use the given design element. Useful if the matrix includes data from more than one array
     * design.
     *
     * @param el el
     * @return int
     */
    int columns( CompositeSequence el );

    /**
     * @param index i
     * @return BioMaterial. Note that if this represents a subsetted data set, the BioMaterial may be a lightweight
     * 'fake'.
     */
    BioMaterial getBioMaterialForColumn( int index );

    /**
     * @param bioMaterial bm
     * @return the index of the column for the data for the bioMaterial, or -1 if missing
     */
    int getColumnIndex( BioMaterial bioMaterial );

    /**
     * @return the index of the column for the data for the bioAssay, or -1 if missing
     */
    int getColumnIndex( BioAssay bioAssay );

    /**
     * Produce a BioAssayDimension representing the matrix columns for a specific row. The designelement argument is
     * needed because a matrix can combine data from multiple array designs, each of which will generate its own
     * bioassaydimension. Note that if this represents a subsetted data set, the return value may be a lightweight
     * 'fake'.
     *
     * @param designElement de
     * @return bad
     */
    BioAssayDimension getBioAssayDimension( CompositeSequence designElement );

    /**
     * @param index i
     * @return bioassays that contribute data to the column. There can be multiple bioassays if more than one array was
     * used in the study.
     */
    Collection<BioAssay> getBioAssaysForColumn( int index );

    /**
     * Obtain a single assay for a column.
     * @param index
     * @return
     * @throws IllegalStateException if there is more than one assay for the column.
     */
    BioAssay getBioAssayForColumn( int index );

    /**
     * Set a value in the matrix, by index
     *
     * @param row    row
     * @param column col
     * @param value  val
     */
    void set( int row, int column, T value );
}

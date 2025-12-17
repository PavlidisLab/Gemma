package ubic.gemma.core.datastructure.matrix;

import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

/**
 * An interface for bulk matrices that supports multiple {@link BioAssay} per {@link BioMaterial}.
 * <p>
 * This is usually achieved by stacking data matrices from multiple {@link QuantitationType}. Thus, this interface
 * allows you to keep track of {@link QuantitationType} and {@link BioAssayDimension} at row-level. Data held in the
 * matrix is always reordered to match the sample ordering from {@link #getBioMaterials()}.
 *
 * @param <T>
 */
public interface MultiAssayBulkExpressionDataMatrix<T> extends BulkExpressionDataMatrix<T> {

    /**
     * Create a matrix using all the vectors, which are assumed to share the same representation.
     *
     * @param vectors raw vectors
     * @return matrix of appropriate type.
     */
    static MultiAssayBulkExpressionDataMatrix<?> getMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        Assert.isTrue( !vectors.isEmpty(), "No vectors." );
        ExpressionExperiment ee = vectors.iterator().next().getExpressionExperiment();
        PrimitiveType representation = vectors.iterator().next().getQuantitationType().getRepresentation();
        switch ( representation ) {
            case DOUBLE:
                return new ExpressionDataDoubleMatrix( ee, vectors );
            case STRING:
                return new ExpressionDataStringMatrix( ee, vectors );
            case INT:
                return new ExpressionDataIntegerMatrix( ee, vectors );
            case BOOLEAN:
                return new ExpressionDataBooleanMatrix( ee, vectors );
            default:
                throw new UnsupportedOperationException( "Don't know how to deal with matrices of " + representation + "." );
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * In the case of multi-assay matrices, more than one quantitation type may be present. When possible, those are
     * merged with {@link QuantitationTypeUtils#mergeQuantitationTypes(Collection)}.
     *
     * @throws IllegalStateException if the matrix has more than one quantitation type that cannot be combined
     */
    @Override
    QuantitationType getQuantitationType();

    /**
     * Return the quantitation types for this matrix. Often (usually) there will be just one.
     */
    Collection<QuantitationType> getQuantitationTypes();

    /**
     * Return the quantitation type used for data from the given design element.
     *
     * @return the quantitation type applicable for the row or {@code null} if the design element is not present in the
     * matrix
     */
    @Nullable
    QuantitationType getQuantitationType( CompositeSequence designElement );

    /**
     * Obtain all the {@link BioAssayDimension}s that are used in this matrix.
     */
    Collection<BioAssayDimension> getBioAssayDimensions();

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if no {@link BioAssayDimension} covers all the biomaterials in this matrix
     */
    @Override
    BioAssayDimension getBioAssayDimension();

    /**
     * Obtain the largest {@link BioAssayDimension} that covers all the biomaterials in this matrix.
     *
     * @return the best {@link BioAssayDimension} for this matrix, or {@link Optional#empty()} if no such dimension
     * exists
     */
    Optional<BioAssayDimension> getBestBioAssayDimension();

    /**
     * Produce a BioAssayDimension representing the matrix columns for a specific row. The designelement argument is
     * needed because a matrix can combine data from multiple array designs, each of which will generate its own
     * bioassaydimension. Note that if this represents a subsetted data set, the return value may be a lightweight
     * 'fake'.
     *
     * @param designElement de
     * @return the dimension applicable to the design element or {@code null} if the design element is not present in
     * the matrix
     */
    @Nullable
    BioAssayDimension getBioAssayDimension( CompositeSequence designElement );

    /**
     * @param index i
     * @return bioassays that contribute data to the column. There can be multiple bioassays if more than one array was
     * used in the study.
     */
    Collection<BioAssay> getBioAssaysForColumn( int index );

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
}

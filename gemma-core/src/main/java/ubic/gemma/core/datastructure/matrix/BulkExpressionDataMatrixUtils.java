package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class BulkExpressionDataMatrixUtils {

    /**
     * Convert a {@link BulkExpressionDataMatrix} to a collection of {@link BulkExpressionDataVector}.
     */
    public static <T extends BulkExpressionDataVector> List<T> toVectors( BulkExpressionDataMatrix<?> matrix, Class<T> vectorType ) {
        List<T> result = new ArrayList<>( matrix.rows() );
        ExpressionExperiment ee = requireNonNull( matrix.getExpressionExperiment(),
                "A matrix must have an ExpressionExperiment to be converted to a collection of vectors." );
        QuantitationType qt = matrix.getQuantitationType();
        BioAssayDimension bad = matrix.getBioAssayDimension();
        for ( int i = 0; i < matrix.rows(); i++ ) {
            T v = createVector( vectorType );
            v.setExpressionExperiment( ee );
            v.setBioAssayDimension( bad );
            v.setDesignElement( matrix.getDesignElementForRow( i ) );
            v.setQuantitationType( qt );
            setData( v, matrix, i );
            setNumberOfCells( v, matrix, i );
            // we don't fill in the ranks because we only have the mean value here.
            result.add( v );
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends BulkExpressionDataVector> T createVector( Class<T> vectorType ) {
        if ( vectorType == RawExpressionDataVector.class ) {
            return ( T ) new RawExpressionDataVector();
        } else if ( vectorType == ProcessedExpressionDataVector.class ) {
            return ( T ) new ProcessedExpressionDataVector();
        } else {
            throw new UnsupportedOperationException( "Cannot create vector of type " + vectorType.getName() + "." );
        }
    }

    private static void setData( BulkExpressionDataVector vector, BulkExpressionDataMatrix<?> matrix, int i ) {
        if ( matrix instanceof BulkExpressionDataPrimitiveDoubleMatrix ) {
            vector.setDataAsDoubles( ( ( BulkExpressionDataPrimitiveDoubleMatrix ) matrix ).getRowAsDoubles( i ) );
        } else if ( matrix instanceof BulkExpressionDataPrimitiveIntMatrix ) {
            vector.setDataAsInts( ( ( BulkExpressionDataPrimitiveIntMatrix ) matrix ).getRowAsInts( i ) );
        } else {
            vector.setDataAsObjects( matrix.getRow( i ) );
        }
    }

    private static void setNumberOfCells( BulkExpressionDataVector v, BulkExpressionDataMatrix<?> matrix, int i ) {
        if ( matrix instanceof SingleCellDerivedBulkExpressionDataMatrix ) {
            v.setNumberOfCells( ( ( SingleCellDerivedBulkExpressionDataMatrix<?> ) matrix ).getNumberOfCellsForRow( i ) );
        }
    }
}

package ubic.gemma.core.datastructure.matrix;

import no.uib.cipr.matrix.sparse.CompRowMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SingleCellExpressionDataMatrixUtils {

    public static List<SingleCellExpressionDataVector> toVectors( SingleCellExpressionDataMatrix<?> matrix ) {
        List<SingleCellExpressionDataVector> vectors = new ArrayList<>( matrix.rows() );
        ExpressionExperiment ee = matrix.getExpressionExperiment();
        QuantitationType qt = matrix.getQuantitationType();
        SingleCellDimension scd = matrix.getSingleCellDimension();
        for ( int i = 0; i < matrix.rows(); i++ ) {
            SingleCellExpressionDataVector vec = new SingleCellExpressionDataVector();
            vec.setExpressionExperiment( ee );
            vec.setSingleCellDimension( scd );
            vec.setQuantitationType( qt );
            vec.setDesignElement( matrix.getDesignElementForRow( i ) );
            setDataAndIndices( vec, matrix, i );
            vectors.add( vec );
        }
        return vectors;
    }

    private static void setDataAndIndices( SingleCellExpressionDataVector vec, SingleCellExpressionDataMatrix<?> matrix, int i ) {
        CompRowMatrix mat;
        if ( matrix instanceof SingleCellExpressionDataDoubleMatrix ) {
            mat = ( ( SingleCellExpressionDataDoubleMatrix ) matrix ).getMatrix();
            vec.setDataAsDoubles( Arrays.copyOfRange( mat.getData(), mat.getRowPointers()[i], mat.getRowPointers()[i + 1] ) );
            vec.setDataIndices( Arrays.copyOfRange( mat.getColumnIndices(), mat.getRowPointers()[i], mat.getRowPointers()[i + 1] ) );
        } else if ( matrix instanceof SingleCellExpressionDataIntMatrix ) {
            mat = ( ( SingleCellExpressionDataIntMatrix ) matrix ).getMatrix();
            int nnz = mat.getRowPointers()[i + 1] - mat.getRowPointers()[i];
            int[] data = new int[nnz];
            for ( int j = mat.getRowPointers()[i]; j < mat.getRowPointers()[i + 1]; j++ ) {
                data[j - mat.getRowPointers()[i]] = ( int ) Math.rint( mat.getData()[j] );
            }
            vec.setDataAsInts( data );
            vec.setDataIndices( Arrays.copyOfRange( mat.getColumnIndices(), mat.getRowPointers()[i], mat.getRowPointers()[i + 1] ) );
        } else {
            Object[] row = matrix.getRow( i );
            int nnz = 0;
            Object defaultValue = QuantitationTypeUtils.getDefaultValue( vec.getQuantitationType() );
            for ( Object elem : row ) {
                if ( !Objects.equals( elem, defaultValue ) ) {
                    nnz++;
                }
            }
            Object[] data = new Object[nnz];
            int[] dataIndices = new int[nnz];
            int k = 0;
            for ( int j = 0; j < row.length; j++ ) {
                if ( !Objects.equals( row[k], defaultValue ) ) {
                    data[k] = row[k];
                    dataIndices[k] = j;
                    k++;
                }
            }
            vec.setDataAsObjects( data );
            vec.setDataIndices( dataIndices );
        }
    }
}

package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A bulk expression data matrix that can be efficiently accessed as a primitive int matrix.
 *
 * @author poirigui
 */
public class BulkExpressionDataIntMatrix extends AbstractBulkExpressionDataMatrix<Integer> implements BulkExpressionDataPrimitiveIntMatrix {

    private final int[][] matrix;

    public BulkExpressionDataIntMatrix( List<BulkExpressionDataVector> vectors ) {
        super( vectors );
        this.matrix = new int[vectors.size()][];
        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vector = vectors.get( i );
            this.matrix[i] = vector.getDataAsInts();
        }
    }

    private BulkExpressionDataIntMatrix( @Nullable ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, QuantitationType quantitationType, List<CompositeSequence> designElements, int[][] matrix ) {
        super( expressionExperiment, bioAssayDimension, quantitationType, designElements );
        Assert.isTrue( matrix.length == 0 || bioAssayDimension.getBioAssays().size() == matrix[0].length,
                "Number of bioassays must match the number of columns in the matrix." );
        Assert.isTrue( designElements.size() == matrix.length,
                "Number of design elements must match the number of rows in the matrix." );
        this.matrix = matrix;
    }

    @Override
    public boolean hasMissingValues() {
        return false;
    }

    @Override
    public Integer[][] getRawMatrix() {
        Integer[][] result = new Integer[matrix.length][];
        for ( int i = 0; i < matrix.length; i++ ) {
            result[i] = ArrayUtils.toObject( matrix[i] );
        }
        return result;
    }

    @Override
    public BulkExpressionDataMatrix<Integer> sliceColumns( List<BioMaterial> bioMaterials ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BulkExpressionDataIntMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[][] getRawMatrixAsInts() {
        return matrix;
    }

    @Override
    public Integer[] getColumn( int column ) {
        return ArrayUtils.toObject( getColumnAsInts( column ) );
    }

    @Override
    public int[] getColumnAsInts( int column ) {
        int[] col = new int[rows()];
        for ( int i = 0; i < rows(); i++ ) {
            col[i] = matrix[i][column];
        }
        return col;
    }

    @Override
    public int[] getColumnAsInts( BioAssay bioAssay ) {
        int column = getColumnIndex( bioAssay );
        if ( column == -1 ) {
            return null;
        }
        return getColumnAsInts( column );
    }

    @Override
    public Integer[] getRow( int index ) {
        return ArrayUtils.toObject( getRowAsInts( index ) );
    }

    @Override
    public BulkExpressionDataIntMatrix sliceRows( List<CompositeSequence> designElements ) {
        int[][] slicedMatrix = new int[designElements.size()][];
        for ( int i = 0; i < designElements.size(); i++ ) {
            CompositeSequence de = designElements.get( i );
            int rowIndex = getRowIndex( de );
            if ( rowIndex == -1 ) {
                throw new IllegalArgumentException( de + " is not found in the matrix." );
            }
            // this is immutable, so slicing rows is very efficient :)
            slicedMatrix[i] = matrix[rowIndex];
        }
        return new BulkExpressionDataIntMatrix( getExpressionExperiment(), getBioAssayDimension(),
                getQuantitationType(), designElements, slicedMatrix );
    }

    @Override
    public int[] getRowAsInts( int index ) {
        return matrix[index];
    }

    @Override
    public int[] getRowAsInts( CompositeSequence designElement ) {
        int index = getRowIndex( designElement );
        if ( index == -1 ) {
            return null;
        }
        return matrix[index];
    }

    @Override
    public Integer get( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    public int getAsInt( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    protected String format( int i, int j ) {
        return format( matrix[i][j] );
    }
}

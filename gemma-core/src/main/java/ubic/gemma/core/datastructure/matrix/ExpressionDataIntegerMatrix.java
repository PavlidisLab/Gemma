package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.List;

/**
 * Warning, not fully tested.
 *
 * @author pavlidis
 */
public class ExpressionDataIntegerMatrix extends AbstractMultiAssayExpressionDataMatrix<Integer> {

    private static final Log log = LogFactory.getLog( ExpressionDataIntegerMatrix.class.getName() );

    private final Integer[][] matrix;
    private final boolean hasMissingValues;

    public ExpressionDataIntegerMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        this.matrix = this.createMatrix( selectVectors( vectors ) );
        this.hasMissingValues = checkMissingValues( this.matrix );
    }

    @Override
    public Integer get( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    public Integer[] getColumn( int column ) {
        if ( column < 0 || column >= this.columns() ) {
            throw new IndexOutOfBoundsException( "Column index " + column + " is out of bounds. Matrix has " + this.columns() + " columns." );
        }
        Integer[] result = new Integer[rows()];
        for ( int i = 0; i < result.length; i++ ) {
            result[i] = matrix[i][column];
        }
        return result;
    }

    @Override
    public Integer[][] getMatrix() {
        return matrix;
    }

    @Override
    public BulkExpressionDataMatrix<Integer> sliceColumns( List<BioMaterial> bioMaterials ) {
        throw new UnsupportedOperationException( "Slicing multi-assay integer matrices is not supported" );
    }

    @Override
    public ExpressionDataIntegerMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        throw new UnsupportedOperationException( "Slicing multi-assay integer matrices is not supported" );
    }

    @Override
    public Integer[] getRow( int index ) {
        return this.matrix[index];
    }

    @Override
    public ExpressionDataMatrix<Integer> sliceRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException( "Slicing multi-assay integer matrices is not supported." );
    }

    @Override
    public boolean hasMissingValues() {
        return hasMissingValues;
    }

    @Override
    protected String format( int row, int column ) {
        return matrix[row][column] != null ? String.valueOf( matrix[row][column] ) : "";
    }

    /**
     * Fill in the data
     *
     * @return DoubleMatrixNamed
     */
    private Integer[][] createMatrix( List<? extends BulkExpressionDataVector> vectors ) {
        int numRows = rows();
        int numCols = columns();

        // missing values will be filled with NaNs
        Integer[][] mat = new Integer[numRows][numCols];

        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vector = vectors.get( i );
            int[] vals = vector.getDataAsInts();
            BioAssayDimension dimension = vector.getBioAssayDimension();
            List<BioAssay> bioAssays = dimension.getBioAssays();
            assert bioAssays.size() == vals.length : "Expected " + vals.length + " got " + bioAssays.size();
            for ( int j = 0; j < bioAssays.size(); j++ ) {
                BioAssay bioAssay = bioAssays.get( j );
                int column = getColumnIndex( bioAssay );
                assert column != -1;
                mat[i][column] = vals[j];
            }
        }

        ExpressionDataIntegerMatrix.log.debug( "Created a " + rows() + " x " + columns() + " matrix" );
        return mat;
    }

    private boolean checkMissingValues( Integer[][] mat ) {
        for ( Integer[] vec : mat ) {
            for ( Integer val : vec ) {
                if ( val == null ) {
                    return true;
                }
            }
        }
        return false;
    }
}

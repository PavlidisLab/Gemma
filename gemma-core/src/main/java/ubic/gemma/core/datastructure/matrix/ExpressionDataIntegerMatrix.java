package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.IntegerMatrix;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Warning, not fully tested.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class ExpressionDataIntegerMatrix extends AbstractMultiAssayExpressionDataMatrix<Integer> {

    private static final Log log = LogFactory.getLog( ExpressionDataIntegerMatrix.class.getName() );

    private IntegerMatrix<CompositeSequence, Integer> matrix;

    public ExpressionDataIntegerMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        for ( DesignElementDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.INT ) ) {
                throw new IllegalStateException( "Cannot convert non-integer quantitation types into int matrix" );
            }
        }
        this.selectVectors( vectors );
        this.vectorsToMatrix( vectors );
    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    @Override
    public Integer get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public Integer[] getColumn( int index ) {
        return this.matrix.getColumn( index );
    }

    @Override
    public Integer[][] getRawMatrix() {
        Integer[][] res = new Integer[this.rows()][];
        for ( int i = 0; i < this.rows(); i++ ) {
            res[i] = this.matrix.getRow( i );
        }
        return res;
    }

    @Override
    public Integer[] getRow( int index ) {
        return this.matrix.getRow( index );
    }

    @Override
    public boolean hasMissingValues() {
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                // only null values count as missing since there's no NAN for Integers.
                if ( matrix.get( i, j ) == null )
                    return true;
            }
        }
        return false;
    }

    @Override
    public int rows() {
        return matrix.rows();
    }

    public Integer get( CompositeSequence designElement, BioMaterial bioMaterial ) {
        return this.matrix.get( matrix.getRowIndexByName( designElement ),
                matrix.getColIndexByName( this.columnBioMaterialMap.get( bioMaterial ) ) );
    }

    @Override
    protected String format( int row, int column ) {
        return String.valueOf( matrix.get( row, column ) );
    }

    @Override
    protected void vectorsToMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        if ( vectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        int maxSize = this.setUpColumnElements();

        this.matrix = this.createMatrix( vectors, maxSize );
    }

    /**
     * Fill in the data
     *
     * @return DoubleMatrixNamed
     */
    private IntegerMatrix<CompositeSequence, Integer> createMatrix(
            Collection<? extends BulkExpressionDataVector> vectors, int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.size();

        IntegerMatrix<CompositeSequence, Integer> mat = new IntegerMatrix<>( numRows, maxSize );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( j );
        }

        // initialize the matrix to 0
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, 0 );
            }
        }

        Map<Integer, CompositeSequence> rowNames = new TreeMap<>();
        for ( BulkExpressionDataVector vector : vectors ) {

            CompositeSequence designElement = vector.getDesignElement();
            assert designElement != null : "No design element for " + vector;

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

            rowNames.put( rowIndex, designElement );

            int[] vals = vector.getDataAsInts();

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            assert bioAssays.size() == vals.length : "Expected " + vals.length + " got " + bioAssays.size();

            Iterator<BioAssay> it = bioAssays.iterator();

            this.setMatBioAssayValues( mat, rowIndex, ArrayUtils.toObject( vals ), bioAssays, it );

        }

        for ( int i = 0; i < mat.rows(); i++ ) {
            mat.addRowName( rowNames.get( i ) );
        }

        ExpressionDataIntegerMatrix.log.debug( "Created a " + mat.rows() + " x " + mat.columns() + " matrix" );
        return mat;
    }

}

package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.AbstractMatrix;
import ubic.basecode.dataStructure.matrix.IntegerMatrix;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.*;

/**
 * Warning, not fully tested.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class ExpressionDataIntegerMatrix extends AbstractMultiAssayExpressionDataMatrix<Integer> {

    private static final Log log = LogFactory.getLog( ExpressionDataIntegerMatrix.class.getName() );

    private final IntegerMatrix<CompositeSequence, Integer> matrix;

    public ExpressionDataIntegerMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        for ( DesignElementDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.INT ) ) {
                throw new IllegalStateException( "Cannot convert non-integer quantitation types into int matrix" );
            }
        }
        this.matrix = this.createMatrix( selectVectors( vectors ) );
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
    public BulkExpressionDataMatrix<Integer> sliceColumns( List<BioMaterial> bioMaterials ) {
        throw new UnsupportedOperationException( "Slicing multi-assay integer matrices is not supported" );
    }

    @Override
    public ExpressionDataIntegerMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        throw new UnsupportedOperationException( "Slicing multi-assay integer matrices is not supported" );
    }

    @Override
    public Integer[] getRow( int index ) {
        return this.matrix.getRow( index );
    }

    @Override
    public ExpressionDataMatrix<Integer> sliceRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException( "Slicing multi-assay integer matrices is not supported." );
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

    public Integer get( CompositeSequence designElement, BioMaterial bioMaterial ) {
        return this.matrix.get( matrix.getRowIndexByName( designElement ),
                matrix.getColIndexByName( this.getColumnIndex( bioMaterial ) ) );
    }

    @Override
    protected String format( int row, int column ) {
        return String.valueOf( matrix.get( row, column ) );
    }

    /**
     * Fill in the data
     *
     * @return DoubleMatrixNamed
     */
    private IntegerMatrix<CompositeSequence, Integer> createMatrix(
            List<? extends BulkExpressionDataVector> vectors ) {

        int numRows = rows();
        int numCols = columns();

        IntegerMatrix<CompositeSequence, Integer> mat = new IntegerMatrix<>( numRows, numCols );

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

            int rowIndex = this.getRowIndex( designElement );
            assert rowIndex != -1;

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

    private <R, C, V> void setMatBioAssayValues( AbstractMatrix<R, C, V> mat, Integer rowIndex, V[] vals,
            Collection<BioAssay> bioAssays, Iterator<BioAssay> it ) {
        for ( int j = 0; j < bioAssays.size(); j++ ) {
            BioAssay bioAssay = it.next();
            int column = getColumnIndex( bioAssay );
            assert column != -1;
            mat.set( rowIndex, column, vals[j] );
        }
    }
}

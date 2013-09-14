package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.IntegerMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Warning, not fully tested.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataIntegerMatrix extends BaseExpressionDataMatrix<Integer> {

    private static final long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog( ExpressionDataIntegerMatrix.class.getName() );

    private IntegerMatrix<CompositeSequence, Integer> matrix;

    public ExpressionDataIntegerMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        init();

        for ( DesignElementDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.INT ) ) {
                throw new IllegalStateException( "Cannot convert non-integer quantitation types into int matrix" );
            }
        }

        selectVectors( vectors );
        vectorsToMatrix( vectors );
    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     * ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public Integer get( CompositeSequence designElement, BioAssay bioAssay ) {
        int i = this.rowElementMap.get( designElement );
        int j = this.columnAssayMap.get( bioAssay );
        return this.matrix.get( i, j );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     * ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public Integer get( CompositeSequence designElement, BioMaterial bioMaterial ) {
        return this.matrix.get( matrix.getRowIndexByName( designElement ),
                matrix.getColIndexByName( this.columnBioMaterialMap.get( bioMaterial ) ) );
    }

    @Override
    public Integer get( int row, int column ) {
        return matrix.get( row, column );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    @Override
    public Integer[][] get( List<CompositeSequence> designElements, List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public Integer[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );
        return this.getColumn( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(java.lang.Integer)
     */
    @Override
    public Integer[] getColumn( Integer index ) {
        return this.matrix.getColumn( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    @Override
    public Integer[][] getColumns( List<BioAssay> bioAssays ) {
        Integer[][] res = new Integer[bioAssays.size()][];
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            res[i] = this.getColumn( bioAssays.get( i ) );
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    @Override
    public Integer[][] getRawMatrix() {
        Integer[][] res = new Integer[rows()][];
        for ( int i = 0; i < rows(); i++ ) {
            res[i] = this.matrix.getRow( i );
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement
     * )
     */
    @Override
    public Integer[] getRow( CompositeSequence designElement ) {
        return this.matrix.getRow( this.getRowIndex( designElement ) );
    }

    @Override
    public Integer[] getRow( Integer index ) {
        return this.matrix.getRow( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowMap()
     */
    public Collection<CompositeSequence> getRowMap() {
        return this.rowElementMap.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    @Override
    public Integer[][] getRows( List<CompositeSequence> designElements ) {
        Integer[][] res = new Integer[rows()][];
        for ( int i = 0; i < designElements.size(); i++ ) {
            res[i] = this.matrix.getRow( this.getRowIndex( designElements.get( i ) ) );
        }
        return res;
    }

    @Override
    public int rows() {
        return matrix.rows();
    }

    @Override
    public void set( int row, int column, Integer value ) {
        this.matrix.setObj( row, column, value );
    }

    @Override
    protected void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        int maxSize = setUpColumnElements();

        this.matrix = createMatrix( vectors, maxSize );
    }

    /**
     * Fill in the data
     * 
     * @param vectors
     * @param maxSize
     * @return DoubleMatrixNamed
     */
    private IntegerMatrix<CompositeSequence, Integer> createMatrix(
            Collection<? extends DesignElementDataVector> vectors, int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.keySet().size();

        IntegerMatrix<CompositeSequence, Integer> mat = new IntegerMatrix<CompositeSequence, Integer>( numRows, maxSize );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( j );
        }

        // initialize the matrix to 0
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, 0 );
            }
        }

        ByteArrayConverter bac = new ByteArrayConverter();
        Map<Integer, CompositeSequence> rowNames = new TreeMap<Integer, CompositeSequence>();
        for ( DesignElementDataVector vector : vectors ) {

            CompositeSequence designElement = vector.getDesignElement();
            assert designElement != null : "No designelement for " + vector;

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

            rowNames.put( rowIndex, designElement );

            byte[] bytes = vector.getData();
            int[] vals = bac.byteArrayToInts( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            assert bioAssays.size() == vals.length : "Expected " + vals.length + " got " + bioAssays.size();

            Iterator<BioAssay> it = bioAssays.iterator();

            for ( int j = 0; j < bioAssays.size(); j++ ) {

                BioAssay bioAssay = it.next();
                Integer column = this.columnAssayMap.get( bioAssay );

                assert column != null;

                mat.set( rowIndex, column, vals[j] );
            }

        }

        for ( int i = 0; i < mat.rows(); i++ ) {
            mat.addRowName( rowNames.get( i ) );
        }

        log.debug( "Created a " + mat.rows() + " x " + mat.columns() + " matrix" );
        return mat;
    }

}

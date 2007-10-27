package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.IntegerMatrix2DNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Warning, not fully tested.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataIntegerMatrix extends BaseExpressionDataMatrix {

    private static Log log = LogFactory.getLog( ExpressionDataIntegerMatrix.class.getName() );

    private IntegerMatrix2DNamed matrix;

    public ExpressionDataIntegerMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {

    }

    public ExpressionDataIntegerMatrix( ExpressionExperiment expressionExperiment,
            Collection<DesignElement> designElements, QuantitationType quantitationType ) {

    }

    public ExpressionDataIntegerMatrix( Collection<DesignElementDataVector> dataVectors,
            QuantitationType quantitationType ) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public Integer get( DesignElement designElement, BioMaterial bioMaterial ) {
        return this.matrix.get( matrix.getRowIndexByName( designElement ), matrix
                .getColIndexByName( this.columnBioMaterialMap.get( bioMaterial ) ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Integer get( DesignElement designElement, BioAssay bioAssay ) {
        int i = this.rowElementMap.get( designElement );
        int j = this.columnAssayMap.get( bioAssay );
        return this.matrix.get( i, j );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Integer[][] get( List designElements, List bioAssays ) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Integer[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );
        return this.matrix.getColumn( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public Integer[][] getColumns( List bioAssays ) {
        Integer[][] res = new Integer[bioAssays.size()][];
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            res[i] = this.getColumn( ( BioAssay ) bioAssays.get( i ) );
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public Integer[][] getMatrix() {
        Integer[][] res = new Integer[rows()][];
        for ( int i = 0; i < rows(); i++ ) {
            res[i] = this.matrix.getRow( i );
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public Integer[] getRow( DesignElement designElement ) {
        return this.matrix.getRow( this.getRowIndex( designElement ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    public Integer[][] getRows( List designElements ) {
        Integer[][] res = new Integer[rows()][];
        for ( int i = 0; i < designElements.size(); i++ ) {
            res[i] = this.matrix.getRow( this.getRowIndex( ( DesignElement ) designElements.get( i ) ) );
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowMap()
     */
    public Collection<DesignElement> getRowMap() {
        return this.rowElementMap.keySet();
    }

    @Override
    protected void vectorsToMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        int maxSize = setUpColumnElements();

        this.matrix = createMatrix( vectors, maxSize );
    }

    public int columns() {
        return matrix.columns();
    }

    public int rows() {
        return matrix.rows();
    }

    public void set( int row, int column, Object value ) {
        this.matrix.set( row, column, value );
    }

    public Object get( int row, int column ) {
        return matrix.get( row, column );
    }

    public Object[] getRow( Integer index ) {
        return this.matrix.getRow( index );
    }

    /**
     * Fill in the data
     * 
     * @param vectors
     * @param maxSize
     * @return DoubleMatrixNamed
     */
    private IntegerMatrix2DNamed createMatrix( Collection<DesignElementDataVector> vectors, int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.keySet().size();

        IntegerMatrix2DNamed matrix = new IntegerMatrix2DNamed( numRows, maxSize );

        for ( int j = 0; j < matrix.columns(); j++ ) {
            matrix.addColumnName( j );
        }

        // initialize the matrix to 0
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.setQuick( i, j, 0 );
            }
        }

        ByteArrayConverter bac = new ByteArrayConverter();
        for ( DesignElementDataVector vector : vectors ) {

            DesignElement designElement = vector.getDesignElement();
            assert designElement != null : "No designelement for " + vector;

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

            matrix.addRowName( rowIndex );

            byte[] bytes = vector.getData();
            int[] vals = bac.byteArrayToInts( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            assert bioAssays.size() == vals.length : "Expected " + vals.length + " got " + bioAssays.size();

            Iterator it = bioAssays.iterator();

            for ( int j = 0; j < bioAssays.size(); j++ ) {

                BioAssay bioAssay = ( BioAssay ) it.next();
                Integer column = this.columnAssayMap.get( bioAssay );

                assert column != null;

                matrix.setQuick( rowIndex, column, vals[j] );
            }

        }

        log.debug( "Created a " + matrix.rows() + " x " + matrix.columns() + " matrix" );
        return matrix;
    }

}

package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.List;

import ubic.basecode.dataStructure.matrix.StringMatrix2DNamed;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * TODO - DOCUMENT ME
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataStringMatrix extends BaseExpressionDataMatrix {

    private StringMatrix2DNamed matrix;

    public ExpressionDataStringMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {

    }

    public ExpressionDataStringMatrix( ExpressionExperiment expressionExperiment,
            Collection<DesignElement> designElements, QuantitationType quantitationType ) {

    }

    public ExpressionDataStringMatrix( Collection<DesignElementDataVector> dataVectors,
            QuantitationType quantitationType ) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public String get( DesignElement designElement, BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public String[][] get( List designElements, List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public String[] getColumn( BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public String[][] getColumns( List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public String[][] getMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public String[] getRow( DesignElement designElement ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    public String[][] getRows( List designElements ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void vectorsToMatrix( Collection<DesignElementDataVector> vectors ) {
        // TODO Auto-generated method stub

    }

    public int columns() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int rows() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void set( int row, int column, Object value ) {
        // TODO Auto-generated method stub

    }

    public Object get( int row, int column ) {
        return matrix.get( row, column );
    }

    public Object[] getRow( Integer index ) {
        // TODO Auto-generated method stub
        return null;
    }

}

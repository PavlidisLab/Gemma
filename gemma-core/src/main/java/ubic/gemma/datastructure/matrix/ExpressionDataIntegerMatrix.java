package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.List;

import ubic.basecode.dataStructure.matrix.IntegerMatrix2DNamed;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * TODO - DOCUMENT ME
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataIntegerMatrix extends BaseExpressionDataMatrix { 

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
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Integer get( DesignElement designElement, BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Integer[][] get( List designElements, List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Integer[] getColumn( BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public Integer[][] getColumns( List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public Integer[][] getMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public Integer[] getRow( DesignElement designElement ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    public Integer[][] getRows( List designElements ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowMap()
     */
    public Collection<DesignElement> getRowMap() {
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

}

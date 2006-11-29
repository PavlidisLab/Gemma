/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ubic.basecode.dataStructure.matrix.ObjectMatrix2DNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Matrix of booleans mapped from an ExpressionExperiment.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataBooleanMatrix extends BaseExpressionDataMatrix {

    private ObjectMatrix2DNamed matrix;

    public ExpressionDataBooleanMatrix( Collection<DesignElementDataVector> dataVectors,
            QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> selectedVectors = selectVectors( quantitationType, dataVectors );
        vectorsToMatrix( selectedVectors );
    }

    public ExpressionDataBooleanMatrix( ExpressionExperiment expressionExperiment,
            Collection<DesignElement> designElements, QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> vectorsOfInterest = selectVectors( designElements, quantitationType );
        vectorsToMatrix( vectorsOfInterest );
    }

    public ExpressionDataBooleanMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> vectorsOfInterest = selectVectors( expressionExperiment, quantitationType );
        this.vectorsToMatrix( vectorsOfInterest );
    }

    public int columns() {
        return matrix.columns();
    }

    /**
     * Fill in the data
     * 
     * @param vectors
     * @param maxSize
     * @return
     */
    private ObjectMatrix2DNamed createMatrix( Collection<DesignElementDataVector> vectors, int maxSize ) {
        ObjectMatrix2DNamed matrix = new ObjectMatrix2DNamed( vectors.size(), maxSize );

        // initialize the matrix to NaN
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.setQuick( i, j, Double.NaN );
            }
        }
        for ( int j = 0; j < matrix.columns(); j++ ) {
        	matrix.addColumnName(j);
        }

        ByteArrayConverter bac = new ByteArrayConverter();
        int rowNum = 0;
        for ( DesignElementDataVector vector : vectors ) {
            matrix.addRowName( vector.getDesignElement() );
            byte[] bytes = vector.getData();
            boolean[] vals = bac.byteArrayToBooleans( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Iterator<BioAssay> it = dimension.getBioAssays().iterator();

            assert dimension.getBioAssays().size() == vals.length;
            for ( int i = 0; i < vals.length; i++ ) {
                BioAssay bioAssay = it.next();
                matrix.setQuick( rowNum, columnAssayMap.get( bioAssay ), vals[i] );
            }

            rowNum++;
        }
        return matrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Boolean get( DesignElement designElement, BioAssay bioAssay ) {
        return ( Boolean ) this.matrix.get( matrix.getRowIndexByName( designElement ), matrix
                .getColIndexByName( this.columnAssayMap.get( bioAssay ) ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Boolean[][] get( List designElements, List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Boolean[] getColumn( BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public Boolean[][] getColumns( List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public Boolean[][] getMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public Boolean[] getRow( DesignElement designElement ) {
    	Object[] objects = this.matrix.getRow( matrix.getRowIndexByName( designElement )) ;
    	Boolean[] returnArray = new Boolean[objects.length];
    	for(int i = 0; i < objects.length; i++)
    		returnArray[i] = (Boolean)objects[i];
        return returnArray;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    @SuppressWarnings("unchecked")
    public Boolean[][] getRows( List designElements ) {
        if ( designElements == null ) {
            return null;
        }

        Boolean[][] result = new Boolean[designElements.size()][];
        int i = 0;
        for ( DesignElement element : ( List<DesignElement> ) designElements ) {
            Boolean[] rowResult = getRow( element );
            result[i] = rowResult;
            i++;
        }
        return result;
    }

    public int rows() {
        return matrix.rows();
    }

    @Override
    protected void vectorsToMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException();
        }

        int maxSize = setUpColumnElements();

        this.matrix = createMatrix( vectors, maxSize );

    }

}

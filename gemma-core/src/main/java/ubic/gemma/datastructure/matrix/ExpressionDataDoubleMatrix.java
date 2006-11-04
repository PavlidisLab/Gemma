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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A data structure that holds a reference to the data for a given expression experiment. The data can be queried by row
 * or column, returning data for a specific DesignElement or data for a specific BioAssay. The data itself is backed by
 * a SparseRaggedDoubleMatrix2DNamed, which allows for each row to contain a different number of values.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataDoubleMatrix extends BaseExpressionDataMatrix {

    private static Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class.getName() );
    private DoubleMatrixNamed matrix;

    /**
     * @param dataVectors
     * @param quantitationType
     */
    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> dataVectors,
            QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> selectedVectors = selectVectors( quantitationType, dataVectors );
        vectorsToMatrix( selectedVectors );
    }

    /**
     * @param expressionExperiment
     * @param designElements
     * @param quantitationType
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment,
            Collection<DesignElement> designElements, QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> vectorsOfInterest = selectVectors( designElements, quantitationType );
        vectorsToMatrix( vectorsOfInterest );
    }

    /**
     * @param expressionExperiment
     * @param quantitationType
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> vectorsOfInterest = selectVectors( expressionExperiment, quantitationType );
        if ( vectorsOfInterest.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors for " + quantitationType );
        }
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
    private DoubleMatrixNamed createMatrix( Collection<DesignElementDataVector> vectors, int maxSize ) {
        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( vectors.size(), maxSize );

        // initialize the matrix to NaN
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.setQuick( i, j, Double.NaN );
            }
        }

        ByteArrayConverter bac = new ByteArrayConverter();
        int rowNum = 0;
        for ( DesignElementDataVector vector : vectors ) {
            matrix.addRowName( vector.getDesignElement() );
            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Iterator<BioAssay> it = dimension.getBioAssays().iterator();

            assert dimension.getBioAssays().size() == vals.length;
            for ( int i = 0; i < vals.length; i++ ) {
                BioAssay bioAssay = it.next();
                matrix.setQuick( rowNum, columnAssayMap.get( bioAssay ), vals[i] );
            }

            rowNum++;
        }

        for ( Object obj : columnBioMaterialMap.values() ) {
            matrix.addColumnName( obj );
        }

        return matrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double get( DesignElement designElement, BioAssay bioAssay ) {
        int i = matrix.getRowIndexByName( designElement );
        int colNum = this.columnAssayMap.get( bioAssay );
        int j = matrix.getColIndexByName( colNum );
        log.debug( designElement + " = " + i + " " + bioAssay + " = " + j + " (colnum=" + colNum );
        return this.matrix.get( i, j );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Double[][] get( List designElements, List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );

        double[] rawResult = this.matrix.getColumn( index );
        assert rawResult != null;
        Double[] result = new Double[rawResult.length];
        for ( int i = 0; i < rawResult.length; i++ ) {
            result[i] = rawResult[i];
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public Double[][] getColumns( List bioAssays ) {
        // if ( bioAssays == null ) {
        // return null;
        // }
        //
        // List<BioAssay> assays = bioAssays;
        //
        // // Double[][] result = new Double[][assays.size()];
        // for ( BioAssay assay : assays ) {
        // Double[] columnResult = getColumn( assay );
        // }
        return null;
    }

    /**
     * @return DoubleMatrixNamed
     * @deprecated Access to the data should be through the ExpressionDataMatrix interface
     */
    public DoubleMatrixNamed getDoubleMatrixNamed() {
        return this.matrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public Double[][] getMatrix() {

        Double[][] dMatrix = new Double[matrix.rows()][matrix.columns()];
        for ( int i = 0; i < matrix.rows(); i++ ) {
            Double[] row = ( Double[] ) matrix.getRowObj( i );
            dMatrix[i] = row;
        }

        return dMatrix;
    }

    /**
     * @return
     * @deprecated Supplied for backwards compatibility. Access to the data should be through the ExpressionDataMatrix
     *             interface
     */
    public DoubleMatrixNamed getNamedMatrix() {
        return this.matrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public Double[] getRow( DesignElement designElement ) {
        double[] rawResult = this.matrix.getRowByName( designElement );
        assert rawResult != null;
        Double[] result = new Double[rawResult.length];
        for ( int i = 0; i < rawResult.length; i++ ) {
            result[i] = rawResult[i];
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    @SuppressWarnings("unchecked")
    public Double[][] getRows( List designElements ) {
        if ( designElements == null ) {
            return null;
        }

        List<DesignElement> elements = designElements;
        Double[][] result = new Double[elements.size()][];
        int i = 0;
        for ( DesignElement element : elements ) {
            Double[] rowResult = getRow( element );
            result[i] = rowResult;
            i++;
        }
        return result;
    }

    public int rows() {
        return matrix.rows();
    }

    @Override
    public String toString() {
        return matrix.toString();
    }

    /**
     * Convert {@link DesignElementDataVector}s into Boolean matrix.
     * 
     * @param vectors
     * @return DoubleMatrixNamed
     */
    protected void vectorsToMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException();
        }

        int maxSize = setUpColumnElements();

        this.matrix = createMatrix( vectors, maxSize );

    }

}

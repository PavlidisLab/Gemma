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
import java.util.HashSet;
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
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

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

    private static final int MAX_ROWS_TO_STRING = 100;
    private static Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class.getName() );
    private DoubleMatrixNamed matrix;

    /**
     * To comply with bean specifications. Not to be instantiated.
     */
    public ExpressionDataDoubleMatrix() {
        throw new RuntimeException( "This default, no-arg constructor cannot be instantiated.  This constructor "
                + "allows java constructs to inspect this class as a java bean." );
    }

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
     * @param dataVectors
     * @param bioAssayDimensions
     * @param quantitationTypes
     */
    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> dataVectors,
            List<BioAssayDimension> bioAssayDimensions, List<QuantitationType> quantitationTypes ) {
        init();
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, bioAssayDimensions,
                quantitationTypes );
        vectorsToMatrix( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> dataVectors,
            BioAssayDimension bioAssayDimension, QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, bioAssayDimension,
                quantitationType );
        vectorsToMatrix( selectedVectors );
    }

    /**
     * @param expressionExperiment
     * @param bioAssayDimension
     * @param quantitationType
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension,
            QuantitationType quantitationType ) {
        init();
        Collection<DesignElementDataVector> selectedVectors = selectVectors( expressionExperiment, quantitationType,
                bioAssayDimension );
        vectorsToMatrix( selectedVectors );
    }

    /**
     * @param expressionExperiment
     * @param bioAssayDimensions A list of bioAssayDimensions to use.
     * @param quantitationTypes A list of quantitation types to use, in the same order as the bioAssayDimensions
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment,
            List<BioAssayDimension> bioAssayDimensions, List<QuantitationType> quantitationTypes ) {
        init();
        Collection<DesignElementDataVector> selectedVectors = selectVectors( expressionExperiment, quantitationTypes,
                bioAssayDimensions );
        vectorsToMatrix( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment,
            Collection<QuantitationType> quantitationTypes ) {
        init();
        Collection<DesignElementDataVector> vectorsOfInterest = selectVectors( expressionExperiment, quantitationTypes );
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
     * @return DoubleMatrixNamed
     */
    private DoubleMatrixNamed createMatrix( Collection<DesignElementDataVector> vectors, int maxSize ) {

        /*
         * The number of rows in the matrix is equal to the number of BioSequences represented - not the number of
         * vectors.
         */
        int numRows = this.rowDesignElementMapByInteger.keySet().size();

        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( numRows, maxSize );

        // initialize the matrix to -Infinity; this marks values that are not yet initialized.
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.setQuick( i, j, Double.NEGATIVE_INFINITY );
            }
        }

        for ( int j = 0; j < matrix.columns(); j++ ) {
            matrix.addColumnName( j );
        }

        log.info( "Creating a " + matrix.rows() + " x " + matrix.columns() + " matrix" );

        ByteArrayConverter bac = new ByteArrayConverter();
        int rowNum = 0;
        Collection<BioAssayDimension> seenDims = new HashSet<BioAssayDimension>();
        for ( DesignElementDataVector vector : vectors ) {

            DesignElement designElement = vector.getDesignElement();
            assert designElement != null : "No designelement for " + vector;

            int currentRowNum;
            int startIndex = 0;

            Integer rowIndex = this.rowElementMap.get( designElement );

            // Rows are indexed by the underlying sequence.
            if ( !matrix.containsRowName( rowIndex ) ) {
                log.debug( "Adding row " + rowIndex );
                matrix.addRowName( rowIndex );
                currentRowNum = rowNum;
                rowNum++; // only add a new row if we are looking at a new sequence.
            } else {
                // we're adding on to the row.
                // This has to index by an integer, not a sequence.
                log.debug( "Adding on to row " + rowIndex );
                currentRowNum = matrix.getRowIndexByName( rowIndex );
                double[] row = matrix.getRowByName( rowIndex );
                for ( startIndex = 0; startIndex < row.length; startIndex++ ) {
                    double d = row[startIndex];
                    if ( d == Double.NEGATIVE_INFINITY ) break;
                }

            }

            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Iterator<BioAssay> it = dimension.getBioAssays().iterator();
            seenDims.add( dimension );
            assert dimension.getBioAssays().size() == vals.length : "Expected " + vals.length + " got "
                    + dimension.getBioAssays().size();

            for ( int i = startIndex; i < vals.length; i++ ) {
                BioAssay bioAssay = it.next();
                if ( vals[i] == Double.NEGATIVE_INFINITY ) {
                    throw new IllegalArgumentException(
                            "Whoops, data contains -infinity, which we use as a special value at row " + currentRowNum
                                    + " col=" + i );
                }
                matrix.setQuick( currentRowNum, columnAssayMap.get( bioAssay ), vals[i] );
            }

        }
        log.info( seenDims.size() + " bioAssayDimensions observed" );

        // fill in remaining missing values.
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                if ( matrix.getQuick( i, j ) == Double.NEGATIVE_INFINITY ) matrix.setQuick( i, j, Double.NaN );
            }
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
        int i = matrix.getRowIndexByName( ( ( CompositeSequence ) designElement ).getBiologicalCharacteristic() );
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
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public Double get( DesignElement designElement, BioMaterial bioMaterial ) {
        if ( bioMaterial == null ) {
            throw new IllegalArgumentException( "Biomaterial cannot be null" );
        }
        Integer columnIndex = this.columnBioMaterialMap.get( bioMaterial );
        if ( columnIndex == null ) {
            throw new IllegalArgumentException( "No such biomaterial " + bioMaterial );
        }

        Integer rowIndex = this.getRowIndex( designElement );
        if ( rowIndex == null ) {
            throw new IllegalArgumentException( "No such designElement " + designElement );
        }

        return ( Double ) this.matrix
                .get( matrix.getRowIndexByName( rowIndex ), matrix.getColIndexByName( columnIndex ) );
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
     * @return DoubleMatrixNamed
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

        Integer row = this.rowElementMap.get( designElement );

        if ( !this.matrix.containsRowName( row ) ) {
            if ( log.isDebugEnabled() ) log.debug( "No row " + row );
            return null;
        }

        double[] rawResult = this.matrix.getRowByName( row );
        assert rawResult != null;
        Double[] result = new Double[rawResult.length];
        for ( int i = 0; i < rawResult.length; i++ ) {
            result[i] = rawResult[i];
        }
        return result;
    }

    /**
     * Sets the row of matrix to the input data.
     * 
     * @param rowIndex The row index of the data in the matrix to be replaced.
     * @param data The input data.
     */
    public void setRow( int rowIndex, Double[] data ) {
        if ( rowIndex > this.matrix.rows() ) {
            throw new RuntimeException( "Specified row index " + rowIndex + " is larger than the matrix of size "
                    + this.matrix.rows() + "." );
        }

        for ( int j = 0; j < data.length; j++ ) {
            this.matrix.set( rowIndex, j, data[j] );
        }
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#rows()
     */
    public int rows() {
        return matrix.rows();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        int columns = this.columns();
        int rows = this.rows();

        StringBuffer buf = new StringBuffer();
        buf.append( rows + " x " + columns + " matrix of double values, showing up to " + MAX_ROWS_TO_STRING
                + " rows\n" );
        int stop = 0;
        buf.append( "Row\\Col" );
        for ( int i = 0; i < columns; i++ ) {
            buf.append( "\t" + this.getBioMaterialForColumn( i ) + ":" );
            for ( BioAssay ba : this.getBioAssaysForColumn( i ) ) {
                buf.append( ba + "," );
            }
        }
        buf.append( "\n" );
        for ( DesignElement de : getRowElements() ) {
            buf.append( de );
            for ( int i = 0; i < columns; i++ ) {
                buf.append( "\t" + this.get( de, this.getBioMaterialForColumn( i ) ) );
            }
            buf.append( "\n" );
            if ( stop > MAX_ROWS_TO_STRING ) {
                buf.append( "...\n" );
                break;
            }
            stop++;
        }
        return buf.toString();
    }

    /**
     * Convert {@link DesignElementDataVector}s into Double matrix.
     * 
     * @param vectors
     * @return DoubleMatrixNamed
     */
    protected void vectorsToMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        int maxSize = setUpColumnElements();

        this.matrix = createMatrix( vectors, maxSize );

    }

    public void set( int row, int column, Object value ) {
        assert value instanceof Double;
        matrix.setQuick( row, column, ( ( Double ) value ).doubleValue() );
    }

    public void set( DesignElement designElement, BioMaterial bioMaterial, Object value ) {
        assert value instanceof Double;
        int row = this.getRowIndex( designElement );
        int column = this.getColumnIndex( bioMaterial );
        matrix.setQuick( row, column, ( ( Double ) value ).doubleValue() );
    }

    public Double get( int row, int column ) {
        return matrix.get( row, column );
    }

}

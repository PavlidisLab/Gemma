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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.emory.mathcs.backport.java.util.Collections;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * A data structure that holds a reference to the data for a given expression experiment. The data can be queried by row
 * or column, returning data for a specific DesignElement or data for a specific BioAssay.
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

    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> dataVectors,
            BioAssayDimension bioAssayDimension, QuantitationType quantitationType ) {
        init();
        this.bioAssayDimensions.addAll( bioAssayDimensions );
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, bioAssayDimension,
                quantitationType );
        vectorsToMatrix( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> vectors ) {
        init();
        selectVectors( vectors );
        vectorsToMatrix( vectors );
    }

    /**
     * @param dataVectors
     * @param bioAssayDimensions
     * @param quantitationTypes
     */
    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> dataVectors,
            List<BioAssayDimension> bioAssayDimensions, List<QuantitationType> quantitationTypes ) {
        init();
        this.bioAssayDimensions.addAll( bioAssayDimensions );
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, bioAssayDimensions,
                quantitationTypes );
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
        this.bioAssayDimensions.add( bioAssayDimension );
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
        this.bioAssayDimensions.addAll( bioAssayDimensions );
        Collection<DesignElementDataVector> selectedVectors = selectVectors( expressionExperiment, quantitationTypes,
                bioAssayDimensions );
        vectorsToMatrix( selectedVectors );
    }

    public int columns() {
        return matrix.columns();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double get( DesignElement designElement, BioAssay bioAssay ) {
        Integer i = this.rowElementMap.get( designElement );
        Integer j = this.columnAssayMap.get( bioAssay );
        if ( i == null || j == null ) {
            log.warn( "No matrix element for " + designElement + ", " + bioAssay );
            return null;
        }
        return this.matrix.get( i, j );
    }

    public Double get( int row, int column ) {
        return matrix.get( row, column );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Double[][] get( List designElements, List bioAssays ) {
        throw new UnsupportedOperationException( "Sorry, not implemented yet" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );

        return this.getColumn( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(java.lang.Integer)
     */
    public Double[] getColumn( Integer index ) {
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
        throw new UnsupportedOperationException( "Sorry, not implemented yet" );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public Double[] getRow( DesignElement designElement ) {
        Integer row = this.rowElementMap.get( designElement );
        if ( row == null ) return null;
        return getRow( row );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(java.lang.Integer)
     */
    public Double[] getRow( Integer index ) {
        double[] rawRow = matrix.getRow( index );
        return ArrayUtils.toObject( rawRow );
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

    /**
     * @param designElement
     * @param bioAssay
     * @param value
     */
    public void set( DesignElement designElement, BioAssay bioAssay, Object value ) {
        assert value instanceof Double;
        int row = this.getRowIndex( designElement );
        int column = this.getColumnIndex( bioAssay );
        matrix.setQuick( row, column, ( ( Double ) value ).doubleValue() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#set(int, int, java.lang.Object)
     */
    public void set( int row, int column, Object value ) {
        if ( value == null ) {
            matrix.setQuick( row, column, Double.NaN );
        } else {
            matrix.setQuick( row, column, ( ( Double ) value ).doubleValue() );
        }
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
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        int columns = this.columns();
        int rows = this.rows();

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 4 );

        StringBuffer buf = new StringBuffer();
        buf.append( rows + " x " + columns + " matrix of double values, showing up to " + MAX_ROWS_TO_STRING
                + " rows\n" );
        int stop = 0;
        buf.append( "Probe" );
        for ( int i = 0; i < columns; i++ ) {
            buf.append( "\t" + this.getBioMaterialForColumn( i ).getName() + ":" );
            for ( BioAssay ba : this.getBioAssaysForColumn( i ) ) {
                buf.append( ba.getName() + "," );
            }
        }
        buf.append( "\n" );

        boolean warned = false;
        for ( int j = 0; j < rows; j++ ) {

            buf.append( this.rowDesignElementMapByInteger.get( j ).getName() );
            BioSequence biologicalCharacteristic = ( ( CompositeSequence ) this.rowDesignElementMapByInteger.get( j ) )
                    .getBiologicalCharacteristic();
            if ( biologicalCharacteristic != null ) {
                try {
                    // buf.append( " [" + biologicalCharacteristic.getName() + "]" );
                } catch ( org.hibernate.LazyInitializationException e ) {
                    if ( !warned ) {
                        warned = true;
                        log
                                .warn( "Unable to print sequence data: information has not been retrieved from the database" );
                    }
                }
            }

            for ( int i = 0; i < columns; i++ ) {
                Double val = this.get( j, i );
                if ( Double.isNaN( val ) ) {
                    buf.append( "\t" + val );
                } else {
                    buf.append( "\t" + nf.format( this.get( j, i ) ) );
                }
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
     * Fill in the data
     * 
     * @param vectors
     * @param maxSize
     * @return DoubleMatrixNamed
     */
    private DoubleMatrixNamed createMatrix( Collection<DesignElementDataVector> vectors, int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.keySet().size();

        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( numRows, maxSize );

        for ( int j = 0; j < matrix.columns(); j++ ) {
            matrix.addColumnName( j );
        }

        // initialize the matrix to -Infinity; this marks values that are not yet initialized.
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                matrix.setQuick( i, j, Double.NEGATIVE_INFINITY );
            }
        }

        ByteArrayConverter bac = new ByteArrayConverter();
        for ( DesignElementDataVector vector : vectors ) {

            DesignElement designElement = vector.getDesignElement();
            assert designElement != null : "No designelement for " + vector;

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

            // Rows are indexed by the underlying designElement.
            // if ( log.isTraceEnabled() ) log.trace( "Adding row " + rowIndex );
            matrix.addRowName( rowIndex );

            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            if ( bioAssays.size() != vals.length )
                throw new IllegalStateException( "Expected " + vals.length + " got " + bioAssays.size() );

            Iterator it = bioAssays.iterator();

            for ( int j = 0; j < bioAssays.size(); j++ ) {

                BioAssay bioAssay = ( BioAssay ) it.next();
                Integer column = this.columnAssayMap.get( bioAssay );

                assert column != null;

                // if ( log.isTraceEnabled() )
                // log.trace( "Setting " + rowIndex + " " + column + " to " + vals[j] + " for " + bioAssay );

                if ( vals[j] == Double.NEGATIVE_INFINITY ) {
                    throw new IllegalArgumentException(
                            "Whoops, negative infinity is a special value, we can't have it in the data" );
                }
                matrix.setQuick( rowIndex, column, vals[j] );
            }

        }

        // fill in remaining missing values.
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                if ( matrix.getQuick( i, j ) == Double.NEGATIVE_INFINITY ) {
                    // log.debug( "Missing value at " + i + " " + j );
                    matrix.setQuick( i, j, Double.NaN );
                }
            }
        }
        log.debug( "Created a " + matrix.rows() + " x " + matrix.columns() + " matrix" );
        return matrix;
    }

    /**
     * Convert {@link DesignElementDataVector}s into Double matrix.
     * 
     * @param vectors
     * @return DoubleMatrixNamed
     */
    @Override
    protected void vectorsToMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        int maxSize = setUpColumnElements();
        this.matrix = createMatrix( vectors, maxSize );

    }

    /**
     * Create a matrix based on another one's selected rows.
     * 
     * @param sourceMatrix
     * @param rowsToUse
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<DesignElement> rowsToUse ) {
        init();
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;

        this.matrix = DoubleMatrix2DNamedFactory.fastrow( rowsToUse.size(), sourceMatrix.columns() );

        log.info( "Creating a filtered matrix " + rowsToUse.size() + " x " + sourceMatrix.columns() );

        int i = 0;
        for ( DesignElement element : rowsToUse ) {
            super.addToRowMaps( i, element );
            Double[] rowVals = sourceMatrix.getRow( element );
            assert rowVals != null : "Source matrix does not have row for " + element;
            for ( int j = 0; j < rowVals.length; j++ ) {
                Double val = rowVals[j];
                set( i, j, val );
            }
            i++;
        }

    }

    /**
     * @return
     */
    public DoubleMatrixNamed getNamedMatrix() {
        return matrix;
    }

}

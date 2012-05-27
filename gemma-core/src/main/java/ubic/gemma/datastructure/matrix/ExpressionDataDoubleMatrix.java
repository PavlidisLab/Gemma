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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * A data structure that holds a reference to the data for a given expression experiment. The data can be queried by row
 * or column, returning data for a specific DesignElement or data for a specific BioAssay. This class is not database
 * aware so the vectors provided must already be 'thawed'.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataDoubleMatrix extends BaseExpressionDataMatrix<Double> {

    private static final long serialVersionUID = 1L;
    private static final int MAX_ROWS_TO_STRING = 100;
    private static Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class.getName() );
    private DoubleMatrix<CompositeSequence, BioMaterial> matrix;

    private Map<CompositeSequence, Double> ranks = new HashMap<CompositeSequence, Double>();

    /**
     * To comply with bean specifications. Not to be instantiated.
     */
    public ExpressionDataDoubleMatrix() {
        // throw new RuntimeException( "This default, no-arg constructor cannot be instantiated. This constructor "
        // + "allows java constructs to inspect this class as a java bean." );
    }

    /**
     * @param vectors
     */
    public ExpressionDataDoubleMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        init();

        for ( DesignElementDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException( "Cannot convert non-double quantitation types into double matrix:"
                        + dedv.getQuantitationType() );
            }
        }

        selectVectors( vectors );
        vectorsToMatrix( vectors );
    }

    /**
     * @param dataVectors
     * @param quantitationTypes
     */
    public ExpressionDataDoubleMatrix( Collection<? extends DesignElementDataVector> dataVectors,
            Collection<QuantitationType> quantitationTypes ) {
        init();
        for ( QuantitationType qt : quantitationTypes ) {
            if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException( "Cannot convert non-double quantitation types into double matrix: "
                        + qt );
            }
        }
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, quantitationTypes );
        vectorsToMatrix( selectedVectors );
    }

    /**
     * @param dataVectors
     * @param quantitationType
     */
    public ExpressionDataDoubleMatrix( Collection<? extends DesignElementDataVector> dataVectors,
            QuantitationType quantitationType ) {
        init();
        if ( !quantitationType.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalStateException( "Cannot convert non-double quantitation types into double matrix: "
                    + quantitationType );
        }
        Collection<DesignElementDataVector> selectedVectors = selectVectors( dataVectors, quantitationType );
        vectorsToMatrix( selectedVectors );
    }

    /**
     * Create a data matrix like sourceMatrix but use the values from dataMatrix.
     * 
     * @param sourceMatrix
     * @param dataMatrix - The rows can be different than the original matrix, but the columns must be the same.
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> dataMatrix ) {
        init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;
        this.quantitationTypes = sourceMatrix.quantitationTypes;
        this.matrix = dataMatrix;

        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
            this.addToRowMaps( i, dataMatrix.getRowName( i ) );
        }

    }

    /**
     * Create a matrix based on another one's selected rows.
     * 
     * @param sourceMatrix
     * @param rowsToUse
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<CompositeSequence> rowsToUse ) {
        init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;

        this.matrix = new DenseDoubleMatrix<CompositeSequence, BioMaterial>( rowsToUse.size(), sourceMatrix.columns() );
        this.matrix.setColumnNames( sourceMatrix.getMatrix().getColNames() );

        log.info( "Creating a filtered matrix " + rowsToUse.size() + " x " + sourceMatrix.columns() );

        int i = 0;
        for ( CompositeSequence element : rowsToUse ) {
            super.addToRowMaps( i, element );
            Double[] rowVals = sourceMatrix.getRow( element );
            assert rowVals != null : "Source matrix does not have row for " + element;

            this.matrix.addRowName( element );

            for ( int j = 0; j < rowVals.length; j++ ) {
                Double val = rowVals[j];
                set( i, j, val );
            }
            i++;
        }
    }

    /**
     * Create a matrix based on another one's selected columns. The results will be somewhat butchered - only a single
     * BioAssayDimension and the ranks will be copied over (not recomputed based on the selected columns).
     * 
     * @param columnsToUse
     * @param sourceMatrix
     */
    public ExpressionDataDoubleMatrix( List<BioMaterial> columnsToUse, ExpressionDataDoubleMatrix sourceMatrix ) {
        init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;

        this.matrix = new DenseDoubleMatrix<CompositeSequence, BioMaterial>( sourceMatrix.rows(), columnsToUse.size() );
        this.matrix.setRowNames( sourceMatrix.getMatrix().getRowNames() );

        this.ranks = sourceMatrix.ranks; // not strictly correct if we are using subcolumns

        /*
         * Indices of the biomaterials in the original matrix.
         */
        List<Integer> originalBioMaterialIndices = new ArrayList<Integer>();

        List<BioAssay> bioAssays = new ArrayList<BioAssay>();
        for ( BioMaterial bm : columnsToUse ) {
            originalBioMaterialIndices.add( sourceMatrix.getColumnIndex( bm ) );
            bioAssays.add( bm.getBioAssaysUsedIn().iterator().next() );
        }

        this.matrix.setColumnNames( columnsToUse );

        /*
         * fix the upper level column name maps.
         */
        BioAssayDimension reorderedDim = BioAssayDimension.Factory.newInstance();
        reorderedDim.setBioAssays( bioAssays );

        this.bioAssayDimensions.clear();

        reorderedDim.setName( "Slice" );

        this.getQuantitationTypes().addAll( sourceMatrix.getQuantitationTypes() );

        int i = 0;
        for ( ExpressionDataMatrixRowElement element : sourceMatrix.getRowElements() ) {
            CompositeSequence designElement = element.getDesignElement();
            super.addToRowMaps( i, designElement );

            Double[] sourceRow = sourceMatrix.getRow( designElement );

            assert sourceRow != null : "Source matrix does not have row for " + designElement;

            for ( int j = 0; j < originalBioMaterialIndices.size(); j++ ) {
                Double val = sourceRow[originalBioMaterialIndices.get( j )];
                set( i, j, val );
            }
            i++;

            this.bioAssayDimensions.put( designElement, reorderedDim );

        }

        super.setUpColumnElements();

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
    public Double get( CompositeSequence designElement, BioAssay bioAssay ) {
        Integer i = this.rowElementMap.get( designElement );
        Integer j = this.columnAssayMap.get( bioAssay );
        if ( i == null || j == null ) {
            log.warn( "No matrix element for " + designElement + ", " + bioAssay );
            return null;
        }
        return this.matrix.get( i, j );
    }

    @Override
    public Double get( int row, int column ) {
        return matrix.get( row, column );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    @Override
    public Double[][] get( List<CompositeSequence> designElements, List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException( "Sorry, not implemented yet" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public Double[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );

        return this.getColumn( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(java.lang.Integer)
     */
    @Override
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
    @Override
    public Double[][] getColumns( List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException( "Sorry, not implemented yet" );
    }

    /**
     * @return
     */
    public DoubleMatrix<CompositeSequence, BioMaterial> getMatrix() {
        return matrix;
    }

    /**
     * @return The expression level ranks (based on mean signal intensity in the vectors); this will be empty if the
     *         vectors used to construct the matrix were not ProcessedExpressionDataVectors.
     */
    public Map<CompositeSequence, Double> getRanks() {
        return this.ranks;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    @Override
    public Double[][] getRawMatrix() {

        Double[][] dMatrix = new Double[matrix.rows()][matrix.columns()];
        for ( int i = 0; i < matrix.rows(); i++ ) {
            Double[] row = matrix.getRowObj( i );
            dMatrix[i] = row;
        }

        return dMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement
     * )
     */
    @Override
    public Double[] getRow( CompositeSequence designElement ) {
        Integer row = this.rowElementMap.get( designElement );
        if ( row == null ) return null;
        return getRow( row );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(java.lang.Integer)
     */
    @Override
    public Double[] getRow( Integer index ) {
        double[] rawRow = matrix.getRow( index );
        return ArrayUtils.toObject( rawRow );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    @Override
    public Double[][] getRows( List<CompositeSequence> designElements ) {
        if ( designElements == null ) {
            return null;
        }

        List<CompositeSequence> elements = designElements;
        Double[][] result = new Double[elements.size()][];
        int i = 0;
        for ( CompositeSequence element : elements ) {
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
    @Override
    public int rows() {
        return matrix.rows();
    }

    /**
     * @param designElement
     * @param bioAssay
     * @param value
     */
    public void set( CompositeSequence designElement, BioAssay bioAssay, Double value ) {
        int row = this.getRowIndex( designElement );
        int column = this.getColumnIndex( bioAssay );
        matrix.set( row, column, value );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#set(int, int, java.lang.Object)
     */
    @Override
    public void set( int row, int column, Double value ) {
        if ( value == null ) {
            matrix.set( row, column, Double.NaN );
        } else {
            matrix.set( row, column, value );
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

        for ( int j = 0; j < rows; j++ ) {

            buf.append( this.rowDesignElementMapByInteger.get( j ).getName() );
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
     * Convert {@link DesignElementDataVector}s into Double matrix.
     * 
     * @param vectors
     * @return DoubleMatrixNamed
     */
    @Override
    protected void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        for ( DesignElementDataVector vector : vectors ) {
            if ( vector instanceof ProcessedExpressionDataVector ) {
                this.ranks
                        .put( vector.getDesignElement(), ( ( ProcessedExpressionDataVector ) vector ).getRankByMean() );
            }
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
    private DoubleMatrix<CompositeSequence, BioMaterial> createMatrix(
            Collection<? extends DesignElementDataVector> vectors, int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.keySet().size();

        DoubleMatrix<CompositeSequence, BioMaterial> mat = new DenseDoubleMatrix<CompositeSequence, BioMaterial>(
                numRows, maxSize );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( this.getBioMaterialForColumn( j ) );
        }

        // initialize the matrix to -Infinity; this marks values that are not yet initialized.
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, Double.NEGATIVE_INFINITY );
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
            double[] vals = bac.byteArrayToDoubles( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            if ( bioAssays.size() != vals.length )
                throw new IllegalStateException( "Mismatch: " + vals.length + " values in vector ( " + bytes.length
                        + " bytes) for " + designElement + " got " + bioAssays.size()
                        + " bioassays in the bioassaydimension" );

            Iterator<BioAssay> it = bioAssays.iterator();

            for ( int j = 0; j < bioAssays.size(); j++ ) {

                BioAssay bioAssay = it.next();
                Integer column = this.columnAssayMap.get( bioAssay );

                assert column != null;

                // if ( log.isTraceEnabled() )
                // log.trace( "Setting " + rowIndex + " " + column + " to " + vals[j] + " for " + bioAssay );

                if ( vals[j] == Double.NEGATIVE_INFINITY ) {
                    throw new IllegalArgumentException(
                            "Whoops, negative infinity is a special value, we can't have it in the data" );
                }
                mat.set( rowIndex, column, vals[j] );
            }

        }

        /*
         * Note: these row names aren't that important unless we use the bare matrix.
         */
        for ( int i = 0; i < mat.rows(); i++ ) {
            mat.addRowName( rowNames.get( i ) );
        }
        assert mat.getRowNames().size() == mat.rows();

        // fill in remaining missing values.
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                if ( mat.get( i, j ) == Double.NEGATIVE_INFINITY ) {
                    // log.debug( "Missing value at " + i + " " + j );
                    mat.set( i, j, Double.NaN );
                }
            }
        }
        log.debug( "Created a " + mat.rows() + " x " + mat.columns() + " matrix" );
        return mat;
    }

}

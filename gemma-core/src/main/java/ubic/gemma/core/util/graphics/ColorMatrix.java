/*
 * The baseCode project
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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
package ubic.gemma.core.util.graphics;

import java.awt.Color;

import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.math.Constants;
import ubic.gemma.core.util.math.DescriptiveWithMissing;
import ubic.gemma.core.util.math.MatrixStats;
import cern.colt.list.DoubleArrayList;

/**
 * Creates a color matrix from a matrix of doubles
 * 
 * @author Will Braynen
 * 
 */
public class ColorMatrix<A, B> implements Cloneable {

    // data fields

    public static <R, C> ColorMatrix<R, C> newInstance( DoubleMatrix<R, C> matrix ) {
        return new ColorMatrix<R, C>( matrix );
    }

    protected Color[] colorMap = ColorMap.BLACKBODY_COLORMAP;

    protected Color[][] colors;
    protected double displayMax;

    /**
     * Min and max values to display, which might not be the actual min and max values in the matrix. For instance, we
     * might want to clip values, or show a bigger color range for equal comparison with other rows or matrices.
     */
    protected double displayMin;
    /** to be able to sort the rows by an arbitrary key */
    protected int m_rowKeys[];

    protected int m_totalRows, m_totalColumns;
    protected double max;

    protected DoubleMatrix<A, B> maxtrix;

    protected double min;

    protected Color missingColor = Color.lightGray;

    public ColorMatrix( DoubleMatrix<A, B> matrix ) {
        init( matrix );
    }

    /**
     * @param matrix the matrix
     * @param colorMap the simplest color map is one with just two colors: { minColor, maxColor }
     * @param missingColor values missing from the matrix or non-numeric entries will be displayed using this color
     */
    public ColorMatrix( DoubleMatrix<A, B> matrix, Color[] colorMap, Color missingColor ) {
        this.missingColor = missingColor;
        this.colorMap = colorMap;
        init( matrix );
    }

    @Override
    public ColorMatrix<A, B> clone() {

        try {
            super.clone();
        } catch ( CloneNotSupportedException e ) {
        }

        // create another double matrix
        DenseDoubleMatrix<A, B> matrix = new DenseDoubleMatrix<A, B>( m_totalRows, m_totalColumns );
        // copy the row and column names
        for ( int i = 0; i < m_totalRows; i++ ) {
            A rowName = maxtrix.getRowName( i );
            matrix.setRowName( rowName, i );
        }
        for ( int i = 0; i < m_totalColumns; i++ ) {
            B colName = maxtrix.getColName( i );
            matrix.setColumnName( colName, i );
            // copy the data
        }
        for ( int r = 0; r < m_totalRows; r++ ) {
            for ( int c = 0; c < m_totalColumns; c++ ) {
                matrix.set( r, c, maxtrix.get( r, c ) );
            }
        }

        // create another copy of a color matrix (this class)
        ColorMatrix<A, B> clonedColorMatrix = new ColorMatrix<A, B>( matrix, colorMap, missingColor );

        int[] rowKeys = m_rowKeys.clone();
        clonedColorMatrix.setRowKeys( rowKeys );

        return clonedColorMatrix;

    } // end clone

    public Color getColor( int row, int column ) throws ArrayIndexOutOfBoundsException {

        rowRangeCheck( row );
        colRangeCheck( column );

        return colors[getTrueRowIndex( row )][column];
    }

    public Color[] getColorMap() {
        return colorMap;
    }

    public Color[][] getColors() {
        return colors;
    }

    public int getColumnCount() {
        return m_totalColumns;
    }

    public B getColumnName( int column ) throws ArrayIndexOutOfBoundsException {

        colRangeCheck( column );

        return maxtrix.getColName( column );
    }

    public String[] getColumnNames() {
        String[] columnNames = new String[m_totalColumns];
        for ( int i = 0; i < m_totalColumns; i++ ) {
            columnNames[i] = getColumnName( i ).toString();
        }
        return columnNames;
    }

    public double getDisplayMax() {
        return displayMax;
    }

    public double getDisplayMin() {
        return displayMin;
    }

    /**
     * @return a DenseDoubleMatrix2DNamed object
     */
    public DoubleMatrix<A, B> getMatrix() {
        return maxtrix;
    }

    public double getMax() {
        return max;
    }

    public DoubleMatrix<A, B> getMaxtrix() {
        return maxtrix;
    }

    public double getMin() {
        return min;
    }

    public Color getMissingColor() {
        return missingColor;
    }

    public double[] getRow( int row ) throws ArrayIndexOutOfBoundsException {

        rowRangeCheck( row );

        return maxtrix.getRow( getTrueRowIndex( row ) );
    }

    public double[] getRowByName( A rowName ) {
        return maxtrix.getRowByName( rowName );
    }

    public int getRowCount() {
        return m_totalRows;
    }

    public int getRowIndexByName( A rowName ) {
        return maxtrix.getRowIndexByName( rowName );
    }

    public Object getRowName( int row ) throws ArrayIndexOutOfBoundsException {

        rowRangeCheck( row );
        return maxtrix.getRowName( getTrueRowIndex( row ) );
    }

    public String[] getRowNames() {
        String[] rowNames = new String[m_totalRows];
        for ( int i = 0; i < m_totalRows; i++ ) {
            int row = getTrueRowIndex( i );
            Object rowName = getRowName( row );
            String rowNameString = rowName.toString();
            rowNames[i] = rowNameString;
        }
        return rowNames;
    }

    public double getValue( int row, int column ) throws ArrayIndexOutOfBoundsException {

        rowRangeCheck( row );
        colRangeCheck( column );

        return maxtrix.get( getTrueRowIndex( row ), column );
    }

    public void mapValuesToColors() {
        ColorMap colorMapO = new ColorMap( colorMap );
        double range = displayMax - displayMin;

        if ( range < Constants.SMALL ) {
            range = Constants.SMALL;
        }

        // zoom factor for stretching or shrinking the range
        double zoomFactor = ( colorMapO.getPaletteSize() - 1.0 ) / range;

        // map values to colors
        for ( int row = 0; row < m_totalRows; row++ ) {
            for ( int column = 0; column < m_totalColumns; column++ ) {
                double value = getValue( row, column );

                if ( Double.isNaN( value ) ) {
                    // the value is missing
                    colors[row][column] = missingColor;
                } else {
                    // clip extreme values
                    if ( value > displayMax ) {
                        value = displayMax;
                    } else if ( value < displayMin ) {
                        value = displayMin;
                    }

                    // shift the minimum value to zero
                    // to the range [0, maxValue + minValue]
                    value -= displayMin;

                    // stretch or shrink the range to [0, totalColors]
                    double valueNew = value * zoomFactor;
                    int i = ( int ) Math.floor( valueNew );
                    colors[row][column] = colorMapO.getColor( i );
                }
            }
        }
    } // end mapValuesToColors

    public void resetRowKeys() {
        for ( int i = 0; i < m_totalRows; i++ ) {
            m_rowKeys[i] = i;
        }
    }

    /**
     * @param row
     * @param column
     * @param newColor
     * @throws ArrayIndexOutOfBoundsException
     */
    public void setColor( int row, int column, Color newColor ) throws ArrayIndexOutOfBoundsException {

        rowRangeCheck( row );
        colRangeCheck( column );

        colors[getTrueRowIndex( row )][column] = newColor;
    }

    /**
     * @param colorMap an array of colors which define the midpoints in the color map; this can be one of the constants
     *        defined in the ColorMap class, like ColorMap.REDGREEN_COLORMAP and ColorMap.BLACKBODY_COLORMAP
     * @throws IllegalArgumentException if the colorMap array argument contains less than two colors.
     */
    public void setColorMap( Color[] colorMap ) throws IllegalArgumentException {

        if ( colorMap.length < 2 ) {
            throw new IllegalArgumentException();
        }

        this.colorMap = colorMap;
        mapValuesToColors();
    }

    /**
     * Standardized display range
     */
    public void setDisplayRange( double min, double max ) {

        displayMin = min;
        displayMax = max;

        mapValuesToColors();
    }

    /**
     * @param rowKeys
     */
    public void setRowKeys( int[] rowKeys ) {
        m_rowKeys = rowKeys;
    }

    /**
     * Normalizes the elements of a matrix to variance one and mean zero, ignoring missing values todo move this to
     * matrixstats or something.
     */
    public void standardize() {

        // normalize the data in each row
        for ( int r = 0; r < m_totalRows; r++ ) {
            double[] rowValues = getRow( r );
            DoubleArrayList doubleArrayList = new cern.colt.list.DoubleArrayList( rowValues );
            DescriptiveWithMissing.standardize( doubleArrayList );
            rowValues = doubleArrayList.elements();
            setRow( r, rowValues );
        }

        displayMin = -2;
        displayMax = +2;

        mapValuesToColors();

    } // end standardize

    /**
     * To be able to sort the rows by an arbitrary key. Creates <code>m_rowKeys</code> array and initializes it in
     * ascending order from 0 to <code>m_totalRows</code> -1, so that by default it matches the physical order of the
     * columns: [0,1,2,...,m_totalRows-1]
     * 
     * @return int[]
     */
    protected int[] createRowKeys() {
        m_rowKeys = new int[m_totalRows];
        for ( int i = 0; i < m_totalRows; i++ ) {
            m_rowKeys[i] = i;
        }
        return m_rowKeys;
    }

    protected int getTrueRowIndex( int row ) {
        return m_rowKeys[row];
    }

    /**
     * Changes values in a row, clipping if there are more values than columns.
     * 
     * @param row row whose values we want to change
     * @param values new row values
     */
    protected void setRow( int row, double values[] ) {

        // clip if we have more values than columns
        int totalValues = Math.min( values.length, m_totalColumns );

        for ( int column = 0; column < totalValues; column++ ) {
            maxtrix.set( getTrueRowIndex( row ), column, values[column] );

        }
    } // end setRow

    private void colRangeCheck( int column ) {
        if ( column >= m_totalColumns )
            throw new ArrayIndexOutOfBoundsException( "The matrix has only " + m_totalColumns
                    + " columns, so the maximum possible column index is " + ( m_totalColumns - 1 ) + ". Column index "
                    + column + " is too high." );
    }

    private void init( DoubleMatrix<A, B> matrix ) {
        if ( matrix == null ) {
            throw new IllegalArgumentException( "Matrix cannot be null" );
        }
        maxtrix = matrix; // by reference, or should we clone?
        m_totalRows = maxtrix.rows();
        m_totalColumns = maxtrix.columns();
        colors = new Color[m_totalRows][m_totalColumns];
        createRowKeys();

        displayMin = min = MatrixStats.min( maxtrix );
        displayMax = max = MatrixStats.max( maxtrix );

        // map values to colors
        mapValuesToColors();
    }

    private void rowRangeCheck( int row ) {
        if ( row >= m_totalRows )
            throw new ArrayIndexOutOfBoundsException( "The matrix has only " + m_totalRows
                    + " rows, so the maximum possible row index is " + ( m_totalRows - 1 ) + ". Row index " + row
                    + " is too high." );
    }

} // end class ColorMatrix

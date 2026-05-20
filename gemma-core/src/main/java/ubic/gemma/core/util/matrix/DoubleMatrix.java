/*
 * The baseCode project
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
package ubic.gemma.core.util.matrix;

import java.util.*;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Abstract base class for 2D matrices of double values with named columns and rows.
 * 
 * @author pavlidis
 * 
 */
public abstract class DoubleMatrix<R, C> extends AbstractMatrix<R, C, Double> implements PrimitiveMatrix<R, C, Double> {

    private static final long serialVersionUID = 1L;

    public abstract double[][] asArray();

    /**
     * Create a deep copy of this
     * @return a deep copy of this
     */
    public abstract DoubleMatrix<R, C> copy();

    /**
     * @param j
     * @param i
     * @return
     */
    public abstract double get( int j, int i );

    @Override
    public Double getByKeys( R r, C c ) {
        return this.get( getRowIndexByName( r ), getColIndexByName( c ) );
    }

    /**
     * @param startCol inclusive, numbered from zero
     * @param endCol inclusive, numbered from zero
     * @return new matrix with just the requested range of columns from this
     */
    public abstract DoubleMatrix<R, C> getColRange( int startCol, int endCol );

    public abstract double[] getColumn( int j );

    /**
     * @param s String
     * @return double[]
     */
    public double[] getColumnByName( C s ) {
        return getColumn( getColIndexByName( s ) );
    }

    @Override
    public Double getEntry( int row, int column ) {
        return get( row, column );
    }

    public double[][] getRawMatrix() {
        double[][] result = new double[this.rows()][];
        for ( int i = 0; i < this.rows(); i++ ) {
            result[i] = this.getRow( i );
        }
        return result;
    }

    public abstract double[] getRow( int i );

    public abstract DoubleArrayList getRowArrayList( int i );

    /**
     * @param s String
     * @return double[]
     */
    public double[] getRowByName( R s ) {
        return getRow( getRowIndexByName( s ) );
    }

    /**
     * @param startRow inclusive, numbered from zero
     * @param endRow inclusive
     * @return
     */
    public abstract DoubleMatrix<R, C> getRowRange( int startRow, int endRow );

    @Override
    public void setByKeys( R r, C c, Double v ) {
        this.set( getRowIndexByName( r ), getColIndexByName( c ), v );
    }

    /**
     * @param i
     * @param descending
     */
    public List<R> sortByColumnAbsoluteValues( final int i, final boolean descending ) {
        List<R> sorted = new ArrayList<R>( this.getRowNames() );

        Collections.sort( sorted, new Comparator<R>() {

            @Override
            public int compare( R o1, R o2 ) {
                double v1 = Math.abs( get( getRowIndexByName( o1 ), i ) );
                double v2 = Math.abs( get( getRowIndexByName( o2 ), i ) );
                if ( descending ) {
                    return Double.compare( v2, v1 );
                }
                return Double.compare( v1, v2 );
            }
        } );
        return sorted;

    }

    /**
     * Create a copy of this matrix with only the selected columns, in the selected order.
     * 
     * @param columns
     * @return
     */
    public abstract DoubleMatrix<R, C> subsetColumns( List<C> columns );

    /**
     * Create a copy of this matrix with only the selected rows, in the selected order.
     * 
     * @param rowNames
     * @return
     */
    public abstract DoubleMatrix<R, C> subsetRows( List<R> rowNames );

    /*
     * For more advanced matrix writing see the MatrixWriter class (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     * 
     * @see ubic.basecode.io.writer.MatrixWriter
     */
    @Override
    public final String toString() {
        int rows = this.rows();
        int columns = this.columns();
        StringBuffer buf = new StringBuffer();
        int stop = 0;
        buf.append( "# " + rows + "x" + columns + " matrix" );
        if ( rows > MAX_ROWS_TO_PRINT ) {
            buf.append( ": showing up to " + MAX_ROWS_TO_PRINT + " rows" );
        }
        buf.append( "\nlabel" );

        for ( int i = 0; i < columns; i++ ) {
            if ( this.hasColNames() ) {
                buf.append( "\t" + this.getColName( i ) );
            } else {
                buf.append( "\t" + i );
            }
        }
        buf.append( "\n" );
        for ( int j = 0; j < rows; j++ ) {

            if ( this.hasRowNames() ) {
                buf.append( this.getRowName( j ) );
            } else {
                buf.append( j );
            }
            for ( int i = 0; i < columns; i++ ) {
                double value = this.get( j, i );
                if ( Double.isNaN( value ) ) {
                    buf.append( "\t" );
                } else {
                    buf.append( "\t" + String.format( Locale.ENGLISH, "%.4g", value ) );
                }
            }
            buf.append( "\n" );
            if ( stop > MAX_ROWS_TO_PRINT ) {
                buf.append( "...\n" );
                break;
            }
            stop++;
        }
        return buf.toString();
    }

    public abstract DoubleMatrix<C, R> transpose();

    public abstract DoubleMatrix1D viewColumn( int column );

    /**
     * @param j
     * @return
     */
    public abstract DoubleMatrix1D viewRow( int j );
}

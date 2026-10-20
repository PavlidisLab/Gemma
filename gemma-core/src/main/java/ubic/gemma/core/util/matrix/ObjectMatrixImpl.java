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

import java.util.List;

import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;

/**
 * Matrix that can hold any type of object
 * 
 * @author pavlidis
 * 
 */
public class ObjectMatrixImpl<R, C, V> extends AbstractMatrix<R, C, V> implements ObjectMatrix<R, C, V> {

    private static final long serialVersionUID = -902358802107186038L;
    private DenseObjectMatrix2D matrix;

    public ObjectMatrixImpl( int x, int y ) {
        super();
        matrix = new DenseObjectMatrix2D( x, y );
    }

    /**
     * @param columns
     * @return
     */
    @Override
    public ObjectMatrixImpl<R, C, V> subsetColumns( List<C> columns ) {
        ObjectMatrixImpl<R, C, V> returnval = new ObjectMatrixImpl<R, C, V>( this.rows(), columns.size() );
        returnval.setRowNames( this.getRowNames() );
        for ( int i = 0; i < this.rows(); i++ ) {
            int currentColumn = 0;
            for ( C c : columns ) {
                int j = this.getColIndexByName( c );

                returnval.set( i, currentColumn, this.get( i, j ) );

                if ( i == 0 ) {
                    returnval.setColumnName( c, currentColumn );
                }

                currentColumn++;

            }

        }
        return returnval;
    }

    /**
     * @return
     */
    @Override
    public int columns() {
        return matrix.columns();
    }

    /**
     * @param row
     * @param column
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public V get( int row, int column ) {
        return ( V ) matrix.getQuick( row, column );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.NamedObjectMatrix#get(java.lang.Object, java.lang.Object)
     */
    public Object get( R row, C column ) {
        return get( getRowIndexByName( row ), getColIndexByName( column ) );
    }

    @Override
    public V getByKeys( R r, C c ) {
        return this.get( getRowIndexByName( r ), getColIndexByName( c ) );
    }

    @Override
    @SuppressWarnings("unchecked")
    public V[] getColumn( int col ) {
        V[] result = ( V[] ) new Object[rows()]; // this is how they do it in ArrayList
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = get( i, col );
        }

        return result;
    }

    @Override
    public V getEntry( int row, int column ) {
        return get( row, column );
    }

    @Override
    @SuppressWarnings("unchecked")
    public V[] getRow( int row ) {
        Object[] ro = viewRow( row ).toArray();
        V[] result = ( V[] ) new Object[columns()]; // this is how they do it in ArrayList
        System.arraycopy( ro, 0, result, 0, ro.length );
        return result;
    }

    @Override
    public boolean isMissing( int i, int j ) {
        return get( i, j ) == "";
    }

    /**
     * @return
     */

    @Override
    public int rows() {
        return matrix.rows();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.NamedMatrix#set(int, int, java.lang.Object)
     */
    @Override
    public void set( int row, int column, V value ) {
        matrix.setQuick( row, column, value );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.NamedMatrix#set(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public void setByKeys( R r, C c, V v ) {
        this.set( getRowIndexByName( r ), getColIndexByName( c ), v );
    }

    /**
     * @return
     */
    @Override
    public int size() {
        return matrix.size();
    }

    @Override
    public ObjectMatrix<R, C, V> subset( int startRow, int startCol, int numRow, int numCol ) {
        int endRow = startRow + numRow - 1;
        super.checkRowRange( startRow, endRow );
        int endCol = startCol + numCol - 1;
        super.checkColRange( startCol, endCol );
        ObjectMatrix<R, C, V> result = new ObjectMatrixImpl<R, C, V>( numRow, numCol );
        int r = 0;
        for ( int i = startRow; i < endRow; i++ ) {
            int c = 0;
            for ( int j = startCol; j < endCol; j++ ) {
                result.set( r, c++, this.get( i, j ) );
            }
            r++;
        }
        /*
         * FIXME set up the row/column names.
         */
        return result;

    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append( "label" );
        for ( int i = 0; i < columns(); i++ ) {
            buf.append( "\t" + getColName( i ) );
        }
        buf.append( "\n" );

        for ( int i = 0; i < rows(); i++ ) {
            buf.append( getRowName( i ) );
            for ( int j = 0; j < columns(); j++ ) {
                V v = get( i, j );

                if ( v instanceof Double ) {
                    buf.append( String.format( "\t%.3g", v ) );

                } else {
                    buf.append( "\t" + v );
                }
            }
            buf.append( "\n" );
        }
        return buf.toString();
    }

    /**
     * @param column
     * @return
     */
    public ObjectMatrix1D viewColumn( int column ) {
        return matrix.viewColumn( column );
    }

    /**
     * @param row
     * @return
     */
    public ObjectMatrix1D viewRow( int row ) {
        return matrix.viewRow( row );
    }
}

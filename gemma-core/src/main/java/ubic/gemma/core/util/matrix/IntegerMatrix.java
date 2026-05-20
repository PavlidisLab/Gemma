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

/**
 * @author pavlidis
 */
public class IntegerMatrix<R, C> extends AbstractMatrix<R, C, Integer> implements PrimitiveMatrix<R, C, Integer> {

    private static final long serialVersionUID = -8413796057024940237L;
    private ObjectMatrixImpl<R, C, Integer> matrix;

    public IntegerMatrix( int x, int y ) {
        super();
        matrix = new ObjectMatrixImpl<>( x, y );
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
    public Integer get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public Integer getByKeys( R r, C c ) {
        return this.get( getRowIndexByName( r ), getColIndexByName( c ) );
    }

    @Override
    public Integer[] getColObj( int col ) {
        Integer[] result = new Integer[rows()];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = get( i, col );
        }
        return result;
    }

    public Integer[] getColumn( int col ) {
        Integer[] result = new Integer[rows()];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = get( i, col );
        }
        return result;
    }

    @Override
    public Integer getEntry( int row, int column ) {
        return get( row, column );
    }

    @Override
    public Integer getObject( int row, int col ) {
        return get( row, col );
    }

    public Integer[] getRow( int row ) {
        Integer[] result = new Integer[columns()];
        for ( int i = 0; i < columns(); i++ ) {
            result[i] = get( i, row );
        }
        return result;

    }

    @Override
    public Integer[] getRowObj( int row ) {
        Integer[] result = new Integer[columns()];
        for ( int i = 0; i < columns(); i++ ) {
            result[i] = get( row, i );
        }
        return result;
    }

    @Override
    public boolean isMissing( int i, int j ) {
        return get( i, j ) == null;
    }

    /**
     * @return
     */
    @Override
    public int rows() {
        return matrix.rows();
    }

    @Override
    public void set( int row, int column, Integer value ) {
        matrix.set( row, column, value );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.NamedMatrix#set(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public void setByKeys( R r, C c, Integer v ) {
        this.set( getRowIndexByName( r ), getColIndexByName( c ), v );
    }

    /**
     * @param row
     * @param column
     * @param value
     */
    public void setObj( int row, int column, Integer value ) {
        matrix.set( row, column, value );
    }

    /**
     * @return
     */
    @Override
    public int size() {
        return matrix.size();
    }

    /**
     * @return java.lang.Integer
     */
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
                buf.append( "\t" + get( i, j ) );
            }
            buf.append( "\n" );
        }
        return buf.toString();
    }

}

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

import java.util.Iterator;
import java.util.List;

/**
 * Represents a matrix with index columns and rows. The keys are generic.
 * 
 * @author Paul Pavlidis
 * 
 * @see ObjectMatrix for matrix storing objects.
 */
public abstract interface Matrix2D<R, C, V> {

    /**
     * Add a column name, to the end of the current column names. Useful when building up a matrix in stages.
     * 
     * @param s Object a column name
     */
    public void addColumnName( C s );

    /**
     * Attempt to coerce the entries into doubles.
     * <p>
     * Numeric entries (Double, BigDecimal, Integer, BigInteger) and Strings that can be parsed as doubles are
     * converted. Booleans are converted to 1 or 0. Dates are converted via Date.getDate(). Null entries are rendered as
     * Double.NaN. For entries that are other types of objects, the HashCode is used.
     * 
     * @return
     */
    public double[][] asDoubles();

    /**
     * Set all values in the matrix to the given value.
     * 
     * @param value
     */
    public void assign( V value );

    /**
     * Get the number of columns the matrix has.
     * 
     * @return int
     */
    public int columns();

    /**
     * @param columnName
     * @return
     */
    public boolean containsColumnName( C columnName );

    /**
     * @param rowName
     * @return
     */
    public boolean containsRowName( R rowName );

    /**
     * @param r
     * @param c
     */
    public V getByKeys( R r, C c );

    /**
     * Get the index of a column by name.
     * 
     * @param s Object
     * @return int
     */
    public int getColIndexByName( C s );

    /**
     * Gte the column name for an index.
     * 
     * @param i int
     * @return java.lang.Object
     */
    public C getColName( int i );

    /**
     * @return list of column names. Do not modify this list. Use the addColumnName methods.
     */
    public List<C> getColNames();

    /**
     * @param i
     * @param j
     * @return
     */
    public V getEntry( int i, int j );

    /**
     * Get the index of a row by name..
     * 
     * @param s Object
     * @return int
     */
    public int getRowIndexByName( R s );

    /**
     * Get the row name for an index
     * 
     * @param i int
     * @return java.lang.Object
     */
    public R getRowName( int i );

    /**
     * @return java.util.Iterator
     */
    public Iterator<R> getRowNameMapIterator();

    /**
     * @return
     */
    public List<R> getRowNames();

    /**
     * Check if this matrix has a valid set of column names.
     * 
     * @return boolean
     */
    public boolean hasColNames();

    /**
     * @param r Object
     * @return boolean
     */
    public boolean hasRow( R r );

    /**
     * @return boolean
     */
    public boolean hasRowNames();

    /**
     * Check if the value at a given index is missing.
     * 
     * @param i row
     * @param j column
     * @return true if the value is missing, false otherwise.
     */
    public boolean isMissing( int i, int j );

    /**
     * Return the number of missing values in the matrix.
     * 
     * @return
     */
    public int numMissing();

    /**
     * Get the number of rows the matrix has
     * 
     * @return int
     */
    public int rows();

    /**
     * @param row
     * @param column
     * @param value
     */
    public void set( int row, int column, V value );

    /**
     * @param r
     * @param c
     * @param v
     */
    public void setByKeys( R r, C c, V v );

    /**
     * Add a column name associated with an index.
     * 
     * @param s Object a column name
     * @param index int the column index associated with this name
     */
    public void setColumnName( C s, int index );

    /**
     * @param v
     */
    public void setColumnNames( List<C> v );

    /**
     * Add a row name associated with a row index.
     * 
     * @param s Object
     * @param index int
     */
    public void setRowName( R s, int index );

    /**
     * @param v
     */
    public void setRowNames( List<R> v );

}
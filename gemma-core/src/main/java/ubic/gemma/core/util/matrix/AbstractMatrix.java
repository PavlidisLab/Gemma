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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pavlidis
 */
public abstract class AbstractMatrix<R, C, V> implements Matrix2D<R, C, V>, java.io.Serializable {

    protected static final int MAX_ROWS_TO_PRINT = 100;

    private static final long serialVersionUID = 1L;
    private Map<C, Integer> colMap;
    private List<C> colNames;
    private int lastColumnIndex = 0;

    private int lastRowIndex = 0;
    private Map<R, Integer> rowMap; // contains a map of each row and elements in the row

    private List<R> rowNames;

    /**
     *
     *
     */
    public AbstractMatrix() {
        rowMap = new LinkedHashMap<>(); // contains a map of each row name to index
        // of the row.
        colMap = new LinkedHashMap<>();
        rowNames = new ArrayList<>();
        colNames = new ArrayList<>();
    }

    /**
     * Add a column name when we don't care what the index will be. The index will be set by the method. This is useful
     * for when we need to set up a matrix before we know how many column or rows there are.
     *
     * @param s
     */
    @Override
    public final void addColumnName( C s ) {

        if ( s == null ) {
            throw new IllegalArgumentException( "Column name cannot be null" );
        }

        if ( colMap.containsKey( s ) ) {
            throw new IllegalArgumentException( "Duplicate column name " + s );
        }

        this.colNames.add( s );
        this.colMap.put( s, lastColumnIndex );
        lastColumnIndex++;

    }

    /**
     * Add a row name when we don't care what the index will be. The index will be set by the method. This is useful for
     * when we need to set up a matrix before we know how many column or rows there are.
     *
     * @param s
     */
    public final void addRowName( R s ) {

        if ( s == null ) {
            throw new IllegalArgumentException( "Row name cannot be null" );
        }
        if ( rowMap.containsKey( s ) ) {
            throw new IllegalArgumentException( "Duplicate row name " + s );
        }

        this.rowNames.add( s );
        this.rowMap.put( s, lastRowIndex );
        lastRowIndex++;
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.matrix.Matrix2D#asDoubles()
     */
    @Override
    public double[][] asDoubles() {
        double[][] result = new double[rows()][columns()];

        int n = this.rows();
        int m = this.columns();
        for ( int i = 0; i < n; i++ ) {

            for ( int j = 0; j < m; j++ ) {

                V value = getEntry( i, j );

                if ( isMissing( i, j ) ) {
                    result[i][j] = Double.NaN;
                } else if ( value instanceof Integer ) {
                    result[i][j] = ( ( Integer ) value ).doubleValue();
                } else if ( value instanceof Long ) {
                    result[i][j] = ( ( Long ) value ).doubleValue();
                } else if ( value instanceof Double ) {
                    result[i][j] = ( Double ) value;
                } else if ( value instanceof Boolean ) {
                    result[i][j] = ( ( Boolean ) value ) ? 1.0 : 0.0;
                } else if ( value instanceof String ) {
                    try {
                        result[i][j] = Double.parseDouble( ( String ) value );
                    } catch ( NumberFormatException e ) {
                        result[i][j] = value.hashCode();
                    }
                } else if ( value instanceof BigDecimal ) {
                    result[i][j] = ( ( BigDecimal ) value ).doubleValue();
                } else if ( value instanceof BigInteger ) {
                    result[i][j] = ( ( BigInteger ) value ).doubleValue();
                } else if ( value instanceof Date ) {
                    result[i][j] = ( ( Date ) value ).getTime();
                } else {
                    result[i][j] = value.hashCode();
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.matrix.Matrix2D#assign(java.lang.Object)
     */
    @Override
    public void assign( V value ) {
        for ( int i = 0; i < this.rows(); i++ ) {
            for ( int j = 0; j < this.columns(); j++ ) {
                this.set( i, j, value );
            }
        }
    }

    @Override
    public final boolean containsColumnName( C columnName ) {
        return colMap.containsKey( columnName );
    }

    @Override
    public final boolean containsRowName( R rowName ) {
        return hasRow( rowName );
    }

    /**
     * @param columnKey String
     * @return int
     */
    @Override
    public final int getColIndexByName( C columnKey ) {
        Integer c = colMap.get( columnKey );
        if ( c == null ) throw new IllegalArgumentException( "'" + columnKey + "' not found" );
        return c.intValue();
    }

    /**
     * @param i int
     * @return java.lang.String
     */
    @Override
    public final C getColName( int i ) {
        if ( !this.hasColNames() || this.colNames.size() < i + 1 ) return null;
        return colNames.get( i );
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.matrix.Matrix2D#getColNames()
     */
    @Override
    public final List<C> getColNames() {
        return colNames;
    }

    /**
     * @param s String
     * @return int
     */
    @Override
    public final int getRowIndexByName( R s ) {
        Integer r = rowMap.get( s );
        if ( r == null ) throw new IllegalArgumentException( s + " not found" );
        return r.intValue();
    }

    /**
     * @param i int
     * @return java.lang.String
     */
    @Override
    public final R getRowName( int i ) {
        if ( !this.hasRowNames() ) return null;
        return rowNames.get( i );
    }

    @Override
    public final Iterator<R> getRowNameMapIterator() {
        return this.rowMap.keySet().iterator();
    }

    @Override
    public final List<R> getRowNames() {
        return rowNames;
    }

    @Override
    public final boolean hasColNames() {
        return colNames.size() > 0;
    }

    /**
     * Test for the presence of missing values (null, or in the case of numbers, NaN)
     *
     * @return
     */
    public boolean hasMissingValues() {
        int n = this.rows();
        int m = this.columns();
        for ( int i = 0; i < n; i++ ) {

            for ( int j = 0; j < m; j++ ) {
                if ( isMissing( i, j ) ) return true;

            }
        }
        return false;
    }

    @Override
    public final boolean hasRow( R r ) {
        return this.rowMap.containsKey( r );
    }

    @Override
    public final boolean hasRowNames() {
        return rowNames.size() > 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see basecode.dataStructure.NamedMatrix#numMissing()
     */
    @Override
    public final int numMissing() {
        int count = 0;
        int n = this.rows();
        int m = this.columns();
        for ( int i = 0; i < n; i++ ) {
            for ( int j = 0; j < m; j++ ) {
                if ( isMissing( i, j ) ) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public final void setColumnName( C s, int i ) {

        if ( s == null ) {
            throw new IllegalArgumentException( "Column name cannot be null" );
        }

        if ( colMap.containsKey( s ) ) {
            throw new IllegalArgumentException( "Duplicate column name " + s );
        }

        if ( this.colNames.size() > i ) {
            this.colNames.set( i, s );
        } else {
            this.colNames.add( s );
        }

        this.colMap.put( s, i );
    }

    @Override
    public void setColumnNames( List<C> v ) {
        this.colNames.clear();
        this.colMap.clear();

        for ( int i = 0; i < v.size(); i++ ) {
            setColumnName( v.get( i ), i );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see basecode.dataStructure.NamedMatrix#addRowName(java.lang.String, int)
     */
    @Override
    public final void setRowName( R s, int i ) {
        if ( s == null ) {
            throw new IllegalArgumentException( "Row name cannot be null" );
        }
        if ( this.hasRow( s ) ) {
            throw new IllegalArgumentException( "Duplicate row name " + s );
        }

        this.rowNames.add( s );
        this.rowMap.put( s, i );
    }

    @Override
    public final void setRowNames( List<R> v ) {
        this.rowNames.clear();
        this.rowMap.clear();

        if ( v.size() != this.rows() ) {
            throw new IllegalArgumentException( "Cannot add " + v.size() + " row names to a matrix with " + this.rows()
                    + " rows" );
        }

        for ( int i = 0; i < v.size(); i++ ) {
            R rowName = v.get( i );
            this.setRowName( rowName, i );
        }
    }

    public abstract int size();

    protected void checkColRange( int startCol, int endCol ) {
        if ( startCol < 0 || startCol > rows() - 1 || startCol >= endCol ) {
            throw new IllegalArgumentException( "Invalid start col" );
        }
        if ( endCol < startCol || endCol > rows() - 1 ) {
            throw new IllegalArgumentException( "Invalid end col" );
        }

    }

    protected void checkRowRange( int startRow, int endRow ) {
        if ( startRow < 0 || startRow > rows() - 1 || startRow > endRow ) {
            throw new IllegalArgumentException( "Invalid start row" );
        }
        if ( endRow < startRow || endRow > rows() - 1 ) {
            throw new IllegalArgumentException( "Invalid end row" );
        }

    }
}
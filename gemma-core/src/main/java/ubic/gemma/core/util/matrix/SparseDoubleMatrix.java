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

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

/**
 * A sparse matrix that knows about row and column names.
 * 
 * @author Paul Pavlidis
 * 
 */
public class SparseDoubleMatrix<R, C> extends DoubleMatrix<R, C> {

    /**
     * 
     */
    private static final long serialVersionUID = -1651885517252689369L;
    private SparseDoubleMatrix2D matrix;

    /**
     * @param T double[][]
     */
    public SparseDoubleMatrix( double T[][] ) {
        super();
        matrix = new SparseDoubleMatrix2D( T );
    }

    /**
     * @param rows int
     * @param cols int
     */
    public SparseDoubleMatrix( int rows, int cols ) {
        super();
        matrix = new SparseDoubleMatrix2D( rows, cols );
    }

    /**
     * @param rows int
     * @param cols int
     * @param initlalCapacity int
     * @param minLoadFactor double
     * @param maxLoadFactor double
     */
    public SparseDoubleMatrix( int rows, int cols, int initialCapacity, double minLoadFactor, double maxLoadFactor ) {
        super();
        matrix = new SparseDoubleMatrix2D( rows, cols, initialCapacity, minLoadFactor, maxLoadFactor );
    }

    /**
     * @return double[][]
     */
    @Override
    public double[][] asArray() {
        double[][] result = new double[rows()][];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = getRow( i );
        }
        return result;
    }

    /**
     * @return
     */
    public int cardinality() {
        return matrix.cardinality();
    }

    /**
     * @return
     */

    @Override
    public int columns() {
        return matrix.columns();
    }

    @Override
    public DoubleMatrix<R, C> copy() {
        DoubleMatrix<R, C> returnval = new SparseDoubleMatrix<R, C>( this.rows(), this.columns() );

        for ( int i = 0; i < this.rows(); i++ ) {
            returnval.setRowName( this.getRowName( i ), i );
            for ( int j = 0; j < this.columns(); j++ ) {
                if ( i == 0 ) {
                    returnval.setColumnName( this.getColName( j ), j );
                }
                returnval.set( i, j, this.get( i, j ) );
            }
        }
        return returnval;

    }

    /**
     * @param row
     * @param column
     * @return
     */
    @Override
    public double get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public Double[] getColObj( int col ) {
        Double[] result = new Double[rows()];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = new Double( get( i, col ) );
        }
        return result;
    }

    @Override
    public DoubleMatrix<R, C> getColRange( int startCol, int endCol ) {
        super.checkColRange( startCol, endCol );

        DoubleMatrix<R, C> returnval = new SparseDoubleMatrix<R, C>( this.rows(), 1 + endCol - startCol );
        int k = 0;
        for ( int i = startCol; i <= endCol; i++ ) {
            C colName = this.getColName( i );
            if ( colName != null ) {
                returnval.setColumnName( colName, i );
            }
            for ( int j = 0, m = this.rows(); j < m; j++ ) {
                if ( i == startCol ) {
                    R rowName = this.getRowName( j );
                    returnval.setRowName( rowName, j );
                }
                returnval.set( j, k, this.get( j, i ) );
            }
            k++;
        }
        return returnval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see basecode.dataStructure.matrix.DoubleMatrixNamed#getColumn(int)
     */
    @Override
    public double[] getColumn( int col ) {
        double[] result = new double[rows()];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = get( i, col );
        }
        return result;
    }

    @Override
    public Double getObject( int row, int col ) {
        return new Double( get( row, col ) );
    }

    /**
     * Return a reference to a specific row.
     * 
     * @param row int
     * @return double[]
     */
    @Override
    public double[] getRow( int row ) {
        return viewRow( row ).toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see basecode.dataStructure.matrix.AbstractNamedDoubleMatrix#getRowArrayList(int)
     */
    @Override
    public DoubleArrayList getRowArrayList( int i ) {
        return new DoubleArrayList( getRow( i ) );
    }

    /**
     * @param s String
     * @return double[]
     */
    @Override
    public double[] getRowByName( R s ) {
        return getRow( getRowIndexByName( s ) );
    }

    @Override
    public Double[] getRowObj( int row ) {
        Double[] result = new Double[columns()];
        for ( int i = 0; i < columns(); i++ ) {
            result[i] = new Double( get( row, i ) );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.Matrix2D#getRowRange(int, int)
     */
    @Override
    public DoubleMatrix<R, C> getRowRange( int startRow, int endRow ) {
        super.checkRowRange( startRow, endRow );

        DoubleMatrix<R, C> returnval = new SparseDoubleMatrix<R, C>( endRow + 1 - startRow, this.columns() );
        int k = 0;
        for ( int i = startRow; i <= endRow; i++ ) {
            R rowName = this.getRowName( i );
            if ( rowName != null ) {
                returnval.setRowName( rowName, i );
            }
            for ( int j = 0, m = this.columns(); j < m; j++ ) {
                if ( i == 0 ) {
                    C colName = this.getColName( j );
                    returnval.setColumnName( colName, j );
                }
                returnval.set( k, j, this.get( i, j ) );
            }
            k++;
        }
        return returnval;
    }

    @Override
    public boolean isMissing( int i, int j ) {
        return Double.isNaN( get( i, j ) );
    }

    /**
     * @return
     */
    @Override
    public int rows() {
        return matrix.rows();
    }

    @Override
    public void set( int row, int column, Double value ) {
        matrix.set( row, column, value );
    }

    /**
     * @return
     */
    @Override
    public int size() {
        return matrix.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.DoubleMatrix#subsetColumns(java.util.List)
     */
    @Override
    public DoubleMatrix<R, C> subsetColumns( List<C> columns ) {

        DoubleMatrix<R, C> returnval = new DenseDoubleMatrix<R, C>( this.rows(), columns.size() );
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

    @Override
    public DoubleMatrix<R, C> subsetRows( List<R> rowNames ) {
        DoubleMatrix<R, C> returnval = new SparseDoubleMatrix<R, C>( rowNames.size(), this.columns() );

        int currentRow = 0;
        for ( R rowName : rowNames ) {

            if ( !this.containsRowName( rowName ) ) continue;

            int i = this.getRowIndexByName( rowName );

            returnval.setRowName( rowName, currentRow );
            for ( int j = 0; j < this.columns(); j++ ) {
                if ( currentRow == 0 ) {
                    returnval.setColumnName( this.getColName( j ), j );
                }
                returnval.set( currentRow, j, this.get( i, j ) );
            }
            currentRow++;
        }

        if ( !returnval.getRowNames().containsAll( rowNames ) ) {
            throw new IllegalArgumentException( "Invalid rows to select, some are not in the original matrix" );
        }

        return returnval;
    }

    @Override
    public DoubleMatrix<C, R> transpose() {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     */
    public void trimToSize() {
        matrix.trimToSize();
    }

    /**
     * @param column
     * @return
     */
    @Override
    public DoubleMatrix1D viewColumn( int column ) {
        return matrix.viewColumn( column );
    }

    /**
     * @param row
     * @return
     */
    @Override
    public DoubleMatrix1D viewRow( int row ) {
        return matrix.viewRow( row );
    }
}
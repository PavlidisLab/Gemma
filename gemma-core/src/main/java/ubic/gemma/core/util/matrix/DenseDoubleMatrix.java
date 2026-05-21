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
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.AbstractMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * A dense matrix of doubles that knows about row and column names.
 * 
 * @author Paul Pavlidis
 * 
 */
public class DenseDoubleMatrix<R, C> extends DoubleMatrix<R, C> {

    private static final long serialVersionUID = -239226762166912931L;
    private DoubleMatrix2D matrix;

    /**
     * @param T double[][]
     */
    public DenseDoubleMatrix( double T[][] ) {
        super();
        matrix = new DenseDoubleMatrix2D( T );
    }

    /**
     * @param rows int
     * @param cols int
     */
    public DenseDoubleMatrix( int rows, int cols ) {
        super();
        matrix = new DenseDoubleMatrix2D( rows, cols );
    }

    /**
     * @return double[][]
     */
    @Override
    public double[][] asArray() {
        return matrix.toArray();
    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    /**
     * @return basecode.dataStructure.DenseDoubleMatrix2DNamed
     */
    @Override
    public DoubleMatrix<R, C> copy() {
        DoubleMatrix<R, C> returnval = new DenseDoubleMatrix<R, C>( this.rows(), this.columns() );
        for ( int i = 0, n = this.rows(); i < n; i++ ) {
            R rowName = this.getRowName( i );
            // Fine, if you don't want row names
            if ( rowName != null )
                returnval.setRowName( rowName, i );
            for ( int j = 0, m = this.columns(); j < m; j++ ) {
                if ( i == 0 ) {
                    C colName = this.getColName( j );
                    if ( colName != null )
                        returnval.setColumnName( colName, j );
                }
                returnval.set( i, j, this.get( i, j ) );
            }
        }
        return returnval;
    }

    /**
     * @param row int
     * @param column int
     * @return
     * @see DoubleMatrix2D#get(int, int)
     */
    @Override
    public double get( int row, int column ) {
        return matrix.getQuick( row, column );
    }

    /**
     * Return a copy of a given column.
     * 
     * @param col int
     * @return double[]
     */
    public double[] getColByName( C s ) {
        int col = getColIndexByName( s );
        double[] result = new double[rows()];
        for ( int i = 0; i < rows(); i++ ) {
            result[i] = get( i, col );
        }
        return result;
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

        DoubleMatrix<R, C> returnval = new DenseDoubleMatrix<R, C>( this.rows(), 1 + endCol - startCol );
        int k = 0;
        for ( int i = startCol; i <= endCol; i++ ) {
            C colName = this.getColName( i );
            if ( colName != null ) {
                returnval.addColumnName( colName );
            }
            for ( int j = 0, m = this.rows(); j < m; j++ ) {
                if ( i == startCol ) {
                    R rowName = this.getRowName( j );
                    if ( rowName != null ) returnval.addRowName( rowName );
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

    /**
     * Converts to a String that can be read by read.table in R, using default parameters
     * 
     * @return java.lang.String
     */
    /*
     * public String toRReadTableString() { Format nf = new Format( "%.4g" ); StringBuffer result = new StringBuffer(
     * this.rows() * this.columns() ); if ( this.hasColNames() ) { for ( int i = 0; i < this.columns(); i++ ) {
     * result.append( "\"" + this.getColName( i ) +"\" "); System.out.println("\"" + this.getColName( i ) +"\" "); }
     * result.append( "\n" ); } for ( int i = 0; i < this.rows(); i++ ) { if ( this.hasRowNames() ) { result.append("\""
     * + this.getRowName( i ) + "\"" ); } for ( int j = 0; j < this.columns(); j++ ) { if ( Double.isNaN( this.get( i, j
     * ) ) ) { result.append( " NA" ); } else { result.append( " " + nf.format( this.get( i, j ) ) ); } } result.append(
     * "\n" ); } return result.toString(); }
     */

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
     * Return a reference to a specific row.
     * 
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

        DoubleMatrix<R, C> returnval = new DenseDoubleMatrix<R, C>( 1 + endRow - startRow, this.columns() );
        int k = 0;
        for ( int i = startRow; i <= endRow; i++ ) {
            R rowName = this.getRowName( i );
            if ( rowName != null ) {
                returnval.setRowName( rowName, i );
            }
            for ( int j = 0, m = this.columns(); j < m; j++ ) {
                if ( i == startRow ) {
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

    @Override
    public int rows() {
        return matrix.rows();
    }

    @Override
    public void set( int row, int column, Double value ) {
        matrix.set( row, column, value );
    }

    /**
     * Primitive overload of {@link #set(int, int, Double)} that avoids autoboxing on hot paths.
     */
    public void set( int row, int column, double value ) {
        matrix.set( row, column, value );
    }

    /**
     * Set every cell to {@code value} using the backing Colt matrix's primitive {@code assign} loop.
     * <p>
     * Avoids both autoboxing and the {@code O(rows*cols)} per-call overhead of doing this with {@link #set(int, int, double)}
     * — useful for initialising a sparse matrix to NaN before populating it from vectors with shorter dimensions.
     */
    public void fill( double value ) {
        matrix.assign( value );
    }

    /**
     * @return int
     * @see AbstractMatrix2D#size()
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

        DoubleMatrix<R, C> returnval = new DenseDoubleMatrix<R, C>( rowNames.size(), this.columns() );

        int currentRow = 0;
        for ( R rowName : rowNames ) {

            if ( !this.containsRowName( rowName ) ) continue;

            int i = this.getRowIndexByName( rowName );

            returnval.setRowName( rowName, currentRow );
            for ( int j = 0, m = this.columns(); j < m; j++ ) {
                if ( currentRow == 0 ) {
                    C colName = this.getColName( j );
                    returnval.setColumnName( colName, j );
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

        DoubleMatrix<C, R> result = new DenseDoubleMatrix<C, R>( this.columns(), this.rows() );
        if ( this.getColNames().size() > 0 ) {
            result.setRowNames( this.getColNames() );
        }
        if ( this.getRowNames().size() > 0 ) {
            result.setColumnNames( this.getRowNames() );
        }

        for ( int i = 0; i < this.rows(); i++ ) {
            for ( int j = 0; j < this.columns(); j++ ) {
                result.set( j, i, this.get( i, j ) );
            }
        }

        return result;

    }

    /**
     * @param column int
     * @return cern.colt.matrix.DoubleMatrix1D
     */
    @Override
    public DoubleMatrix1D viewColumn( int column ) {
        return matrix.viewColumn( column );
    }

    /**
     * @param row int
     * @return DoubleMatrix1D
     * @see DenseDoubleMatrix#viewRow(int)
     */
    @Override
    public DoubleMatrix1D viewRow( int row ) {
        return matrix.viewRow( row );
    }

}
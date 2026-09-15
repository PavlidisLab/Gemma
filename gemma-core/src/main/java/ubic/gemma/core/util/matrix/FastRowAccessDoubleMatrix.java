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

/**
 * Dense 2D matrix implementation designed for very fast access of entire rows.
 * <p>
 * Implementation note: The key difference between this and the DenseDouble2DMatrixNamed is that this delegates to a
 * DoubleArrayList[], while DenseDouble2DMatrixNamed delegates to a DenseDoubleMatrix2D.
 * 
 * @author pavlidis
 * 
 */
public class FastRowAccessDoubleMatrix<R, C> extends DoubleMatrix<R, C> {

    private static final long serialVersionUID = -5458302072944941517L;
    private DoubleArrayList[] data;

    /**
     * Implementation note: The input matrix is NOT COPIED (due to the way colt DoubleArrayList(double[]) is
     * implemented).F
     * 
     * @param t
     */
    public FastRowAccessDoubleMatrix( double[][] t ) {
        super();
        data = new DoubleArrayList[t.length];
        for ( int i = 0; i < t.length; i++ ) {
            data[i] = new DoubleArrayList( t[i] );
        }
    }

    /**
     * @param rows
     * @param cols
     */
    public FastRowAccessDoubleMatrix( int rows, int cols ) {
        super();
        data = new DoubleArrayList[rows];
        for ( int i = 0; i < rows; i++ ) {
            data[i] = new DoubleArrayList( new double[cols] );
        }
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

    /*
     * (non-Javadoc)
     * 
     * @see basecode.dataStructure.matrix.DoubleMatrixNamed#columns()
     */

    @Override
    public int columns() {
        assert data[0] != null;
        return data[0].size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.DoubleMatrixNamed#copy()
     */
    @Override
    public DoubleMatrix<R, C> copy() {
        DoubleMatrix<R, C> returnval = new FastRowAccessDoubleMatrix<R, C>( this.rows(), this.columns() );

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

    @Override
    public double get( int x, int y ) {
        assert data[x] != null;
        return data[x].get( y );
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

        DoubleMatrix<R, C> returnval = new FastRowAccessDoubleMatrix<R, C>( this.rows(), 1 + endCol - startCol );
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

    /*
     * (non-Javadoc)
     * 
     * @see basecode.dataStructure.matrix.DoubleMatrixNamed#getRow(int)
     */

    @Override
    public Double getObject( int row, int col ) {
        return new Double( get( row, col ) );
    }

    @Override
    public double[] getRow( int i ) {
        return data[i].elements();
    }

    /*
     * (non-Javadoc)
     * 
     * @see basecode.dataStructure.matrix.DoubleMatrixNamed#getRowArrayList(int)
     */
    @Override
    public DoubleArrayList getRowArrayList( int i ) {
        return data[i];
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

        DoubleMatrix<R, C> returnval = new FastRowAccessDoubleMatrix<R, C>( endRow + 1 - startRow, this.columns() );
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
                double value = this.get( i, j );
                assert k < returnval.rows();
                assert j < returnval.columns();
                returnval.set( k, j, value );

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
        return data.length;
    }

    @Override
    public void set( int x, int y, Double value ) {
        assert data != null;
        assert data[x] != null;
        assert value != null;
        assert y >= 0 && y < data[x].size();
        data[x].set( y, value );
    }

    @Override
    public int size() {
        return this.data.length * this.data[0].size();
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.matrix.DoubleMatrix#subsetRows(java.util.Collection)
     */
    @Override
    public DoubleMatrix<R, C> subsetRows( List<R> rowNames ) {
        DoubleMatrix<R, C> returnval = new FastRowAccessDoubleMatrix<R, C>( rowNames.size(), this.columns() );

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

        DoubleMatrix<C, R> result = new DenseDoubleMatrix<C, R>( this.columns(), this.rows() );
        result.setRowNames( this.getColNames() );
        result.setColumnNames( this.getRowNames() );

        for ( int i = 0; i < this.rows(); i++ ) {
            for ( int j = 0; j < this.columns(); j++ ) {
                result.set( j, i, this.get( i, j ) );
            }
        }

        return result;

    }

    @Override
    public DoubleMatrix1D viewColumn( int column ) {
        return new DenseDoubleMatrix1D( this.getColumn( column ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see basecode.dataStructure.matrix.DoubleMatrixNamed#viewRow(int)
     */
    @Override
    public DoubleMatrix1D viewRow( int j ) {
        return new DenseDoubleMatrix1D( data[j].elements() );
    }
}

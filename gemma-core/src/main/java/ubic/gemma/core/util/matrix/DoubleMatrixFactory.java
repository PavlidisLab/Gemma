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
 * Use this factory to create matrices of type selected at runtime (String parameterization only)
 * 
 * @author Paul Pavlidis
 * 
 */
public class DoubleMatrixFactory {

    public static CompressedSparseDoubleMatrix<String, String> compressedsparse( int rows, int cols ) {
        return new CompressedSparseDoubleMatrix<String, String>( rows, cols );
    }

    public static DenseDoubleMatrix<String, String> dense( double T[][] ) {
        return new DenseDoubleMatrix<String, String>( T );
    }

    /**
     * Creates a matrix in which the underlying data is a copy; the row and column labels are not copied.
     * 
     * @param T
     * @return
     */
    public static DenseDoubleMatrix<String, String> dense( DoubleMatrix<String, String> T ) {
        DenseDoubleMatrix<String, String> copy = new DenseDoubleMatrix<String, String>( T.rows(), T.columns() );
        copy.setRowNames( T.getRowNames() );
        copy.setColumnNames( T.getColNames() );
        for ( int i = 0; i < T.rows(); i++ ) {
            for ( int j = 0; j < T.columns(); j++ ) {
                copy.set( i, j, T.get( i, j ) );
            }
        }
        return copy;
    }

    public static DenseDoubleMatrix<String, String> dense( int rows, int cols ) {
        return new DenseDoubleMatrix<String, String>( rows, cols );
    }

    public static FastRowAccessDoubleMatrix<String, String> fastrow( double T[][] ) {
        return new FastRowAccessDoubleMatrix<String, String>( T );
    }

    public static FastRowAccessDoubleMatrix<String, String> fastrow( int rows, int cols ) {
        return new FastRowAccessDoubleMatrix<String, String>( rows, cols );
    }

    public static SparseDoubleMatrix<String, String> sparse( double T[][] ) {
        return new SparseDoubleMatrix<String, String>( T );
    }

    public static SparseDoubleMatrix<String, String> sparse( int rows, int cols ) {
        return new SparseDoubleMatrix<String, String>( rows, cols );
    }

}
/*
 * The baseCode project
 * 
 * Copyright (c) 2008-2019 University of British Columbia
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.DoubleFunction;

import cern.colt.list.BooleanArrayList;
import ubic.gemma.core.util.math.Constants;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * @author Paul
 */
public class MatrixUtil {

    /**
     * @param  d
     * @return   true if any of the values are very close to zero.
     */
    public static boolean containsNearlyZeros( DoubleMatrix1D d ) {
        for ( int i = 0; i < d.size(); i++ ) {
            if ( Math.abs( d.getQuick( i ) ) < Constants.SMALL ) return true;
        }
        return false;
    }

    /**
     * Extract the diagonal from a matrix.
     * 
     * @param  matrix
     * @return
     */
    public static DoubleMatrix1D diagonal( DoubleMatrix2D matrix ) {

        int mindim = Math.min( matrix.rows(), matrix.columns() );
        DoubleMatrix1D result = new DenseDoubleMatrix1D( mindim );
        for ( int i = mindim; --i >= 0; ) {
            result.set( i, matrix.getQuick( i, i ) );
        }
        return result;
    }

    /**
     * @param  n
     * @param  indexToDrop
     * @return
     */
    public static DoubleMatrix2D dropColumn( DoubleMatrix2D n, int indexToDrop ) {
        int columns = n.columns() - 1;
        if ( columns < 0 ) throw new IllegalArgumentException( "Must leave some columns" );
        DoubleMatrix2D res = new DenseDoubleMatrix2D( n.rows(), columns );
        int k = 0;
        for ( int j = 0; j < n.columns(); j++ ) {
            if ( indexToDrop == j ) {
                continue;
            }
            for ( int i = 0; i < n.rows(); i++ ) {
                res.set( i, k, n.getQuick( i, j ) );
            }
            k++;
        }
        return res;
    }

    /**
     * @param n
     * @param droppedColumns
     */
    public static DoubleMatrix2D dropColumns( DoubleMatrix2D n, Collection<Integer> droppedColumns ) {
        int columns = n.columns() - droppedColumns.size();
        if ( columns < 0 ) throw new IllegalArgumentException( "Must leave some columns" );
        DoubleMatrix2D res = new DenseDoubleMatrix2D( n.rows(), columns );
        int k = 0;
        for ( int j = 0; j < n.columns(); j++ ) {
            if ( droppedColumns.contains( j ) ) {
                continue;
            }
            for ( int i = 0; i < n.rows(); i++ ) {
                res.set( i, k, n.getQuick( i, j ) );
            }
            k++;
        }
        return res;
    }

    /**
     * Makes a copy
     * 
     * @param  list
     * @return
     */
    public static DoubleMatrix1D fromList( DoubleArrayList list ) {
        DoubleMatrix1D r = new DenseDoubleMatrix1D( list.size() );
        for ( int i = 0; i < list.size(); i++ ) {
            r.setQuick( i, list.getQuick( i ) );
        }
        return r;
    }

    /**
     * @param           <R>
     * @param           <C>
     * @param           <V>
     * @param  matrix
     * @param  rowIndex
     * @param  colIndex
     * @return
     */
    public static <R, C, V> V getObject( Matrix2D<R, C, V> matrix, int rowIndex, int colIndex ) {
        if ( ObjectMatrix.class.isAssignableFrom( matrix.getClass() ) ) {
            return ( ( ObjectMatrix<R, C, V> ) matrix ).get( rowIndex, colIndex );
        } else if ( matrix instanceof PrimitiveMatrix<?, ?, ?> ) {
            return ( ( PrimitiveMatrix<R, C, V> ) matrix ).getObject( rowIndex, colIndex );
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @param           <R>
     * @param           <C>
     * @param           <V>
     * @param  matrix
     * @param  rowIndex
     * @return
     */
    public static <R, C, V> V[] getRow( Matrix2D<R, C, V> matrix, int rowIndex ) {
        if ( ObjectMatrix.class.isAssignableFrom( matrix.getClass() ) ) {
            return ( ( ObjectMatrix<R, C, V> ) matrix ).getRow( rowIndex );
        } else if ( matrix instanceof PrimitiveMatrix<?, ?, ?> ) {
            return ( ( PrimitiveMatrix<R, C, V> ) matrix ).getRowObj( rowIndex );
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @param source the source of information about missing values
     * @param target the target where we want to convert values to missing
     */
    public static void maskMissing( DoubleMatrix2D source, DoubleMatrix2D target ) {
        source.checkShape( target );

        for ( int i = 0; i < source.rows(); i++ ) {
            for ( int j = 0; j < source.columns(); j++ ) {
                if ( Double.isNaN( source.getQuick( i, j ) ) ) {
                    target.set( i, j, Double.NaN );
                }
            }
        }

    }

    public static DoubleMatrix1D multWithMissing( DoubleMatrix1D a, DoubleMatrix2D b ) {
        return multWithMissing( a.like2D( 1, a.size() ).assign( new double[][] { a.toArray() } ), b ).viewRow( 0 );
    }

    /**
     * @param  a
     * @param  b
     * @return
     */
    public static DoubleMatrix1D multWithMissing( DoubleMatrix2D a, DoubleMatrix1D b ) {
        int m = a.rows();
        int n = a.columns();

        if ( b.size() != a.columns() ) {
            throw new IllegalArgumentException();
        }

        DoubleMatrix1D C = new DenseDoubleMatrix1D( m );
        C.assign( 0.0 );

        for ( int j = 0; j < m; j++ ) {
            double s = 0.0;
            for ( int k = 0; k < n; k++ ) {
                double aval = a.getQuick( j, k );
                double bval = b.getQuick( k );
                if ( Double.isNaN( aval ) || Double.isNaN( bval ) ) {
                    continue;
                }
                s += aval * bval;
            }
            C.setQuick( j, s + C.getQuick( j ) );
        }

        return C;
    }

    /**
     * Multiple two matrices, tolerate missing values.
     * 
     * @param  a
     * @param  b
     * @return
     */
    public static DoubleMatrix2D multWithMissing( DoubleMatrix2D a, DoubleMatrix2D b ) {
        int m = a.rows();
        int n = a.columns();
        int p = b.columns();

        if ( b.rows() != a.columns() ) {
            throw new IllegalArgumentException( "Nonconformant matrices: " + b.rows() + " != " + a.columns() );
        }

        DoubleMatrix2D C = new DenseDoubleMatrix2D( m, p );
        C.assign( 0.0 );
        for ( int i = 0; i < p; i++ ) {
            for ( int j = 0; j < m; j++ ) {
                double s = 0.0;
                for ( int k = 0; k < n; k++ ) {
                    double aval = a.getQuick( j, k );
                    double bval = b.getQuick( k, i );
                    if ( Double.isNaN( aval ) || Double.isNaN( bval ) ) {
                        continue;
                    }
                    s += aval * bval;
                }
                C.setQuick( j, i, s + C.getQuick( j, i ) );
            }
        }
        return C;
    }

    public static List<Integer> notNearlyZeroIndices( DoubleMatrix1D d ) {
        List<Integer> result = new ArrayList<>();
        for ( int i = 0; i < d.size(); i++ ) {
            if ( Math.abs( d.getQuick( i ) ) > Constants.SMALL ) result.add( i );
        }
        return result;
    }

    /**
     * @param  data
     * @return      a copy of the data with missing or infinite values removed (might be empty!)
     */
    public static DoubleMatrix1D removeMissingOrInfinite(DoubleMatrix1D data ) {
        int sizeWithoutMissingValues = sizeWithoutMissingValues( data );
        if ( sizeWithoutMissingValues == data.size() ) return data;
        DoubleMatrix1D r = new DenseDoubleMatrix1D( sizeWithoutMissingValues );
        double[] elements = data.toArray();
        int size = data.size();
        int j = 0;
        for ( int i = 0; i < size; i++ ) {
            if ( Double.isNaN( elements[i] ) || Double.isInfinite( elements[i] ) ) {
                continue;
            }
            r.set( j++, elements[i] );
        }
        return r;
    }


    /**
     * @param x
     * @return a copy of x with missing values removed
     */
    public static final DoubleMatrix1D removeMissing(DoubleMatrix1D x) {
        if (x.size() == 0) return x.copy();
        BooleanArrayList ok = new BooleanArrayList(x.size());

        for (int i = 0; i < x.size(); i++) {
            double a = x.getQuick(i);
            ok.add(!(Double.isNaN(a)));
        }

        return stripNonOK(x, ok);
    }

    /**
     * Remove values from data corresponding to missing values in reference.
     * 
     * @param  reference
     * @param  data
     * @return
     */
    public static DoubleMatrix1D removeMissingOrInfinite(DoubleMatrix1D reference, DoubleMatrix1D data ) {
        if ( data.size() != reference.size() ) throw new IllegalArgumentException( "Reference and data must have same size" );
        int sizeWithoutMissingValues = sizeWithoutMissingValues( reference );
        if ( sizeWithoutMissingValues == reference.size() ) return data; // no missing values.
        DoubleMatrix1D r = new DenseDoubleMatrix1D( sizeWithoutMissingValues );
        double[] elements = data.toArray();
        double[] refels = reference.toArray();
        int size = data.size();
        int j = 0;
        for ( int i = 0; i < size; i++ ) {
            if ( Double.isNaN( refels[i] ) || Double.isInfinite( refels[i] ) ) {
                continue;
            }
            r.set( j++, elements[i] );
        }
        return r;
    }




    public static final DoubleMatrix1D stripNegative(DoubleMatrix1D x) {
        if (x.size() == 0) return x.copy();
        BooleanArrayList ok = new BooleanArrayList(x.size());

        for (int i = 0; i < x.size(); i++) {
            double a = x.getQuick(i);
            ok.add(a >= 0.0);
        }

        return stripNonOK(x, ok);
    }

    public static final DoubleMatrix1D stripByCriterion(DoubleMatrix1D x, DoubleFunction<Boolean> criterion) {
        DoubleArrayList okvals = new DoubleArrayList();
        for (int i = 0; i < x.size(); i++) {
            if (criterion.apply(x.get(i))) {
                okvals.add(x.get(i));
            }
        }
        DoubleMatrix1D answer = new cern.colt.matrix.impl.DenseDoubleMatrix1D(okvals.size());
        for (int i = 0; i < answer.size(); i++) {
            answer.set(i, okvals.get(i));
        }
        return answer;
    }


    /**
     * Compute the conjuction (logical 'and') of two boolean vectors
     *
     * @param a
     * @param b
     * @return conjunction of a and b
     */
    public static final BooleanArrayList conjunction(BooleanArrayList a, BooleanArrayList b) {
        assert a.size() == b.size();
        BooleanArrayList answer = new BooleanArrayList();
        for (int i = 0; i < a.size(); i++) {
            answer.add(a.get(i) && b.get(i));
        }
        return answer;
    }

    /**
     * @param x         vector to be operated on
     * @param criterion a function that returns a boolean if a double matches the desired criteria
     * @return booleans indicating which values in x match the criterion func
     */
    public static BooleanArrayList matchingCriteria(DoubleMatrix1D x, DoubleFunction<Boolean> criterion) {
        BooleanArrayList ok = new BooleanArrayList(x.size());
        for (int i = 0; i < x.size(); i++) {
            double a = x.getQuick(i);
            ok.add(criterion.apply(a));
        }
        return ok;
    }


    /**
     * @param x         vector to be operated on
     * @param criterion criterion used to test values if they should be acted on.
     * @param action    function applied to values if they match the criterion
     * @return copy of x in which the values meeting the criterion have been replaced with the return value of action, otherwise unchanged from the original x
     */
    public static DoubleMatrix1D applyToIndicesMatchingCriteria(DoubleMatrix1D
                                                                        x, DoubleFunction<Boolean> criterion, DoubleFunction<Double> action) {

        if (x.size() == 0) return x.copy();
        DoubleMatrix1D result = new cern.colt.matrix.impl.DenseDoubleMatrix1D(x.size());

        for (int i = 0; i < x.size(); i++) {
            double a = x.getQuick(i);
            if (criterion.apply(a)) {
                result.set(i, action.apply(a));
            } else {
                result.set(i, a);
            }
        }
        return result;
    }

    /**
     * @param x  a vector of values to be filtered
     * @param ok a list of booleans defining which values are "ok".
     * @return A copy of x that has the non-ok values removed.
     */
    public static final DoubleMatrix1D stripNonOK(DoubleMatrix1D x, BooleanArrayList ok) {

        assert ok.size() == x.size();

        DoubleArrayList okvals = new DoubleArrayList();
        for (int i = 0; i < x.size(); i++) {
            if (ok.get(i)) {
                okvals.add(x.get(i));
            }
        }
        DoubleMatrix1D answer = new cern.colt.matrix.impl.DenseDoubleMatrix1D(okvals.size());

        for (int i = 0; i < answer.size(); i++) {
            answer.set(i, okvals.get(i));
        }
        return answer;
    }

    /**
     * Perform the awkward operation of substituting certain values in a vector from values in another vector.
     *
     * @param x            vector to be operated on in place (it will be modified).
     * @param toReplace    boolean indicators of same length of x, 'true' indicates a value to be replaced in x with a value from 'replacements'; 'false' will be unmodified in x.
     * @param replacements the replacements, in order, to be substituted in x where toReplace(i) is true. This vector can be shorter than x.
     */
    public static void replaceValues(DoubleMatrix1D x, BooleanArrayList toReplace, DoubleMatrix1D replacements) {

        if (toReplace.size() != x.size()) {
            throw new IllegalArgumentException("replacements and x must be the same size");
        }
        if (replacements.size() > x.size()) {
            throw new IllegalArgumentException("Replacements must not outnumber the target");
        }

        int numToReplace = 0;
        for (int i = 0; i < toReplace.size(); i++) {
            if (toReplace.get(i)) {
                numToReplace++;
            }
        }

        if (numToReplace != replacements.size()) {
            throw new IllegalArgumentException("Number of replacements has to match the nubmer of true values in toReplace");
        }

        int j = 0;
        for (int i = 0; i < x.size(); i++) {
            if (toReplace.get(i)) {
                x.set(i, replacements.get(j));
                j++;
            }
        }

    }


    public static DoubleMatrix1D select( DoubleMatrix1D v, Collection<Integer> selected ) {
        DoubleMatrix1D result = new DenseDoubleMatrix1D( selected.size() );
        int k = 0;
        for ( int i = 0; i < v.size(); i++ ) {
            if ( selected.contains( i ) ) {
                result.set( k, v.getQuick( i ) );
                k++;
            }
        }
        return result;
    }

    public static DoubleMatrix2D selectColumns( DoubleMatrix2D n, Collection<Integer> selected ) {
        int ncols = selected.size();
        DoubleMatrix2D res = new DenseDoubleMatrix2D( n.rows(), ncols );
        int k = 0;
        for ( int j = 0; j < n.columns(); j++ ) {
            if ( !selected.contains( j ) ) {
                continue;
            }
            for ( int i = 0; i < n.rows(); i++ ) {
                res.set( i, k, n.getQuick( i, j ) );
                i++;
            }
        }
        return res;
    }

    /**
     * @param  n        square matrix
     * @param  selected
     * @return
     */
    public static DoubleMatrix2D selectColumnsAndRows( DoubleMatrix2D n, Collection<Integer> selected ) {
        if ( n.rows() != n.columns() ) {
            throw new IllegalArgumentException( "must be a square matrix" );
        }

        if ( selected.isEmpty() ) {
            throw new IllegalArgumentException( "must select more than one" );
        }

        int columns = selected.size();

        if ( columns == n.columns() ) {
            return n;
        }

        if ( columns < 0 ) throw new IllegalArgumentException( "Must leave some columns" );
        DoubleMatrix2D res = new DenseDoubleMatrix2D( columns, columns );
        int k = 0;
        for ( int j = 0; j < n.columns(); j++ ) {
            if ( !selected.contains( j ) ) {
                continue;
            }
            int m = 0;
            for ( int i = 0; i < n.rows(); i++ ) {
                if ( !selected.contains( i ) ) {
                    continue;
                }
                res.set( m, k, n.getQuick( i, j ) );
                m++;
            }
            k++;
        }
        return res;
    }

    public static DoubleMatrix2D selectRows( DoubleMatrix2D n, Collection<Integer> selected ) {
        int nrows = selected.size();
        DoubleMatrix2D res = new DenseDoubleMatrix2D( nrows, n.columns() );
        for ( int j = 0; j < n.columns(); j++ ) {
            int m = 0;
            for ( int i = 0; i < n.rows(); i++ ) {
                if ( !selected.contains( i ) ) {
                    continue;
                }
                res.set( m, j, n.getQuick( i, j ) );
                m++;
            }
        }
        return res;
    }

    public static int sizeWithoutMissingValues( DoubleMatrix1D list ) {

        int size = 0;
        for ( int i = 0; i < list.size(); i++ ) {
            double v = list.getQuick( i );
            if ( !Double.isNaN( v ) && !Double.isInfinite( v ) ) {
                size++;
            }
        }
        return size;
    }

    /**
     * Makes a copy
     * 
     * @param  vector
     * @return
     */
    public static DoubleArrayList toList( DoubleMatrix1D vector ) {
        return new DoubleArrayList( vector.toArray() );
    }

    /**
     * Shim for callers that still receive a baseCode {@link ubic.basecode.dataStructure.matrix.DoubleMatrix} (e.g. from
     * {@code ubic.basecode.io.reader.DoubleMatrixReader}, which has not yet been ported in-tree). Performs a value-wise
     * copy into an in-tree {@link DenseDoubleMatrix}, preserving row / column names.
     * <p>
     * Delete once {@code DoubleMatrixReader} is ported in-tree (see {@code BASECODE_MATH_LINEARMODELS_RECCE.md}).
     */
    public static <R, C> DoubleMatrix<R, C> fromBaseCode( ubic.basecode.dataStructure.matrix.DoubleMatrix<R, C> src ) {
        if ( src == null ) return null;
        DenseDoubleMatrix<R, C> out = new DenseDoubleMatrix<>( src.rows(), src.columns() );
        if ( src.hasRowNames() ) out.setRowNames( src.getRowNames() );
        if ( src.hasColNames() ) out.setColumnNames( src.getColNames() );
        for ( int i = 0; i < src.rows(); i++ ) {
            for ( int j = 0; j < src.columns(); j++ ) {
                out.set( i, j, src.get( i, j ) );
            }
        }
        return out;
    }

    /**
     * Companion shim to {@link #fromBaseCode(ubic.basecode.dataStructure.matrix.DoubleMatrix)} for string matrices
     * (e.g. from {@code ubic.basecode.io.reader.StringMatrixReader}). Same caveat: delete once readers are ported in-tree.
     */
    public static <R, C> StringMatrix<R, C> fromBaseCode( ubic.basecode.dataStructure.matrix.StringMatrix<R, C> src ) {
        if ( src == null ) return null;
        StringMatrix<R, C> out = new StringMatrix<>( src.rows(), src.columns() );
        if ( src.hasRowNames() ) out.setRowNames( src.getRowNames() );
        if ( src.hasColNames() ) out.setColumnNames( src.getColNames() );
        for ( int i = 0; i < src.rows(); i++ ) {
            for ( int j = 0; j < src.columns(); j++ ) {
                out.set( i, j, src.get( i, j ) );
            }
        }
        return out;
    }

}

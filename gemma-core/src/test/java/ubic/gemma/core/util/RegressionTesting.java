/*
 * The Gemma project
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
package ubic.gemma.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;

/**
 * Tools to help make regression testing easier — comparing matrices, arrays, and collections within a tolerance.
 * <p>
 * In-tree port of {@code ubic.basecode.util.RegressionTesting} (Renovations Phase 3 baseCode util batch 3).
 * The cern.colt-heavy overloads ({@code DoubleArrayList}, {@code DoubleMatrix1D}, {@code DoubleMatrix2D}) and the
 * file I/O helpers ({@code readTestResult}, {@code writeTestResult}, {@code readTestResultFromFile}) were dropped —
 * none are called from Gemma. The remaining overloads kept here cover every Gemma call site:
 * {@code closeEnough(double[], double[], double)}, {@code closeEnough(DoubleMatrix, DoubleMatrix, double)}, and
 * {@code containsSame(Collection, Collection)}. Other simple overloads ({@code Object[]}, {@code int[]},
 * {@code double[]} {@code containsSame}, {@code sameArray}) are retained because they're self-contained and trivial.
 *
 * @author pavlidis
 */
public class RegressionTesting {

    private static final Logger log = LoggerFactory.getLogger( RegressionTesting.class );

    private RegressionTesting() {
        // block instantiation
    }

    /**
     * @param expected  expected values
     * @param actual    actual values
     * @param tolerance permitted delta between the values
     * @return true if all paired values agree within tolerance
     */
    public static boolean closeEnough( double[] expected, double[] actual, double tolerance ) {
        if ( expected.length != actual.length ) return false;

        for ( int i = 0; i < expected.length; i++ ) {
            if ( Math.abs( expected[i] - actual[i] ) > tolerance ) {
                log.error( "Expected " + expected[i] + " got " + actual[i] + " at " + i );
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether two {@link DoubleMatrix}es are 'close enough' to call equal.
     *
     * @return true if all the values in both matrices are within 'tolerance' of each other.
     */
    public static boolean closeEnough( DoubleMatrix<?, ?> expected, DoubleMatrix<?, ?> actual, double tolerance ) {
        if ( expected.rows() != actual.rows() || expected.columns() != actual.columns() ) {
            log.error( "Unequal rows and/or columns" );
            return false;
        }

        for ( int i = 0; i < expected.rows(); i++ ) {
            for ( int j = 0; j < expected.columns(); j++ ) {
                if ( Math.abs( expected.get( i, j ) - actual.get( i, j ) ) > tolerance ) {
                    log.error( "Expected: " + expected.get( i, j ) + ", actual=" + actual.get( i, j ) );
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Test whether two object arrays are the same (element-wise, ordered).
     */
    public static boolean closeEnough( Object[] a, Object[] b ) {
        if ( a.length != b.length ) {
            return false;
        }

        for ( int i = 0; i < a.length; i++ ) {
            if ( !a[i].equals( b[i] ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether two collections contain the same items.
     */
    public static boolean containsSame( Collection<? extends Object> a, Collection<? extends Object> b ) {
        if ( a.size() != b.size() ) return false;
        if ( !a.containsAll( b ) ) return false;
        return true;
    }

    /**
     * Test whether two double arrays contain the same items in any order (tolerance is ZERO).
     */
    public static boolean containsSame( double[] a, double[] b ) {
        if ( a.length != b.length ) return false;

        List<Double> av = new ArrayList<>( a.length );
        List<Double> bv = new ArrayList<>( b.length );
        for ( int i = 0; i < b.length; i++ ) {
            av.add( a[i] );
            bv.add( b[i] );
        }

        return av.containsAll( bv );
    }

    /**
     * Test whether two object arrays contain the same items in any order. The arrays are treated as Sets — repeats
     * are not considered.
     */
    public static boolean containsSame( Object[] a, Object[] b ) {
        if ( a.length != b.length ) return false;

        List<Object> av = new ArrayList<>( a.length );
        List<Object> bv = new ArrayList<>( b.length );

        for ( int i = 0; i < b.length; i++ ) {
            av.add( a[i] );
            bv.add( b[i] );
        }

        return av.containsAll( bv );
    }

    /**
     * Test whether two int arrays contain the same items in the same order.
     */
    public static boolean sameArray( int[] a, int[] b ) {
        if ( a.length != b.length ) return false;
        for ( int i = 0; i < b.length; i++ ) {
            if ( b[i] != a[i] ) return false;
        }
        return true;
    }
}

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
package ubic.gemma.core.util.math;

import java.util.ArrayList;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;

/**
 * Methods for p-value correction of sets of hypothesis tests.
 * <p>
 * FIXME make this API more consistent.
 * 
 * @author Paul Pavlidis
 * 
 */
public class MultipleTestCorrection {

    /**
     * @param pvalues
     * @return false discovery rates (FDR) computed using the method of Benjamini and Hochberg. The order is the same as
     *         the original pvalues (that is, each value fdr[i] corresponds to the pvaluesp[i].
     */
    public static DoubleArrayList benjaminiHochberg( DoubleArrayList pvalues ) {
        int nump = pvalues.size();
        int n = nump;

        IntArrayList order = Rank.order( pvalues );

        DoubleMatrix1D tmp = new DenseDoubleMatrix1D( nump );

        DoubleArrayList sorted = pvalues.copy();
        sorted.sort();

        double previous = 1.0;
        for ( int i = sorted.size() - 1; i >= 0; i-- ) {
            double pval = sorted.get( i );
            // never let the qvalue increase.
            double qval = Math.min( pval * nump / n, previous );
            tmp.set( i, qval );
            previous = qval;
            n--;
        }
        DoubleArrayList results = new DoubleArrayList( nump );
        for ( int i = 0; i < nump; i++ ) {
            results.add( 0.0 );
        }
        for ( int i = 0; i < nump; i++ ) {
            results.set( order.get( i ), tmp.get( i ) );
        }

        return results;
    }

    /**
     * @param pvalues; can contain missing values or invalid pvalues (outside range [0-1]), which are ignored.
     * @return false discovery rates (FDRs) computed using the method of Benjamini and Hochberg; or null if they could
     *         not be computed. The order is the same as the original pvalues (that is, each value fdr[i] corresponds to
     *         the pvaluesp[i].
     */
    public static DoubleMatrix1D benjaminiHochberg( DoubleMatrix1D pvalues ) {
        double[] qvalues = new double[pvalues.size()];

        /* Create a list with only the p-values that are not Double.NaN */
        ArrayList<Double> pvaluesList = new ArrayList<Double>();
        for ( int i = 0; i < pvalues.size(); i++ ) {
            qvalues[i] = Double.NaN; // initialize.

            Double pvalue = pvalues.getQuick( i );
            if ( pvalue < 0.0 || pvalue > 1.0 || Double.isNaN( pvalue ) ) continue;
            pvaluesList.add( pvalue );
        }

        if ( pvaluesList.isEmpty() ) {
            return null;
        }

        /* convert to primitive array */
        double[] pvaluesToUse = new double[pvaluesList.size()];
        int j = 0;
        for ( Double d : pvaluesList ) {
            pvaluesToUse[j] = d;
            j++;
        }

        DoubleArrayList r = benjaminiHochberg( new DoubleArrayList( pvaluesToUse ) );

        /* Add the Double.NaN back in */
        int k = 0;
        for ( int i = 0; i < qvalues.length; i++ ) {
            Double pvalue = pvalues.get( i );
            if ( pvalue < 0.0 || pvalue > 1.0 || Double.isNaN( pvalue ) ) {
                qvalues[i] = Double.NaN;
            } else {
                qvalues[i] = r.get( k );
                k++;
            }
        }

        return new DenseDoubleMatrix1D( qvalues );
    }

    /**
     * Benjamini-Hochberg method. Determines the maximum p value to maintain the false discovery rate. (Assuming pvalues
     * are independent);
     * 
     * @param pvalues list of pvalues. Need not be sorted.
     * @param fdr false discovery rate (value q in B-H).
     * @return The maximum pvalue that maintains the false discovery rate
     * @throws IllegalArgumentException if invalid pvalues are encountered.
     */
    public static double benjaminiHochbergCut( DoubleArrayList pvalues, double fdr ) {
        int numpvals = pvalues.size();
        DoubleArrayList pvalcop = pvalues.copy();
        pvalcop.sort();

        for ( int i = numpvals - 1; i >= 0; i-- ) {

            double p = pvalcop.get( i );
            if ( p < 0.0 || p > 1.0 ) throw new IllegalArgumentException( "p-value must be in range [0,1]" );

            double thresh = fdr * i / numpvals;
            if ( p < thresh ) {
                return p;
            }
        }
        return 0.0;
    }

    /**
     * Benjamini-Yekuteli method. Determines the maximum p value to maintain the false discovery rate. Valid under
     * dependence of the pvalues. This method is more conservative than Benjamini-Hochberg.
     * 
     * @param pvalues list of pvalues. Need not be sorted.
     * @param fdr false discovery rate (value q in B-H).
     * @return The maximum pvalue that maintains the false discovery rate
     * @throws IllegalArgumentException if invalid pvalues are encountered.
     */
    public static double BenjaminiYekuteliCut( DoubleArrayList pvalues, double fdr ) {

        int numpvals = pvalues.size();
        DoubleArrayList pvalcop = pvalues.copy();
        pvalcop.sort();

        // as per Theorem 1.3 in paper.
        // qmod replaces q (fdr) in the method
        double qmod = 0.0;
        for ( int i = 1; i <= numpvals; i++ ) {
            qmod += 1.0 / i;
        }
        qmod = fdr / qmod;

        for ( int i = numpvals - 1; i >= 0; i-- ) {

            double p = pvalcop.get( i );
            if ( p < 0.0 || p > 1.0 ) {
                throw new IllegalArgumentException( "p-value must be in range [0,1], got: " + p );
            }

            double thresh = qmod * i / numpvals;
            if ( p < thresh ) {
                return p;
            }
        }
        return 0.0;
    }

    /**
     * Determine the Bonferroni pvalue threshold to maintain the family wise error rate (assuming pvalues are
     * independent).
     * 
     * @param pvalues The pvalues
     * @param fwe The family wise error rate
     * @return The minimum pvalue that maintains the FWE
     */
    public static double BonferroniCut( DoubleArrayList pvalues, double fwe ) {
        int numpvals = pvalues.size();
        return fwe / numpvals;
    }

}
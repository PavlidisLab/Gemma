/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.analysis.expression.coexpression.links;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ubic.basecode.math.Rank;
import cern.colt.list.DoubleArrayList;

/**
 * @author paul
 * @version $Id$
 */
public class SpearmanMetricsTest {

    /**
     * Value from R; this has ties.
     * 
     * <pre>
     * &gt; a&lt;-c(49.0, 43.0, 310.0, 20.0, 20.0, 688.0, 498.0, 533.0, 723.0, 1409.0,279.0);
     * &gt; b&lt;-c(1545.0, 1287.0, 2072.0, 1113.0, 676.0, 2648.0, 2478.0, 2574.0, 3554.0,5155.0, 1624.0);
     * &gt; cor(a,b, method=&quot;spearman&quot;);
     * [1] 0.9977247
     * </pre>
     */
    @Test
    public void testCorrel() {

        // note the nominal tie in one (20)
        double[] a = new double[] { 49.0, 43.0, 310.0, 20.0, 20.0, 688.0, 498.0, 533.0, 723.0, 1409.0, 279.0 };
        double[] b = new double[] { 1545.0, 1287.0, 2072.0, 1113.0, 676.0, 2648.0, 2478.0, 2574.0, 3554.0, 5155.0,
                1624.0 };

        boolean[] usedA = new boolean[] { true, true, true, true, true, true, true, true, true, true, true };
        boolean[] usedB = new boolean[] { true, true, true, true, true, true, true, true, true, true, true };

        assertEquals( a.length, usedA.length );
        assertEquals( b.length, usedB.length );

        DoubleArrayList ranksIA = Rank.rankTransform( new DoubleArrayList( a ) );
        DoubleArrayList ranksIB = Rank.rankTransform( new DoubleArrayList( b ) );

        SpearmanMetrics test = new SpearmanMetrics( 10 );

        double actualValue = test.spearman( ranksIA.elements(), ranksIB.elements(), usedA, usedB, 0, 1 );
        double expectedValue = 0.9977247;
        assertEquals( expectedValue, actualValue, 0.0001 );

    }

    /**
     * This tests the same values as testCorrelWithMissing, different method than testCorrelC
     */
    @Test
    public void testCorrelB() {
        boolean[] usedA = new boolean[] { true, true, true, true, true, true, true, true, true };
        boolean[] usedB = new boolean[] { true, true, true, true, true, true, true, true, true };
        double[] a = new double[] { 400, 310, 20, 20, 688, 498, 533, 1409, 1500 };
        double[] b = new double[] { 1545, 2072, 1113, 676, 2648, 2478, 2574, 5155, 1624 };
        assertEquals( a.length, b.length );
        assertEquals( a.length, usedA.length );
        assertEquals( b.length, usedB.length );

        DoubleArrayList ranksIA = Rank.rankTransform( new DoubleArrayList( a ) );
        DoubleArrayList ranksIB = Rank.rankTransform( new DoubleArrayList( b ) );

        SpearmanMetrics test = new SpearmanMetrics( 10 );

        double actualValue = test.spearman( ranksIA.elements(), ranksIB.elements(), usedA, usedB, 0, 1 );
        double expectedValue = 0.7113033;
        assertEquals( expectedValue, actualValue, 0.0001 );
    }

    /**
     * Without missing values, fast method (same data as testCorrelB)
     */
    @Test
    public void testCorrelC() {
        double[] a = new double[] { 400, 310, 20, 20, 688, 498, 533, 1409, 1500 };
        double[] b = new double[] { 1545, 2072, 1113, 676, 2648, 2478, 2574, 5155, 1624 };
        assertEquals( a.length, b.length );

        DoubleArrayList ranksIA = Rank.rankTransform( new DoubleArrayList( a ) );
        DoubleArrayList ranksIB = Rank.rankTransform( new DoubleArrayList( b ) );

        SpearmanMetrics test = new SpearmanMetrics( 10 );

        double actualValue = test.correlFast( ranksIA.elements(), ranksIB.elements(), 7.713624, 7.745967, 5, 5 );
        double expectedValue = 0.7113033;
        assertEquals( expectedValue, actualValue, 0.0001 );
    }

    @Test
    public void testCorrelFromRanks() {

        double[] a = new double[] { 4.0, 3.0, 6.0, 1.5, 1.5, 9.0, 7.0, 8.0, 10.0, 11.0, 5.0 };
        double[] b = new double[] { 4.0, 3.0, 6.0, 2, 1, 9, 7, 8, 10, 11, 5 };

        boolean[] usedA = new boolean[] { true, true, true, true, true, true, true, true, true, true, true, true, true,
                true, true, true };
        boolean[] usedB = new boolean[] { true, true, true, true, true, true, true, true, true, true, true, true, true,
                true, true, true };

        SpearmanMetrics test = new SpearmanMetrics( 10 );

        double actualValue = test.spearman( a, b, usedA, usedB, 0, 1 );
        double expectedValue = 0.9977247;
        assertEquals( expectedValue, actualValue, 0.00001 );

    }

    /**
     * See testCorrelB for the same numbers tested a different way (with no missing values)
     */
    @Test
    public void testCorrelWithMissing() {

        // note the nominal tie in one (20)
        double[] a = new double[] { 400, 43, 310, 20, 20, 688, 498, 533, 723, 1409, 1500 };
        double[] b = new double[] { 1545, 1287, 2072, 1113, 676, 2648, 2478, 2574, 3554, 5155, 1624 };
        boolean[] usedA = new boolean[] { true, false, true, true, true, true, true, true, false, true, true };
        boolean[] usedB = new boolean[] { true, true, true, true, true, true, true, true, false, true, true };

        assertEquals( a.length, b.length );
        assertEquals( a.length, usedA.length );
        assertEquals( b.length, usedB.length );

        DoubleArrayList ranksIA = Rank.rankTransform( new DoubleArrayList( a ) );
        DoubleArrayList ranksIB = Rank.rankTransform( new DoubleArrayList( b ) );

        SpearmanMetrics test = new SpearmanMetrics( 10 );

        double actualValue = test.spearman( ranksIA.elements(), ranksIB.elements(), usedA, usedB, 0, 1 );
        double expectedValue = 0.7113033;
        assertEquals( expectedValue, actualValue, 0.0001 );

    }
}

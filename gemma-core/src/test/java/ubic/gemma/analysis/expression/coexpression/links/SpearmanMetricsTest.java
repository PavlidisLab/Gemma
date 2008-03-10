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
package ubic.gemma.analysis.expression.coexpression.links;

import ubic.basecode.math.Rank;
import ubic.gemma.analysis.expression.coexpression.links.SpearmanMetrics;
import cern.colt.list.DoubleArrayList;
import junit.framework.TestCase;

/**
 * @author paul
 * @version $Id$
 */
public class SpearmanMetricsTest extends TestCase {

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
    public void testCorrelFast() {

        // note the nominal tie
        double[] a = new double[] { 49.0, 43.0, 310.0, 20.0, 20.0, 688.0, 498.0, 533.0, 723.0, 1409.0, 279.0 };
        double[] b = new double[] { 1545.0, 1287.0, 2072.0, 1113.0, 676.0, 2648.0, 2478.0, 2574.0, 3554.0, 5155.0,
                1624.0 };

        DoubleArrayList ranksIA = Rank.rankTransform( new DoubleArrayList( a ) );
        DoubleArrayList ranksIB = Rank.rankTransform( new DoubleArrayList( b ) );

        double denom = ( Math.pow( a.length, 2 ) - 1 ) * a.length;

        SpearmanMetrics test = new SpearmanMetrics( 1 );

        double actualValue = test.correlFast( ranksIA.elements(), ranksIB.elements(), denom );
        double expectedValue = 0.9977247;
        assertEquals( expectedValue, actualValue, 0.0001 );

    }

    public void testCorrelFastFromRanks() {

        double[] a = new double[] { 4.0, 3.0, 6.0, 1.5, 1.5, 9.0, 7.0, 8.0, 10.0, 11.0, 5.0 };
        double[] b = new double[] { 4.0, 3.0, 6.0, 2, 1, 9, 7, 8, 10, 11, 5 };

        double denom = Math.pow( a.length, 3 ) - a.length;

        SpearmanMetrics test = new SpearmanMetrics( 1 );

        double actualValue = test.correlFast( a, b, denom );
        double expectedValue = 0.9977247;
        assertEquals( expectedValue, actualValue, 0.00001 );

    }
}

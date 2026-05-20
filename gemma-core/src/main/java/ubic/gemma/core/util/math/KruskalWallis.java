/*
 * The baseCode project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.util.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.jet.stat.Probability;

/**
 * Perform a Kruskal-Wallis test.
 * 
 * @author paul
 * 
 */
public class KruskalWallis {

    /**
     * @param scores
     * @param groupings
     * @return number of degrees of freedom (number of groups - 1)
     */
    public static int dof( DoubleArrayList scores, IntArrayList groupings ) {
        return groupedRanks( groupings, scores ).size() - 1;
    }

    /**
     * @param scores
     * @param groupings
     * @return statistic; chi-squared with numgroups - 1 dof under the null.
     */
    public static double kwStatistic( DoubleArrayList scores, IntArrayList groupings ) {
        Map<Integer, Collection<Integer>> groupedRanks = groupedRanks( groupings, scores );
        int n = scores.size();
        return kwStatistic( n, groupedRanks );
    }

    /**
     * Perform a Kruskal-Wallis one-way ANOVA.
     * <p>
     * Implementation note: Does not make special corrections for ties, though ties are given the averaged ranks.
     * Completely bare-bones. Missing values are not tolerated well.
     * 
     * @param scores
     * @param groupings integer indicators of which values are in which groups. The actual values don't matter.
     * @return p-values based on the chi-squared statistic.
     */
    public static double test( DoubleArrayList scores, IntArrayList groupings ) {

        assert scores != null && groupings != null && scores.size() == groupings.size();

        assert scores.size() > 2;

        int n = scores.size();

        Map<Integer, Collection<Integer>> groupedRanks = groupedRanks( groupings, scores );

        int numGroups = groupedRanks.size();

        if ( numGroups < 2 ) {
            throw new IllegalArgumentException( "Must have at least two group" );
        }

        double k = kwStatistic( n, groupedRanks );

        return Probability.chiSquareComplemented( numGroups - 1, k );
    }

    /**
     * @param groupings
     * @param ranks
     * @param n
     * @return
     */
    private static Map<Integer, Collection<Integer>> groupedRanks( IntArrayList groupings, DoubleArrayList scores ) {
        /*
         * See for example http://en.wikipedia.org/wiki/Kruskal%E2%80%93Wallis_one-way_analysis_of_variance
         */
        DoubleArrayList ranks = Rank.rankTransform( scores );
        assert ranks != null;

        Map<Integer, Collection<Integer>> groupedRanks = new HashMap<Integer, Collection<Integer>>();
        for ( int i = 0; i < groupings.size(); i++ ) {
            Integer group = ( int ) groupings.get( i );

            if ( !groupedRanks.containsKey( group ) ) {
                groupedRanks.put( group, new ArrayList<Integer>() );
            }
            groupedRanks.get( group ).add( ( int ) ranks.get( i ) );

        }
        return groupedRanks;
    }

    /**
     * @param n
     * @param groupedRanks
     * @return statistic; chi-squared with numgroups - 1 dof under the null.
     */
    private static double kwStatistic( int n, Map<Integer, Collection<Integer>> groupedRanks ) {
        double scale = 12.0 / ( n * ( n + 1.0 ) );
        double sum = 0.0;
        for ( Integer g : groupedRanks.keySet() ) {

            Collection<Integer> gr = groupedRanks.get( g );

            double m = gr.size();

            double groupMeanRank = 0.0;
            for ( Integer r : gr ) {
                groupMeanRank += r;
            }
            groupMeanRank /= m;

            double term = m * Math.pow( groupMeanRank - ( ( n + 1.0 ) / 2.0 ), 2 );

            sum += term;
        }

        double k = scale * sum;
        return k;
    }
}

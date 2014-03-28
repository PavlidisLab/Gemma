/*
 * The gemma-core project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.analysis.expression.coexpression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ubic.basecode.dataStructure.CountingMap;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class CoexpressionUtils {
    /**
     * @param nodeDegreeDist computed with CoexpressionUtils.getNodeDegrees
     * @param nodeDegree
     * @return genes that have the exact requested node degree
     */
    static Set<Long> getGenesWithNodeDegree( Map<Long, Integer> nodeDegreeDist, int nodeDegree ) {
        Set<Long> result = new HashSet<>();
        for ( Long g : nodeDegreeDist.keySet() ) {
            if ( nodeDegreeDist.get( g ) == nodeDegree ) {
                result.add( g );
            }
        }
        return result;
    }

    /**
     * @param geneResults
     * @return Map of gene ids to how many links that gene has
     */
    static CountingMap<Long> getNodeDegrees( List<CoexpressionValueObjectExt> geneResults ) {
        CountingMap<Long> result = new CountingMap<>();
        for ( CoexpressionValueObjectExt cvoe : geneResults ) {
            result.increment( cvoe.getQueryGene().getId() );
            result.increment( cvoe.getFoundGene().getId() );
        }
        return result;

    }

    /**
     * @param geneResults
     * @return a map of degree; value is how many genes have exactly that degree. Values of degree not used are not
     *         present as keys.
     */
    static CountingMap<Integer> getNodeDegreeDistribution( List<CoexpressionValueObjectExt> geneResults ) {
        CountingMap<Integer> result = new CountingMap<>();

        CountingMap<Long> nd = getNodeDegrees( geneResults );
        for ( Long g : nd.keySet() ) {
            result.increment( nd.get( g ) );
        }
        return result;

    }

    /**
     * @param geneResults
     * @return a map of support level; value is how many links have exactly that level of support. Values of support not
     *         used are not present as keys.
     */
    static CountingMap<Integer> getSupportDistribution( List<CoexpressionValueObjectExt> geneResults ) {
        CountingMap<Integer> result = new CountingMap<>();
        for ( CoexpressionValueObjectExt cvoe : geneResults ) {
            Integer support = cvoe.getSupport();
            result.increment( support );
        }
        return result;
    }

    /**
     * Call on results of getSupportDistribution or getNodeDegreeDistribution to get a cumulative distribution (starting
     * from 1)
     * 
     * @param dist
     * @return Map of keys starting from one (1), up to the maximum, to values of the cumulative total of the
     *         distribution
     */
    static Map<Integer, Integer> cumulate( CountingMap<Integer> dist ) {
        Map<Integer, Integer> result = new HashMap<>();
        int max = dist.max();
        for ( int i = 1; i <= max; i++ ) {
            int v = 0;
            if ( i > 1 ) {
                v = result.get( i ) == null ? result.get( i ) : 0;
            }
            result.put( i, v );
        }
        return result;
    }

}

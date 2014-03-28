/*
 * The gemma-core project
 * 
 * Copyright (c) 2014 University of British Columbia
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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;

/**
 * Provides access to Gene2Gene links. The use of this service provides 'high-level' access to functionality in the
 * Gene2GeneCoexpressionService.
 * 
 * @author Paul
 * @version $Id$
 */
public interface GeneCoexpressionSearchService {

    /**
     * The smallest support value dealt with by the system. (in the old days, this was 2, but it's a bit of an artifact
     * now)
     */
    public static final int MINIMUM_SUPPORT = 1;

    /**
     * @param inputEeIds Expression experiments ids to consider; if null, use all available data.
     * @param genes Genes to find coexpression for
     * @param stringency Minimum support level
     * @param maxResults per gene
     * @param queryGenesOnly Whether to return only coexpression among the query genes (assuming there are more than
     *        one). Otherwise, coexpression with genes 'external' to the queries will be returned.
     * @return CoexpressionMetaValueObject in which the results are already populated and sorted.
     */
    public abstract CoexpressionMetaValueObject coexpressionSearch( Collection<Long> inputEeIds,
            Collection<Long> genes, int stringency, int maxResults, boolean queryGenesOnly );

    /**
     * Skips some of the postprocessing steps, use in situations where raw speed is more important than details.
     * 
     * @param inputEeIds Expression experiments ids to consider; if null or empty, use all available data.
     * @param genes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @return CoexpressionMetaValueObject in which the results are already populated and sorted.
     */
    public abstract CoexpressionMetaValueObject coexpressionSearchQuick( Collection<Long> inputEeIds,
            Collection<Long> genes, int stringency, int maxResults, boolean queryGenesOnly );

}
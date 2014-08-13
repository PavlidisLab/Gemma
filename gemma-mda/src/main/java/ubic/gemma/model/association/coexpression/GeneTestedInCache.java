/*
 * The gemma-mda project
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

package ubic.gemma.model.association.coexpression;

import java.util.Map;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionTestedIn;

/**
 * Cache of the 'tested-in' information for genes. As new analyses are done, this cache must be invalidated but
 * otherwise can speed up the post-processing steps.
 * 
 * @author paul
 * @version $Id$
 */
public interface GeneTestedInCache {

    public void cacheTestedIn( GeneCoexpressionTestedIn testedIn );

    public void clearCache();

    public GeneCoexpressionTestedIn get( Long geneId );

    /**
     * @param idMap
     */
    public void cache( Map<Long, GeneCoexpressionTestedIn> idMap );

    /**
     * @param queryGeneId
     * @return
     */
    public boolean contains( Long queryGeneId );

    /**
     * @param id
     */
    public void remove( Long id );

}

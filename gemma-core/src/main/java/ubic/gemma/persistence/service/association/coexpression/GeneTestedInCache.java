/*
 * The gemma project
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

package ubic.gemma.persistence.service.association.coexpression;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionTestedIn;

import java.util.Map;

/**
 * Cache of the 'tested-in' information for genes. As new analyses are done, this cache must be invalidated but
 * otherwise can speed up the post-processing steps.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface GeneTestedInCache {

    void cacheTestedIn( GeneCoexpressionTestedIn testedIn );

    void clearCache();

    @Nullable
    GeneCoexpressionTestedIn get( Long geneId );

    void cache( Map<Long, GeneCoexpressionTestedIn> idMap );

    boolean contains( Long queryGeneId );

    void remove( Long id );

}

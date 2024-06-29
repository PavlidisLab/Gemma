/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.analysis.expression.diff.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;

import java.util.Collection;
import java.util.List;

/**
 * Cache for differential expression results. This actually manages two caches; one is for resultset x gene results,
 * requested for the (typically) main visualization and meta-analysis. The second is of the "top hits" for a resultset.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface DifferentialExpressionResultCache {

    void addToCache( DiffExprGeneSearchResult diffExForCache );

    void addToCache( Collection<DiffExprGeneSearchResult> diffExForCache );

    void clearCache();

    /**
     * Remove all elements from the cache for the given result set, if the cache exists.
     *
     * @param resultSetId - specific cache to be cleared.
     */
    void clearCache( Long resultSetId );

    /**
     * Remove all elements from the top hits cache for the given result set, if the cache exists.
     *
     * @param resultSetId id
     */
    void clearTopHitCache( Long resultSetId );

    Collection<DiffExprGeneSearchResult> get( Long resultSet, Collection<Long> genes );

    @Nullable
    DiffExprGeneSearchResult get( Long resultSet, Long g );

    boolean isEnabled();

    void setEnabled( boolean enabled );

    void addToTopHitsCache( ExpressionAnalysisResultSet resultSet, List<DifferentialExpressionValueObject> items );

    /**
     * @param resultSet result set
     * @return top hits, or null.
     */
    @Nullable
    List<DifferentialExpressionValueObject> getTopHits( ExpressionAnalysisResultSet resultSet );
}

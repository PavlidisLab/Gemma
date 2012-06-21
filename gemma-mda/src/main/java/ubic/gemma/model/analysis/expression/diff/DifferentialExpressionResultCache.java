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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;


/**
 * Cache for differential expression results.
 * 
 * @author paul
 * @version $Id$
 */
public interface DifferentialExpressionResultCache {

    /**
     * @param diffExForCache
     */
    public abstract void addToCache( DiffExprGeneSearchResult diffExForCache );

    public abstract void addToCache( Collection<DiffExprGeneSearchResult> diffExForCache );

    /**
     * 
     */
    public abstract void clearCache();

    /**
     * Remove all elements from the cache for the given result set, if the cache exists.
     * 
     * @param e the resultSetId - specific cache to be cleared.
     */
    public abstract void clearCache( Long resultSetId );

    /**
     * @param resultSetId
     * @param genes
     * @return
     */
    public abstract Collection<DiffExprGeneSearchResult> get( Long resultSet, Collection<Long> genes );

    /**
     * @param resultSet
     * @param g
     * @return
     */
    public abstract DiffExprGeneSearchResult get( Long resultSet, Long g );

    /**
     * @return the enabled
     */
    public abstract Boolean isEnabled();

    /**
     * @param enabled the enabled to set
     */
    public abstract void setEnabled( Boolean enabled );

}

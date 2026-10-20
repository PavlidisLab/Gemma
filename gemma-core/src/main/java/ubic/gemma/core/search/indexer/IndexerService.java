/*
 * The Gemma project
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
package ubic.gemma.core.search.indexer;

import ubic.gemma.model.common.Identifiable;

/**
 * Drives Hibernate Search 7's mass-indexer (built-in
 * {@link org.hibernate.search.mapper.orm.massindexing.MassIndexer}) across the
 * {@code @Indexed} entity roots Gemma actually searches.
 * <p>
 * This is the HS 7 successor to the pre-Phase-2 {@code IndexerService} (HS 5 era):
 * the underlying call switched from {@code FullTextSession.createIndexer(Class)} to
 * {@code Search.session(session).massIndexer(Class)}. See SEARCH_RECCE.md
 * Section 2.6 / Section 4 Step 4.
 *
 * @author poirigui
 */
public interface IndexerService {

    /**
     * Reindex the given entity class with the default number of loader threads.
     * <p>
     * This is a destructive operation: by default the existing on-disk index for
     * {@code classToIndex} is purged before the rebuild ({@code purgeAllOnStart(true)}).
     */
    void index( Class<? extends Identifiable> classToIndex );

    /**
     * Reindex the given entity class with an explicit number of loader threads.
     */
    void index( Class<? extends Identifiable> classToIndex, int numThreads );
}

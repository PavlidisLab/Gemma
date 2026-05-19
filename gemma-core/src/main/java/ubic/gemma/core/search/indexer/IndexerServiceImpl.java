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

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.Identifiable;

/**
 * Drives Hibernate Search 7's {@link MassIndexer} for one entity class at a time.
 *
 * <p>HS 5 → HS 7 API delta:
 * <ul>
 *   <li>{@code Search.getFullTextSession(session).createIndexer(Class).startAndWait()}
 *       → {@code Search.session(session).massIndexer(Class).startAndWait()}.</li>
 *   <li>The HS 5 progress monitor (custom {@code MassIndexerProgressMonitor}) is replaced
 *       by HS 7's built-in {@code monitor(...)} hook; we use the default monitor (logs
 *       INFO messages periodically) — its output is identical in spirit to the bespoke
 *       counter the HS 5 impl wired up.</li>
 *   <li>{@code Future} polling loop dropped — {@code startAndWait()} blocks the calling
 *       thread until indexing is complete, which is what the CLI wants.</li>
 * </ul>
 *
 * <p>Knobs we use (sensible defaults matching the HS 5 era):
 * <ul>
 *   <li>{@link MassIndexer#threadsToLoadObjects(int)} — # of threads loading entities
 *       from MySQL (default 4, can be overridden per call).</li>
 *   <li>{@link MassIndexer#batchSizeToLoadObjects(int)} — JDBC fetch batch (25).</li>
 *   <li>{@link MassIndexer#idFetchSize(int)} — driver-level fetch size for the id query
 *       (Integer.MIN_VALUE = streaming for MySQL).</li>
 *   <li>{@link MassIndexer#mergeSegmentsOnFinish(boolean)} — squash Lucene segments after
 *       the rebuild for faster subsequent search ({@code true}).</li>
 *   <li>{@link MassIndexer#purgeAllOnStart(boolean)} — wipe the existing index for this
 *       entity before rebuilding ({@code true}; the safe + correct default).</li>
 * </ul>
 *
 * <p>See SEARCH_RECCE.md Section 2.6 / Section 4 Step 4.
 *
 * @author poirigui
 */
@Service
@CommonsLog
public class IndexerServiceImpl implements IndexerService {

    private static final int DEFAULT_NUM_THREADS = 4;
    private static final int DEFAULT_BATCH_SIZE = 25;
    /**
     * MySQL streaming fetch size. {@code Integer.MIN_VALUE} tells the MySQL JDBC driver
     * to stream rows one at a time rather than buffer the entire result set in memory —
     * essential for tables like {@code GENE} or {@code COMPOSITE_SEQUENCE} with millions
     * of rows.
     */
    private static final int DEFAULT_ID_FETCH_SIZE = Integer.MIN_VALUE;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public void index( Class<? extends Identifiable> classToIndex ) {
        index( classToIndex, DEFAULT_NUM_THREADS );
    }

    @Override
    public void index( Class<? extends Identifiable> classToIndex, int numThreads ) {
        // openSession() rather than getCurrentSession(): mass indexing runs outside any
        // surrounding Spring transaction (it manages its own connections and tx
        // semantics), and we must not contaminate a request-scoped session.
        try ( Session session = sessionFactory.openSession() ) {
            SearchSession searchSession = Search.session( session );
            MassIndexer indexer = searchSession.massIndexer( classToIndex )
                    .threadsToLoadObjects( numThreads )
                    .batchSizeToLoadObjects( DEFAULT_BATCH_SIZE )
                    .idFetchSize( DEFAULT_ID_FETCH_SIZE )
                    .mergeSegmentsOnFinish( true )
                    .purgeAllOnStart( true );
            log.info( "Indexing of " + classToIndex.getName() + " has started (threads=" + numThreads + ")..." );
            try {
                indexer.startAndWait();
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new RuntimeException( "Mass-indexing of " + classToIndex.getName() + " was interrupted.", e );
            }
            log.info( "Done indexing " + classToIndex.getName() + "." );
        }
    }
}

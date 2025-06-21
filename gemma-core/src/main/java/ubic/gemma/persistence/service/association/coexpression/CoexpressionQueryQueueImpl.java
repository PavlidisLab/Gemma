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

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.genome.GeneDao;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Paul
 */
@CommonsLog
class CoexpressionQueryQueueImpl implements CoexpressionQueryQueue, InitializingBean, DisposableBean {

    private static final int QUEUE_SIZE = 1000;
    private final BlockingQueue<Long> geneQueue = new ArrayBlockingQueue<>(
            CoexpressionQueryQueueImpl.QUEUE_SIZE );

    private final CoexpressionDao coexpressionDao;
    private final GeneDao geneDao;

    private final ExecutorService queryCacheExecutor = Executors.newSingleThreadExecutor();

    public CoexpressionQueryQueueImpl( CoexpressionDao coexpressionDao, GeneDao geneDao ) {
        this.coexpressionDao = coexpressionDao;
        this.geneDao = geneDao;
    }

    @Override
    public void addToFullQueryQueue( Collection<Gene> genes ) {
        for ( Gene gene : genes ) {
            if ( !this.addToFullQueryQueue( gene.getId() ) ) {
                break;
            }
        }
    }

    @Override
    public void addToFullQueryQueue( Gene gene ) {
        this.addToFullQueryQueue( gene.getId() );
    }

    @Override
    public void removeFromQueue( Collection<Gene> genes ) {
        int count = 0;
        for ( Gene gene : genes ) {
            if ( geneQueue.remove( gene.getId() ) )
                count++;
        }

        if ( count > 0 ) {
            CoexpressionQueryQueueImpl.log.info( count + " genes removed from the query queue" );
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        queryCacheExecutor.submit( new Runnable() {

            private static final int MAX_WARNINGS = 5;

            @Override
            public void run() {
                int numWarnings = 0;
                while ( true ) {
                    Long gene;
                    try {
                        gene = geneQueue.poll( 1000, TimeUnit.MILLISECONDS );
                        if ( gene != null ) {
                            CoexpressionQueryQueueImpl.this.queryForCache( gene );
                        }
                    } catch ( InterruptedException e ) {
                        Thread.currentThread().interrupt();
                        log.warn( "Coexpression query cache was interrupted." );
                        break;
                    } catch ( Exception e ) {
                        // can happen during tests
                        if ( numWarnings < MAX_WARNINGS ) {
                            CoexpressionQueryQueueImpl.log
                                    .error( "Error while caching coexpression: " + e.getMessage() );
                        } else if ( numWarnings == MAX_WARNINGS ) {
                            CoexpressionQueryQueueImpl.log.error( "Further warnings suppressed" );
                        }
                        numWarnings++;
                    }
                }
            }
        } );
    }

    @Override
    public void destroy() throws Exception {
        // filling the coexpression cache is not really critical, so we can safely interrupt it
        queryCacheExecutor.shutdownNow();
    }

    private void queryForCache( Long geneId ) {
        Gene g = Objects.requireNonNull( geneDao.load( geneId ),
                String.format( "No Gene with ID %d.", geneId ) );
        int numCached = coexpressionDao.queryAndCache( g );
        //noinspection StatementWithEmptyBody // Better readability
        if ( numCached < 0 ) {
            // it was already in the cache
        } else if ( numCached > 0 ) {
            CoexpressionQueryQueueImpl.log.debug( "Cached " + numCached + " coexpression links at stringency="
                    + CoexpressionCache.CACHE_QUERY_STRINGENCY + " for " + geneId );
        } else {
            CoexpressionQueryQueueImpl.log
                    .debug( "No coexpression links to cache at stringency=" + CoexpressionCache.CACHE_QUERY_STRINGENCY
                            + " for " + geneId );
        }
    }

    private boolean addToFullQueryQueue( Long geneId ) {
        if ( this.geneQueue.offer( geneId ) ) {
            CoexpressionQueryQueueImpl.log.debug( "Queuing gene=" + geneId + " for cache warm" );
        } else {
            CoexpressionQueryQueueImpl.log.debug( "Queue is full, cannot add genes for cache warm." );
        }
        return false;
    }
}
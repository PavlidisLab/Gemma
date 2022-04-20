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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.genome.GeneDao;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Paul
 */
// @Repository
class CoexpressionQueryQueueImpl implements CoexpressionQueryQueue, InitializingBean {

    private static final int QUEUE_SIZE = 1000;
    private static final Logger log = LoggerFactory.getLogger( CoexpressionQueryQueueImpl.class );
    private final BlockingQueue<QueuedGene> geneQueue = new ArrayBlockingQueue<>(
            CoexpressionQueryQueueImpl.QUEUE_SIZE );
    @Autowired
    private CoexpressionDao coexpressionDao;
    @Autowired
    private GeneDao geneDao;
    @Autowired
    private TaskExecutor taskExecutor;

    @Override
    public synchronized void addToFullQueryQueue( Collection<Long> geneIds ) {
        if ( this.geneQueue.remainingCapacity() == 0 ) {
            CoexpressionQueryQueueImpl.log.debug( "Queue is full, cannot add genes for cache warm." );
            return;
        }

        for ( Long id : geneIds ) {
            this.addToFullQueryQueue( id );
        }
    }

    @Override
    public synchronized void addToFullQueryQueue( Gene gene ) {
        this.addToFullQueryQueue( gene.getId() );
    }

    @Override
    public synchronized void removeFromQueue( Collection<Long> geneIds ) {
        int count = 0;
        for ( Long id : geneIds ) {
            if ( geneQueue.remove( new QueuedGene( id ) ) )
                count++;
        }

        if ( count > 0 ) {
            CoexpressionQueryQueueImpl.log.info( count + " genes removed from the query queue" );
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final SecurityContext context = SecurityContextHolder.getContext();

        taskExecutor.execute( new Runnable() {

            private final int MAX_WARNINGS = 5;

            @Override
            public void run() {
                SecurityContextHolder.setContext( context );

                int numWarnings = 0;
                //noinspection InfiniteLoopStatement // Expected
                while ( true ) {
                    QueuedGene gene;
                    try {

                        synchronized ( geneQueue ) {
                            gene = geneQueue.poll();
                        }

                        if ( gene == null ) {
                            Thread.sleep( 1000 );
                            continue;
                        }

                        CoexpressionQueryQueueImpl.this.queryForCache( gene );

                        Thread.sleep( 500 );

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

    private void queryForCache( QueuedGene gene ) {

        int numCached = coexpressionDao.queryAndCache( geneDao.load( gene.getId() ) );
        //noinspection StatementWithEmptyBody // Better readability
        if ( numCached < 0 ) {
            // it was already in the cache
        } else if ( numCached > 0 ) {
            CoexpressionQueryQueueImpl.log.debug( "Cached " + numCached + " coexpression links at stringency="
                    + CoexpressionCache.CACHE_QUERY_STRINGENCY + " for " + gene.getId() );
        } else {
            CoexpressionQueryQueueImpl.log
                    .debug( "No coexpression links to cache at stringency=" + CoexpressionCache.CACHE_QUERY_STRINGENCY
                            + " for " + gene.getId() );
        }
    }

    private synchronized void addToFullQueryQueue( Long id ) {
        if ( this.geneQueue.remainingCapacity() == 0 ) {
            CoexpressionQueryQueueImpl.log.debug( "Queue is full, cannot add genes for cache warm." );
            return;
        }

        try {
            CoexpressionQueryQueueImpl.log.debug( "Queuing gene=" + id + " for cache warm" );
            this.geneQueue.add( new QueuedGene( id ) );
        } catch ( Exception e ) {
            // if JVM is finished (e.g. test) this will happen.
            CoexpressionQueryQueueImpl.log.error( "Could not add to queue: " + e.getMessage() );
        }
    }
}

class QueuedGene {

    private final Long id;

    QueuedGene( Long id ) {
        super();
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        QueuedGene other = ( QueuedGene ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
    }

}

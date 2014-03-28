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

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;

/**
 * @author Paul
 * @version $Id$
 */
@Repository
@Lazy
public class CoexpressionQueryQueueImpl extends HibernateDaoSupport implements CoexpressionQueryQueue {

    private static Logger log = LoggerFactory.getLogger( CoexpressionQueryQueueImpl.class );
    private static final int QUEUE_SIZE = 1000;

    @Autowired
    private CoexpressionDao coexpressionDao;

    @Autowired
    private GeneDao geneDao;

    private final BlockingQueue<QueuedGene> geneQueue = new ArrayBlockingQueue<QueuedGene>( QUEUE_SIZE );

    @Autowired
    public CoexpressionQueryQueueImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.CoexpressionQueryQueue#addToFullQueryQueue(java.util.Collection,
     * java.lang.String)
     */
    @Override
    public synchronized void addToFullQueryQueue( Collection<Long> geneIds, String className ) {
        if ( this.geneQueue.remainingCapacity() == 0 ) {
            log.debug( "Queue is full, cannot add genes for cache warm." );
            return;
        }

        for ( Long id : geneIds ) {
            addToFullQueryQueue( id, className );
        }
    }

    @Override
    public synchronized void removeFromQueue( Collection<Long> geneIds, String className ) {
        int count = 0;
        for ( Long id : geneIds ) {
            if ( geneQueue.remove( new QueuedGene( id, className ) ) ) count++;
        }

        if ( count > 0 ) {
            log.info( count + " genes removed from the query queue" );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.CoexpressionQueryQueue#addToFullQueryQueue(ubic.gemma.model.genome.
     * Gene)
     */
    @Override
    public synchronized void addToFullQueryQueue( Gene gene ) {
        addToFullQueryQueue( gene.getId(), CoexpressionQueryUtils.getGeneLinkClassName( gene ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.CoexpressionQueryQueue#addToFullQueryQueue(java.lang.Long,
     * java.lang.String)
     */
    @Override
    public synchronized void addToFullQueryQueue( Long id, String className ) {
        if ( this.geneQueue.remainingCapacity() == 0 ) {
            log.debug( "Queue is full, cannot add genes for cache warm." );
            return;
        }

        try {
            log.debug( "Queuing gene=" + id + " for cache warm" );
            this.geneQueue.add( new QueuedGene( id, className ) );
        } catch ( Exception e ) {
            // if JVM is finished (e.g. test) this will happen.
            log.error( "Could not add to queue: " + e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.CoexpressionQueryQueue#queryForCache(ubic.gemma.model.association.
     * coexpression.QueuedGeneBatch)
     */
    @Override
    @Transactional(readOnly = true)
    public void queryForCache( QueuedGene gene ) {

        int numCached = coexpressionDao.queryAndCache( geneDao.load( gene.getId() ) );
        if ( numCached < 0 ) {
            // it was already in the cache
        } else if ( numCached > 0 ) {
            log.debug( "Cached " + numCached + " coexpression links at stringency="
                    + CoexpressionCache.CACHE_QUERY_STRINGENCY + " for " + gene.getId() );
        } else {
            log.debug( "No coexpression links to cache at stringency=" + CoexpressionCache.CACHE_QUERY_STRINGENCY
                    + " for " + gene.getId() );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.dao.support.DaoSupport#initDao()
     */
    @Override
    protected void initDao() throws Exception {
        super.initDao();
        final SecurityContext context = SecurityContextHolder.getContext();

        Thread loadThread = new Thread( new Runnable() {

            private static final int MAX_WARNINGS = 5;

            @Override
            public void run() {
                SecurityContextHolder.setContext( context );
                // TODO might need to manage this.

                int numWarnings = 0;
                while ( true ) {
                    QueuedGene gene = null;
                    try {

                        synchronized ( geneQueue ) {
                            gene = geneQueue.poll();
                        }

                        if ( gene == null ) {
                            Thread.sleep( 1000 );
                            continue;
                        }

                        queryForCache( gene );

                        Thread.sleep( 500 );

                    } catch ( Exception e ) {
                        // can happen during tests
                        if ( numWarnings < MAX_WARNINGS ) {
                            log.error( "Error while caching coexpression: " + e.getMessage() );
                        } else if ( numWarnings == MAX_WARNINGS ) {
                            log.error( "Further warnings suppressed" );
                        }
                        numWarnings++;
                    }
                }
            }

        }, "Fetching coexpression for recently used genes" );
        loadThread.setDaemon( true );
        loadThread.start();
    }
}

class QueuedGene {

    private String className;

    private Long id;

    public QueuedGene( Long id, String className ) {
        super();
        this.id = id;
        this.className = className;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        QueuedGene other = ( QueuedGene ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    public String getClassName() {
        return className;
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

}

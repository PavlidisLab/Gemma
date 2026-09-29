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
package ubic.gemma.core.loader.association;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.util.FileTools;
import ubic.gemma.core.util.concurrent.ThreadUtils;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.persistence.persister.RelationshipPersister;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads gene2go with two threads joined by a bounded queue: the parser and the loader, which persists in batches.
 * <p>
 * The first exception or error in either thread stops the other and is rethrown from {@link #load(InputStream)}.
 * Before, it ended only its own thread: a parser failure left the main thread waiting forever, and a loader failure
 * either blocked the parser on the full queue forever or, within the queue's size of the end, let the run finish as
 * though every association had been loaded.
 *
 * @author keshav
 * @author pavlidis
 */
public class NCBIGene2GOAssociationLoader {

    private static final Log log = LogFactory.getLog( NCBIGene2GOAssociationLoader.class );
    private static final int QUEUE_SIZE = 60000;
    private static final int BATCH_SIZE = 12000;
    private final AtomicBoolean producerDone = new AtomicBoolean( false );
    private final AtomicBoolean consumerDone = new AtomicBoolean( false );
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private volatile boolean stopped = false;
    private RelationshipPersister relationshipPersister;
    private NCBIGene2GOAssociationParser parser = null;
    private int queueSize = QUEUE_SIZE;
    private int batchSize = BATCH_SIZE;
    /**
     * Associations handed to the persister; written by the loader thread only.
     */
    private volatile int count;

    /**
     * @return the number of associations persisted
     */
    public int getCount() {
        return count;
    }

    /**
     * Capacity of the queue between the parser and the loader.
     */
    void setQueueSize( int queueSize ) {
        this.queueSize = queueSize;
    }

    /**
     * Number of associations persisted at a time.
     */
    void setBatchSize( int batchSize ) {
        this.batchSize = batchSize;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public boolean isConsumerDone() {
        return consumerDone.get();
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public boolean isProducerDone() {
        return producerDone.get();
    }

    public void load( final InputStream inputStream ) {
        // in case this is reused
        producerDone.set( false );
        consumerDone.set( false );
        failure.set( null );
        stopped = false;
        count = 0;

        final BlockingQueue<Gene2GOAssociation> queue = new ArrayBlockingQueue<>( queueSize );

        Thread loadThread = ThreadUtils.newThread( () -> {
            NCBIGene2GOAssociationLoader.log.info( "Starting loading" );
            try {
                NCBIGene2GOAssociationLoader.this.load( queue );
            } catch ( Throwable e ) {
                if ( !stopped ) {
                    failure.compareAndSet( null, new RuntimeException( "Failed to persist GO associations after "
                            + count + " were loaded.", e ) );
                }
            }
        }, "gene2go loader" );

        Thread parseThread = ThreadUtils.newThread( () -> {
            try {
                parser.parse( inputStream, queue );
            } catch ( Throwable e ) {
                if ( !stopped ) {
                    failure.compareAndSet( null, new RuntimeException( "Failed to parse gene2go.", e ) );
                }
                return;
            }
            NCBIGene2GOAssociationLoader.log.info( "Done parsing" );
            producerDone.set( true );
        }, "gene2go parser" );

        loadThread.start();
        parseThread.start();

        while ( !this.isProducerDone() || !this.isConsumerDone() ) {
            Throwable t = failure.get();
            if ( t != null ) {
                this.stop( parseThread, loadThread );
                throw t instanceof RuntimeException ? ( RuntimeException ) t : new RuntimeException( t );
            }
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                this.stop( parseThread, loadThread );
                Thread.currentThread().interrupt();
                throw new RuntimeException( "Interrupted while waiting for the GO associations to load.", e );
            }
        }
    }

    private void stop( Thread parseThread, Thread loadThread ) {
        stopped = true;
        parseThread.interrupt();
        loadThread.interrupt();
    }

    public void load( File ncbiFile ) {

        try ( InputStream inputStream = FileTools
                .getInputStreamFromPlainOrCompressedFile( ncbiFile.getAbsolutePath() ) ) {
            this.load( inputStream );

        } catch ( IOException e ) {
            NCBIGene2GOAssociationLoader.log.error( e, e );
            throw new RuntimeException( e );
        }

    }

    public void setParser( NCBIGene2GOAssociationParser parser ) {
        this.parser = parser;
    }

    /**
     * Persister-shrink S3: was {@code setPersisterHelper(Persister)}; now takes the
     * typed {@link RelationshipPersister} bean directly. The {@code persist(itemsToPersist)}
     * calls route through {@link RelationshipPersister#persistGene2GOAssociations(Collection)}.
     */
    public void setRelationshipPersister( RelationshipPersister relationshipPersister ) {
        this.relationshipPersister = relationshipPersister;
    }

    private void load( BlockingQueue<Gene2GOAssociation> queue ) {

        NCBIGene2GOAssociationLoader.log.debug( "Entering 'load' " );

        long millis = System.currentTimeMillis();
        int cpt = 0;
        double secspt = 0.0;

        Collection<Gene2GOAssociation> itemsToPersist = new ArrayList<>();
        int queued = 0;
        while ( !( producerDone.get() && queue.isEmpty() ) ) {
            Gene2GOAssociation association;
            try {
                association = queue.poll( 1, TimeUnit.SECONDS );
            } catch ( InterruptedException e ) {
                // stopped because the parser failed
                return;
            }

            if ( association == null ) {
                continue;
            }

            itemsToPersist.add( association );
            if ( ++queued % batchSize == 0 ) {
                relationshipPersister.persistGene2GOAssociations( itemsToPersist );
                count += itemsToPersist.size();
                itemsToPersist.clear();
            }

            // just some timing information.
            if ( queued % 10000 == 0 ) {
                cpt++;
                double secsperthousand = ( System.currentTimeMillis() - millis ) / 1000.0;
                secspt += secsperthousand;
                double meanspt = secspt / cpt;

                String progString = "Processed and loaded " + queued + " (" + secsperthousand
                        + " seconds elapsed, average per thousand=" + String.format( "%.2f", meanspt ) + "), last was: " + association;
                NCBIGene2GOAssociationLoader.log.info( progString );
                millis = System.currentTimeMillis();
            }

        }

        // finish up.
        relationshipPersister.persistGene2GOAssociations( itemsToPersist );
        count += itemsToPersist.size();

        NCBIGene2GOAssociationLoader.log.info( "Finished, loaded total of " + count + " GO associations" );
        consumerDone.set( true );

    }
}

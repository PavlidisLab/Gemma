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
import ubic.basecode.util.FileTools;
import ubic.gemma.core.util.concurrent.ThreadUtils;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.persistence.persister.Persister;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author keshav
 * @author pavlidis
 */
public class NCBIGene2GOAssociationLoader {

    private static final Log log = LogFactory.getLog( NCBIGene2GOAssociationLoader.class );
    private static final int QUEUE_SIZE = 60000;
    private static final int BATCH_SIZE = 12000;
    private final AtomicBoolean producerDone = new AtomicBoolean( false );
    private final AtomicBoolean consumerDone = new AtomicBoolean( false );
    private Persister persisterHelper;
    private NCBIGene2GOAssociationParser parser = null;
    private int count;

    public int getCount() {
        return count;
    }

    private void setCount( int count ) {
        this.count = count;
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
        final BlockingQueue<Gene2GOAssociation> queue = new ArrayBlockingQueue<>(
                NCBIGene2GOAssociationLoader.QUEUE_SIZE );

        Thread loadThread = ThreadUtils.newThread( new Runnable() {
            @Override
            public void run() {
                NCBIGene2GOAssociationLoader.log.info( "Starting loading" );
                NCBIGene2GOAssociationLoader.this.load( queue );
            }
        } );

        loadThread.start();

        Thread parseThread = ThreadUtils.newThread( new Runnable() {
            @Override
            public void run() {
                try {
                    // NCBIGene2GOAssociationParser parser = new NCBIGene2GOAssociationParser();
                    parser.parse( inputStream, queue );
                    NCBIGene2GOAssociationLoader.this.setCount( parser.getCount() );
                } catch ( IOException e ) {
                    NCBIGene2GOAssociationLoader.log.error( e, e );
                    throw new RuntimeException( e );
                }
                NCBIGene2GOAssociationLoader.log.info( "Done parsing" );
                producerDone.set( true );
            }
        } );

        parseThread.start();

        while ( !this.isProducerDone() || !this.isConsumerDone() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
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

    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    private void load( BlockingQueue<Gene2GOAssociation> queue ) {

        NCBIGene2GOAssociationLoader.log.debug( "Entering 'load' " );

        long millis = System.currentTimeMillis();
        int cpt = 0;
        double secspt = 0.0;

        Collection<Gene2GOAssociation> itemsToPersist = new ArrayList<>();
        try {
            while ( !( producerDone.get() && queue.isEmpty() ) ) {
                Gene2GOAssociation association = queue.poll();

                if ( association == null ) {
                    continue;
                }

                itemsToPersist.add( association );
                if ( ++count % NCBIGene2GOAssociationLoader.BATCH_SIZE == 0 ) {
                    persisterHelper.persist( itemsToPersist );
                    itemsToPersist.clear();
                }

                // just some timing information.
                if ( count % 10000 == 0 ) {
                    cpt++;
                    double secsperthousand = ( System.currentTimeMillis() - millis ) / 1000.0;
                    secspt += secsperthousand;
                    double meanspt = secspt / cpt;

                    String progString = "Processed and loaded " + count + " (" + secsperthousand
                            + " seconds elapsed, average per thousand=" + String.format( "%.2f", meanspt ) + "), last was: " + association;
                    NCBIGene2GOAssociationLoader.log.info( progString );
                    millis = System.currentTimeMillis();
                }

            }
        } catch ( Exception e ) {
            consumerDone.set( true );
            NCBIGene2GOAssociationLoader.log.fatal( e, e );
            throw new RuntimeException( e );
        }

        // finish up.
        persisterHelper.persist( itemsToPersist );

        NCBIGene2GOAssociationLoader.log.info( "Finished, loaded total of " + count + " GO associations" );
        consumerDone.set( true );

    }
}

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
package ubic.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.Persister;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGene2GOAssociationLoader {

    protected static final Log log = LogFactory.getLog( NCBIGene2GOAssociationLoader.class );
    private static final int QUEUE_SIZE = 60000;
    private static final int BATCH_SIZE = 12000;

    private Persister persisterHelper;

    private NCBIGene2GOAssociationParser parser = null;

    private AtomicBoolean producerDone = new AtomicBoolean( false );
    private AtomicBoolean consumerDone = new AtomicBoolean( false );
    private int count;

    public int getCount() {
        return count;
    }

    public boolean isConsumerDone() {
        return consumerDone.get();
    }

    public boolean isProducerDone() {
        return producerDone.get();
    }

    /**
     * @param inputStream
     */
    public void load( final InputStream inputStream ) {
        final BlockingQueue<Gene2GOAssociation> queue = new ArrayBlockingQueue<Gene2GOAssociation>( QUEUE_SIZE );
        final SecurityContext context = SecurityContextHolder.getContext();
        final Authentication authentication = context.getAuthentication();

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                log.info( "Starting loading" );
                SecurityContextHolder.setContext( context );
                load( queue );
            }
        } );

        loadThread.start();

        Thread parseThread = new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    // NCBIGene2GOAssociationParser parser = new NCBIGene2GOAssociationParser();
                    SecurityContextHolder.getContext().setAuthentication( authentication );
                    parser.parse( inputStream, queue );
                    setCount( parser.getCount() );
                } catch ( IOException e ) {
                    log.error( e, e );
                    throw new RuntimeException( e );
                }
                log.info( "Done parsing" );
                producerDone.set( true );
            }
        } );

        parseThread.start();

        while ( !isProducerDone() || !isConsumerDone() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    public void setParser( NCBIGene2GOAssociationParser parser ) {
        this.parser = parser;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param queue
     */
    protected void load( BlockingQueue<Gene2GOAssociation> queue ) {

        log.debug( "Entering 'load' " );

        long millis = System.currentTimeMillis();
        int cpt = 0;
        double secspt = 0.0;

        Collection<Gene2GOAssociation> itemsToPersist = new ArrayList<Gene2GOAssociation>();
        try {
            while ( !( producerDone.get() && queue.isEmpty() ) ) {
                Gene2GOAssociation associations = queue.poll();

                if ( associations == null ) {
                    continue;
                }

                itemsToPersist.add( associations );
                if ( ++count % BATCH_SIZE == 0 ) {
                    persisterHelper.persist( itemsToPersist );
                    itemsToPersist.clear();
                }

                // just some timing information.
                if ( count % 1000 == 0 ) {
                    cpt++;
                    double secsperthousand = ( System.currentTimeMillis() - millis ) / 1000.0;
                    secspt += secsperthousand;
                    double meanspt = secspt / cpt;

                    String progString = "Processed and loaded " + count + " (" + secsperthousand
                            + " seconds elapsed, average per thousand=" + String.format( "%.2f", meanspt ) + ")";
                    log.info( progString );
                    millis = System.currentTimeMillis();
                }

            }
        } catch ( Exception e ) {
            consumerDone.set( true );
            log.fatal( e, e );
            throw new RuntimeException( e );
        }

        // finish up.
        persisterHelper.persist( itemsToPersist );

        log.info( "Finished, loaded total of " + count + " GO associations" );
        consumerDone.set( true );

    }

    protected int load( Collection<Gene2GOAssociation> g2GoCol ) {

        for ( Gene2GOAssociation association : g2GoCol ) {
            if ( ++count % 1000 == 0 ) {
                log.info( "Persisted " + count + " Gene to GO associations" );
            }
            load( association );
        }

        return count;
    }

    /**
     * @param entity
     * @return
     */
    protected Gene2GOAssociation load( Gene2GOAssociation entity ) {
        assert entity.getGene() != null;
        assert entity.getOntologyEntry() != null;
        return ( Gene2GOAssociation ) persisterHelper.persist( entity );
    }

    protected void load( LocalFile ncbiFile ) {

        try (InputStream inputStream = FileTools.getInputStreamFromPlainOrCompressedFile( ncbiFile.asFile()
                .getAbsolutePath() );) {
            load( inputStream );

        } catch ( IOException e ) {
            log.error( e, e );
            throw new RuntimeException( e );
        }

    }

    private void setCount( int count ) {
        this.count = count;
    }
}

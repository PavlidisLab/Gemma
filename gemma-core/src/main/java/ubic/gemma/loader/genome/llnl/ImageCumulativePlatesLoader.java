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
package ubic.gemma.loader.genome.llnl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Persist biosequences generated from the IMAGE clone library. IMAGE contains ESTs from human, mouse, rat, rhesus and a
 * few others. This class assumes that the sequences are new.
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class ImageCumulativePlatesLoader {

    Log log = LogFactory.getLog( ImageCumulativePlatesLoader.class.getName() );

    private static final int BATCH_SIZE = 2000;
    private static final int QUEUE_SIZE = 30000;

    boolean producerDone = false;
    private boolean consumerDone = false;

    PersisterHelper persisterHelper;

    ExternalDatabaseService externalDatabaseService;
    BioSequenceService bioSequenceService;
    ExternalDatabase genbank;

    private int numLoaded;

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
        genbank = externalDatabaseService.find( "Genbank" );
        assert ( genbank != null && genbank.getId() != null );
    }

    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public int load( File file ) throws IOException {
        if ( file == null ) {
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );
        int count = load( stream );
        stream.close();
        return count;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public int load( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        log.info( "Parsing " + filename );
        File infile = new File( filename );
        return load( infile );
    }

    /**
     * @param inputStream
     * @throws IOException
     */
    public int load( final InputStream inputStream ) {
        final ImageCumulativePlatesParser parser = new ImageCumulativePlatesParser();
        final BlockingQueue<BioSequence> queue = new ArrayBlockingQueue<BioSequence>( QUEUE_SIZE );
        final SecurityContext context = SecurityContextHolder.getContext();

        Thread loadThread = new Thread( new Runnable() {
            public void run() {
                log.info( "Starting loading" );
                SecurityContextHolder.setContext( context );
                load( queue );
            }
        } );

        loadThread.start();

        Thread parseThread = new Thread( new Runnable() {
            public void run() {
                try {
                    parser.parse( inputStream, queue );
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
                log.info( "Done parsing" );
                producerDone = true;
            }
        } );

        parseThread.start();

        while ( !producerDone || !consumerDone ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
        return this.numLoaded;

    }

    void load( BlockingQueue<BioSequence> queue ) {
        log.debug( "Entering 'load' " );

        long millis = System.currentTimeMillis();

        int count = 0;
        int cpt = 0;
        double secspt = 0.0;

        Collection<BioSequence> bioSequencesToPersist = new ArrayList<BioSequence>();
        try {
            while ( !( producerDone && queue.isEmpty() ) ) {
                BioSequence sequence = queue.poll();

                if ( sequence == null ) {
                    continue;
                }

                sequence.getSequenceDatabaseEntry().setExternalDatabase( genbank );
                sequence.setTaxon( ( Taxon ) persisterHelper.persist( sequence.getTaxon() ) );

                bioSequencesToPersist.add( sequence );
                if ( ++count % BATCH_SIZE == 0 ) {
                    persistBatch( bioSequencesToPersist );
                    bioSequencesToPersist.clear();
                }

                // just some timing information.
                if ( count % 1000 == 0 ) {
                    cpt++;
                    double secsperthousand = ( System.currentTimeMillis() - millis ) / 1000.0;
                    secspt += secsperthousand;
                    double meanspt = secspt / cpt;

                    String progString = "Processed and loaded " + count + " sequences, last one was "
                            + sequence.getName() + " (" + secsperthousand + " seconds elapsed, average per thousand="
                            + meanspt + ")";
                    log.info( progString );
                    millis = System.currentTimeMillis();
                }

            }

            // finish up.
            persistBatch( bioSequencesToPersist );

        } catch ( Exception e ) {
            consumerDone = true;
            producerDone = true; // stop everything.
            throw new RuntimeException( e );
        }

        log.info( "Loaded total of " + count + " sequences" );
        consumerDone = true;

        this.numLoaded = count;
    }

    /**
     * We only update sequences if possible, to avoid duplicates.
     * 
     * @param bioSequencesToPersist
     */
    private void persistBatch( Collection<BioSequence> bioSequencesToPersist ) {
        int alreadyThere = 0;
        for ( BioSequence sequence2 : bioSequencesToPersist ) {
            BioSequence existing = bioSequenceService.findByAccession( sequence2.getSequenceDatabaseEntry() );
            if ( existing != null ) {
                alreadyThere++;
                if ( !existing.getName().equals( sequence2.getName() ) ) {
                    existing.setName( sequence2.getName() );
                    bioSequenceService.update( existing );
                }
            } else {
                if ( log.isDebugEnabled() ) log.debug( "Adding " + sequence2 );
                persisterHelper.persist( sequence2 );
            }
        }
        log.info( alreadyThere + "/" + BATCH_SIZE
                + " of last batch were already in the database, at most just updated name" );
    }

}

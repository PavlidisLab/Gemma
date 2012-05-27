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
package ubic.gemma.loader.genome.goldenpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.externalDb.GoldenPathDumper;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;

/**
 * Load a dump of a Goldenpath table. The input is expected to have just two columns: sequence identifier (accession)
 * and sequence length. Note that this uses create, not findOrCreate, so it should only be used to 'prime' the system.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathBioSequenceLoader {

    private static final int QUEUE_SIZE = 30000;
    private static final int BATCH_SIZE = 2000;
    static Log log = LogFactory.getLog( GoldenPathBioSequenceLoader.class.getName() );
    ExternalDatabaseService externalDatabaseService;
    BioSequenceService bioSequenceService;
    ExternalDatabase genbank;

    int limit = -1;

    GoldenPathBioSequenceParser parser = new GoldenPathBioSequenceParser();
    private Taxon taxon;
    boolean producerDone = false;
    private boolean consumerDone = false;

    public GoldenPathBioSequenceLoader( Taxon taxon ) {
        this.taxon = taxon;
    }

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
        genbank = externalDatabaseService.find( "Genbank" );
    }

    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public void load( File file ) throws IOException {
        if ( file == null ) {
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        FileInputStream stream = new FileInputStream( file );
        load( stream );
        stream.close();
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public void load( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        log.info( "Parsing " + filename );
        File infile = new File( filename );
        load( infile );
    }

    /**
     * @param inputStream
     * @return
     * @throws IOException
     */
    public void load( final InputStream inputStream ) {

        final BlockingQueue<BioSequence> queue = new ArrayBlockingQueue<BioSequence>( QUEUE_SIZE );
        final SecurityContext context = SecurityContextHolder.getContext();

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

    }

    /**
     * Load from a database source.
     * 
     * @param dumper
     */
    public void load( final GoldenPathDumper dumper ) {
        final BlockingQueue<BioSequence> queue = new ArrayBlockingQueue<BioSequence>( QUEUE_SIZE );

        final SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;
        Thread parseThread = new Thread( new Runnable() {
            @Override
            public void run() {
                dumper.dumpTranscriptBioSequences( limit, queue );
                log.info( "Done dumping" );
                producerDone = true;
            }
        }, "Parser" );

        parseThread.start();

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                SecurityContextHolder.setContext( context );
                log.info( "Starting loading" );
                load( queue );
            }
        }, "Loader" );

        loadThread.start();

        while ( !producerDone || !consumerDone ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param bioSequences
     */ 
    void load( BlockingQueue<BioSequence> queue ) {
        log.debug( "Entering 'load' " );

        StopWatch timer = new StopWatch();
        timer.start();

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
                sequence.setTaxon( taxon );
                bioSequencesToPersist.add( sequence );
                if ( ++count % BATCH_SIZE == 0 ) {
                    bioSequenceService.create( bioSequencesToPersist );
                    bioSequencesToPersist.clear();
                }

                // just some timing information.
                if ( count % 1000 == 0 ) {
                    cpt++;
                    timer.stop();
                    double secsperthousand = timer.getTime() / 1000.0;
                    secspt += secsperthousand;
                    double meanspt = secspt / cpt;

                    String progString = "Processed and loaded " + count + " sequences, last one was "
                            + sequence.getName() + " (" + secsperthousand + "s for last 1000, mean per 1000 ="
                            + String.format( "%.1f", meanspt ) + "s)";
                    log.info( progString );
                    timer.reset();
                    timer.start();
                }

            }
        } catch ( Exception e ) {
            consumerDone = true;
            throw new RuntimeException( e );
        }

        // finish up.
        bioSequenceService.create( bioSequencesToPersist );

        log.info( "Loaded total of " + count + " sequences" );
        consumerDone = true;

    }

    public void setLimit( int limit ) {
        this.limit = limit;
    }
}

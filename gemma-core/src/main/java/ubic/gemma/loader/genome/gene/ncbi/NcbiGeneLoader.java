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
package ubic.gemma.loader.genome.gene.ncbi;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.PersisterHelper;

/**
 * @author jsantos
 * @version $Id$
 */
public class NcbiGeneLoader {
    private static Log log = LogFactory.getLog( NcbiGeneConverter.class.getName() );
    private static final int QUEUE_SIZE = 1000;

    private AtomicBoolean generatorDone;
    private AtomicBoolean converterDone;
    private AtomicBoolean loaderDone;
    private PersisterHelper persisterHelper;
    private int loadedGeneCount = 0;

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @return the loadedGeneCount
     */
    public int getLoadedGeneCount() {
        return loadedGeneCount;
    }

    public NcbiGeneLoader() {
        generatorDone = new AtomicBoolean( false );
        converterDone = new AtomicBoolean( false );
        loaderDone = new AtomicBoolean( false );
    }

    public NcbiGeneLoader( PersisterHelper persisterHelper ) {
        this();
        this.setPersisterHelper( persisterHelper );
    }

    /**
     * @param file the gene_info file
     * @param file the gene2accession file
     * @param filterTaxa should we filter out taxa we're not supporting
     * @return
     * @throws IOException
     */
    public void load( String geneInfoFile, String gene2AccFile, boolean filterTaxa ) {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator();
        sdog.setProducerDoneFlag( generatorDone );
        NcbiGeneConverter converter = new NcbiGeneConverter();
        converter.setSourceDoneFlag( generatorDone );
        converter.setProducerDoneFlag( converterDone );

        // create queue for GeneInfo objects
        final BlockingQueue<NcbiGeneData> geneInfoQueue = new ArrayBlockingQueue<NcbiGeneData>( QUEUE_SIZE );
        final BlockingQueue<Gene> geneQueue = new ArrayBlockingQueue<Gene>( QUEUE_SIZE );

        // Threaded producer - loading files into queue as GeneInfo objects
        if ( StringUtils.isEmpty( geneInfoFile ) || StringUtils.isEmpty( geneInfoFile ) ) {
            sdog.generate( geneInfoQueue );
        } else {
            sdog.generateLocal( geneInfoFile, gene2AccFile, geneInfoQueue, filterTaxa );
        }

        // Threaded consumer/producer - consumes GeneInfo objects and generates
        // Gene/GeneProduct/DatabaseEntry entries
        converter.convert( geneInfoQueue, geneQueue );

        // Threaded consumer. Consumes Gene objects and persists them into
        // the database
        this.load( geneQueue );
    }

    /**
     * download the gene_info and gene2accession files, then call load
     * 
     * @return
     * @throws IOException
     */
    public void load( boolean filterTaxa ) {
        String geneInfoFile = "";
        String gene2AccFile = "";
        load( geneInfoFile, gene2AccFile, filterTaxa );
    }

    /**
     * @param geneQueue a blocking queue of genes to be loaded into the database loads genes into the database
     * @param geneQueue
     */
    private void load( final BlockingQueue<Gene> geneQueue ) {
        final SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;

        Thread loadThread = new Thread( new Runnable() {
            public void run() {
                SecurityContextHolder.setContext( context );
                doLoad( geneQueue );
            }

        }, "Loading" );
        loadThread.start();

        while ( !generatorDone.get() || !converterDone.get() || !loaderDone.get() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param geneQueue
     */
    void doLoad( final BlockingQueue<Gene> geneQueue ) {
        while ( !( converterDone.get() && geneQueue.isEmpty() ) ) {

            try {
                Gene gene = geneQueue.poll();
                if ( gene == null ) {
                    continue;
                }
                
                persisterHelper.persistOrUpdate( gene );

                if ( ++loadedGeneCount % 1000 == 0 ) {
                    log.info( "Loaded " + loadedGeneCount + " genes. " + "Current queue has " + geneQueue.size()
                            + " items." );
                }

            } catch ( Exception e ) {
                log.error( e, e );
                loaderDone.set( true );
                throw new RuntimeException( e );
            }
        }
        log.info( "Loaded " + loadedGeneCount + " genes. " );
        loaderDone.set( true );
    }

    public boolean isLoaderDone() {
        return loaderDone.get();
    }
}

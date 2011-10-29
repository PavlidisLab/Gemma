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
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Load or update information about genes from the NCBI Gene database.
 * 
 * @author jsantos, paul
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
    private TaxonService taxonService;

    // whether to fetch files from ncbi or use existing ones
    private boolean doDownload = true;

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
     * @return the loadedGeneCount
     */
    public int getLoadedGeneCount() {
        return loadedGeneCount;
    }

    public boolean isLoaderDone() {
        return loaderDone.get();
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
        String geneHistoryFile = "";
        String geneEnsemblFile = "";
        load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, filterTaxa );
    }

    /**
     * @param geneInfoFile the gene_info file
     * @param gene2AccFile the gene2accession file
     * @param historyfile
     * @param ensembl mapping file
     * @param filterTaxa should we filter out taxa we're not supporting
     */
    public void load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile,
            boolean filterTaxa ) {

        Collection<Taxon> supportedTaxa = null;
        if ( filterTaxa ) {
            supportedTaxa = this.taxonService.loadAll();
        }
        load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, supportedTaxa );

    }

    /**
     * @param geneInfoFile
     * @param gene2AccFile
     * @param geneHistoryFile
     * @param geneEnsemblFile
     * @param t the specific taxon to process
     */
    public void load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile, Taxon t ) {

        Collection<Taxon> taxaToUse = new HashSet<Taxon>();
        taxaToUse.add( t );

        this.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, taxaToUse );

    }

    public void load( Taxon t ) {
        String geneInfoFile = "";
        String gene2AccFile = "";
        String geneHistoryFile = "";
        String geneEnsemblFile = "";
        load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, t );
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setTaxonService( TaxonService bean ) {
        this.taxonService = bean;

    }

    /**
     * Method to update taxon to indicate that genes have been loaded for that taxon are are usable. If there is a
     * parent taxon for this species and it has genes loaded against it then use that parent's taxons genes rather than
     * the species found in NCBI. Set the flag genesUSable to false for that child taxon that was found in ncbi.
     * 
     * @param taxaGenesLoaded List of taxa that have had genes loaded into GEMMA from NCBI.
     */
    public void updateTaxaWithGenesUsable( Collection<Taxon> taxaGenesLoaded ) {

        if ( taxaGenesLoaded != null && !taxaGenesLoaded.isEmpty() ) {
            for ( Taxon taxon : taxaGenesLoaded ) {

                if ( taxon == null ) {
                    log.warn( "null taxon" );
                    continue;
                }

                Boolean genesUsableParent = false;
                Taxon parentTaxon = taxon.getParentTaxon();
                if ( parentTaxon != null && parentTaxon.getIsGenesUsable() ) {
                    genesUsableParent = true;
                    taxon.setIsGenesUsable( false );
                    taxonService.update( taxon );
                    log.debug( "Parent taxon found: " + parentTaxon + ": Not using genes from taxon: " + taxon );
                }
                if ( !taxon.getIsGenesUsable() && !genesUsableParent ) {
                    taxon.setIsGenesUsable( true );
                    taxonService.update( taxon );
                    log.debug( "Updating taxon genes usable to true for taxon " + taxon );
                }
            }
        } else {
            throw new IllegalArgumentException( "No taxa were processed for this NCBI load" );
        }
    }

    /**
     * @param geneQueue
     */
    void doLoad( final BlockingQueue<Gene> geneQueue ) {
        StopWatch timer = new StopWatch();
        timer.start();
        while ( !( converterDone.get() && geneQueue.isEmpty() ) ) {

            try {
                Gene gene = geneQueue.poll();
                if ( gene == null ) {
                    continue;
                }

                persisterHelper.persistOrUpdate( gene );

                if ( ++loadedGeneCount % 1000 == 0 || timer.getTime() > 30 * 1000 ) {
                    log.info( "Processed " + loadedGeneCount + " genes. Queue has " + geneQueue.size()
                            + " items; last gene: " + gene );
                    timer.reset();
                    timer.start();
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
     * @param geneInfoFile
     * @param gene2AccFile
     * @param geneHistoryFile
     * @param geneEnsemblFile
     * @param supportedTaxa can be null if we just want everything
     */
    private void load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile,
            Collection<Taxon> supportedTaxa ) {
        /*
         * In case this is reused.
         */
        this.generatorDone.set( false );
        this.converterDone.set( false );
        this.loaderDone.set( false );

        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator( supportedTaxa );
        sdog.setDoDownload( doDownload );
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
            sdog.generateLocal( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, geneInfoQueue );
        }

        // Threaded consumer/producer - consumes GeneInfo objects and generates
        // Gene/GeneProduct/DatabaseEntry entries
        converter.convert( geneInfoQueue, geneQueue );

        // Threaded consumer. Consumes Gene objects and persists them into
        // the database
        this.load( geneQueue );

        // update taxon table to indicate that now there are genes loaded for that taxa.
        // all or nothing so that if fails for some taxa then no taxa will be updated.
        this.updateTaxaWithGenesUsable( sdog.getSupportedTaxaWithNCBIGenes() );
    }

    public void setSkipDownload( boolean skipDownload ) {
        this.doDownload = !skipDownload;

    }

}

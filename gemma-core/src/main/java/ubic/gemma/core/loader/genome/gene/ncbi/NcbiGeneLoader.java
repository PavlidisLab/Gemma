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
package ubic.gemma.core.loader.genome.gene.ncbi;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.util.concurrent.ThreadUtils;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Load or update information about genes from the NCBI Gene database.
 *
 * @author jsantos, paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class NcbiGeneLoader {
    private static final int QUEUE_SIZE = 1000;
    private static final Log log = LogFactory.getLog( NcbiGeneConverter.class.getName() );
    private final AtomicBoolean generatorDone;
    private final AtomicBoolean converterDone;
    private final AtomicBoolean loaderDone;
    private Persister persisterHelper;
    private int loadedGeneCount = 0;
    private TaxonService taxonService;

    // whether to fetch files from ncbi or use existing ones
    private boolean doDownload = true;
    private Integer startingNcbiId = null;

    public NcbiGeneLoader() {
        generatorDone = new AtomicBoolean( false );
        converterDone = new AtomicBoolean( false );
        loaderDone = new AtomicBoolean( false );
    }

    public NcbiGeneLoader( Persister persisterHelper ) {
        this();
        this.setPersisterHelper( persisterHelper );
    }

    /**
     * @return the loadedGeneCount
     */
    public int getLoadedGeneCount() {
        return loadedGeneCount;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    public boolean isLoaderDone() {
        return loaderDone.get();
    }

    /**
     * download the gene_info and gene2accession files, then call load
     *
     * @param filterTaxa filter taxa
     */
    public void load( boolean filterTaxa ) {
        this.load( "", "", "", "", filterTaxa );
    }

    /**
     * @param geneInfoFile    the gene_info file
     * @param gene2AccFile    the gene2accession file
     * @param geneHistoryFile history file
     * @param geneEnsemblFile mapping file
     * @param filterTaxa      should we filter out taxa we're not supporting
     */
    public void load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile,
            boolean filterTaxa ) {

        Collection<Taxon> supportedTaxa = null;
        if ( filterTaxa ) {
            supportedTaxa = this.taxonService.loadAll();
        }
        this.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, supportedTaxa );

    }

    public void load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile,
            Taxon t ) {

        Collection<Taxon> taxaToUse = new HashSet<>();
        taxaToUse.add( t );

        this.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, taxaToUse );

    }

    public void load( Taxon t ) {
        this.load( "", "", "", "", t );
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( Persister persisterHelper ) {
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
                    NcbiGeneLoader.log.warn( "null taxon" );
                    continue;
                }

                if ( !taxon.getIsGenesUsable() ) {
                    taxonService.updateGenesUsable( taxon, true );
                    NcbiGeneLoader.log.debug( "Updating taxon genes usable to true for taxon " + taxon );
                }
            }
        } else {
            throw new IllegalArgumentException( "No taxa were processed for this NCBI load" );
        }
    }

    /**
     * Set to true to avoid downloading the files, if copies already exist (not recommended if you want an update!)
     *
     * @param skipDownload skip download
     */
    public void setSkipDownload( boolean skipDownload ) {
        this.doDownload = !skipDownload;

    }

    /**
     * Indicate
     *
     * @param startNcbiid start ncbi id
     */
    public void setStartingNcbiId( Integer startNcbiid ) {
        this.startingNcbiId = startNcbiid;
    }

    void doLoad( final BlockingQueue<Gene> geneQueue ) {
        StopWatch timer = new StopWatch();
        timer.start();
        while ( !( converterDone.get() && geneQueue.isEmpty() ) ) {
            Gene gene = null;
            try {
                // the converted genes.
                gene = geneQueue.poll();
                if ( gene == null ) {
                    continue;
                }

                persisterHelper.persistOrUpdate( gene );

                if ( ++loadedGeneCount % 1000 == 0 || timer.getTime() > 30 * 1000 ) {
                    NcbiGeneLoader.log.info( "Processed " + loadedGeneCount + " genes. Queue has " + geneQueue.size()
                            + " items; last gene: " + gene );
                    timer.reset();
                    timer.start();
                }

            } catch ( Exception e ) {
                NcbiGeneLoader.log.error( "Error while loading gene: " + gene + ": " + e.getMessage(), e );
                loaderDone.set( true );
                throw new RuntimeException( e );
            }
        }
        NcbiGeneLoader.log.info( "Loaded " + loadedGeneCount + " genes. " );
        loaderDone.set( true );
    }

    /**
     * @param geneQueue a blocking queue of genes to be loaded into the database loads genes into the database
     */
    private void load( final BlockingQueue<Gene> geneQueue ) {
        Thread loadThread = ThreadUtils.newThread( new Runnable() {
            @Override
            public void run() {
                NcbiGeneLoader.this.doLoad( geneQueue );
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
        sdog.setStartingNcbiId( startingNcbiId );

        NcbiGeneConverter converter = new NcbiGeneConverter();
        converter.setSourceDoneFlag( generatorDone );
        converter.setProducerDoneFlag( converterDone );

        // create queue for GeneInfo objects
        final BlockingQueue<NcbiGeneData> geneInfoQueue = new ArrayBlockingQueue<>( NcbiGeneLoader.QUEUE_SIZE );
        final BlockingQueue<Gene> geneQueue = new ArrayBlockingQueue<>( NcbiGeneLoader.QUEUE_SIZE );

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

}

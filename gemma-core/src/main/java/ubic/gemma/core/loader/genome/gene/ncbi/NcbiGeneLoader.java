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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.util.concurrent.ThreadUtils;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Load or update information about genes from the NCBI Gene database.
 * <p>
 * Three threads, connected by two bounded queues: the gene2accession parser, the converter, and the loader, which
 * upserts each gene in its own transaction. The first exception in any of them stops the others and is rethrown from
 * {@code load()}; before, it ended only its own thread and the run either waited forever on a full queue or finished
 * as though it had succeeded.
 *
 * @author jsantos, paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class NcbiGeneLoader {
    private static final int QUEUE_SIZE = 1000;
    /**
     * How many NCBI ids of each kind the summary lists.
     */
    private static final int SUMMARY_EXAMPLES = 10;
    private static final Log log = LogFactory.getLog( NcbiGeneLoader.class.getName() );
    private final AtomicBoolean generatorDone;
    private final AtomicBoolean converterDone;
    private final AtomicBoolean loaderDone;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private GeneWriteService geneWriteService;
    private int loadedGeneCount = 0;
    private TaxonService taxonService;

    // whether to fetch files from ncbi or use existing ones
    private boolean doDownload = true;
    private Integer startingNcbiId = null;

    @Nullable
    private Integer limit = null;
    private boolean dryRun = false;
    @Nullable
    private TransactionTemplate transactionTemplate;
    @Nullable
    private SessionFactory sessionFactory;
    private int queueSize = QUEUE_SIZE;
    private volatile boolean limitReached = false;

    // summary
    private final List<Integer> createdGenes = new ArrayList<>();
    private int createdGeneCount = 0;
    private final List<Integer> updatedGenes = new ArrayList<>();
    private int updatedGeneCount = 0;
    private final List<String> failedGenes = new ArrayList<>();
    private int failedGeneCount = 0;
    private final List<Integer> genesLosingProducts = new ArrayList<>();
    private int genesLosingProductsCount = 0;
    private long removedGeneProducts = 0;
    private long removedSequenceAssociations = 0;

    public NcbiGeneLoader() {
        generatorDone = new AtomicBoolean( false );
        converterDone = new AtomicBoolean( false );
        loaderDone = new AtomicBoolean( false );
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
     * @param geneWriteService the gene write service used by the loader thread
     *        to upsert each converted Gene. Required for {@link #load} calls.
     */
    public void setGeneWriteService( GeneWriteService geneWriteService ) {
        this.geneWriteService = geneWriteService;
    }

    public void setTaxonService( TaxonService bean ) {
        this.taxonService = bean;

    }

    /**
     * Stop after this many genes have been upserted. A limited run does not mark any taxon as having usable genes.
     */
    public void setLimit( @Nullable Integer limit ) {
        this.limit = limit;
    }

    /**
     * Upsert and flush each gene as usual, then roll its transaction back. Requires
     * {@link #setTransactionManager(PlatformTransactionManager)} and {@link #setSessionFactory(SessionFactory)}.
     * <p>
     * A gene that fails is counted and the run continues, so one rehearsal reports every failure.
     */
    public void setDryRun( boolean dryRun ) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setTransactionManager( PlatformTransactionManager transactionManager ) {
        this.transactionTemplate = new TransactionTemplate( transactionManager );
    }

    /**
     * Used to flush in a dry run, and for the summary's counts of created genes and removed gene products, which come
     * from Hibernate's statistics.
     */
    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Capacity of each of the two queues between the threads.
     */
    void setQueueSize( int queueSize ) {
        this.queueSize = queueSize;
    }

    public int getCreatedGeneCount() {
        return createdGeneCount;
    }

    public int getUpdatedGeneCount() {
        return updatedGeneCount;
    }

    public int getFailedGeneCount() {
        return failedGeneCount;
    }

    public long getRemovedGeneProducts() {
        return removedGeneProducts;
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
            if ( limit != null && loadedGeneCount >= limit ) {
                NcbiGeneLoader.log.info( "Reached the limit of " + limit + " genes." );
                limitReached = true;
                break;
            }
            Gene gene;
            try {
                gene = geneQueue.poll( 1, TimeUnit.SECONDS );
            } catch ( InterruptedException e ) {
                // stopped because another thread failed
                return;
            }
            if ( gene == null ) {
                continue;
            }

            try {
                this.upsert( gene );
            } catch ( Exception e ) {
                if ( dryRun ) {
                    NcbiGeneLoader.log.error( "Dry run: loading " + gene + " failed: " + e.getMessage(), e );
                    if ( failedGenes.size() < SUMMARY_EXAMPLES ) {
                        failedGenes.add( gene.getNcbiGeneId() + " (" + e.getMessage() + ")" );
                    }
                    failedGeneCount++;
                    loadedGeneCount++;
                    continue;
                }
                failure.compareAndSet( null, new RuntimeException( "Failed to load " + gene
                        + ". Every gene before it is committed; after fixing the cause, resume with -restart "
                        + gene.getNcbiGeneId() + ".", e ) );
                return;
            }

            if ( ++loadedGeneCount % 1000 == 0 || timer.getTime() > 30 * 1000 ) {
                NcbiGeneLoader.log.info( "Processed " + loadedGeneCount + " genes. Queue has " + geneQueue.size()
                        + " items; last gene: " + gene );
                timer.reset();
                timer.start();
            }
        }
        NcbiGeneLoader.log.info( "Loaded " + loadedGeneCount + " genes. " );
        loaderDone.set( true );
    }

    /**
     * Upsert one gene in its own transaction, rolled back in a dry run, and classify the outcome from Hibernate's
     * statistics. Only this thread writes while the load runs, so the deltas are this gene's.
     */
    private void upsert( Gene gene ) {
        Statistics stats = sessionFactory != null && sessionFactory.getStatistics().isStatisticsEnabled() ?
                sessionFactory.getStatistics() : null;
        long genesInserted = stats != null ? stats.getEntityStatistics( Gene.class.getName() ).getInsertCount() : 0;
        long productsDeleted = stats != null ? stats.getEntityStatistics( GeneProduct.class.getName() ).getDeleteCount() : 0;
        long associationsDeleted = stats != null ? countDeletedSequenceAssociations( stats ) : 0;

        if ( dryRun ) {
            if ( transactionTemplate == null || sessionFactory == null ) {
                throw new IllegalStateException( "A dry run needs a transaction manager and a session factory." );
            }
            transactionTemplate.executeWithoutResult( status -> {
                geneWriteService.upsert( gene );
                // run the SQL, so constraint violations and flush-time failures show up
                sessionFactory.getCurrentSession().flush();
                status.setRollbackOnly();
            } );
        } else {
            geneWriteService.upsert( gene );
        }

        if ( stats == null ) {
            return;
        }
        if ( stats.getEntityStatistics( Gene.class.getName() ).getInsertCount() > genesInserted ) {
            if ( createdGenes.size() < SUMMARY_EXAMPLES ) createdGenes.add( gene.getNcbiGeneId() );
            createdGeneCount++;
        } else {
            if ( updatedGenes.size() < SUMMARY_EXAMPLES ) updatedGenes.add( gene.getNcbiGeneId() );
            updatedGeneCount++;
        }
        long removed = stats.getEntityStatistics( GeneProduct.class.getName() ).getDeleteCount() - productsDeleted;
        if ( removed > 0 ) {
            if ( genesLosingProducts.size() < SUMMARY_EXAMPLES ) genesLosingProducts.add( gene.getNcbiGeneId() );
            genesLosingProductsCount++;
            removedGeneProducts += removed;
        }
        removedSequenceAssociations += countDeletedSequenceAssociations( stats ) - associationsDeleted;
    }

    /**
     * BLAT and annotation associations: the probe-to-gene mappings GENE2CS is built from.
     */
    private long countDeletedSequenceAssociations( Statistics stats ) {
        return stats.getEntityStatistics( BlatAssociation.class.getName() ).getDeleteCount()
                + stats.getEntityStatistics( AnnotationAssociation.class.getName() ).getDeleteCount();
    }

    /**
     * @param geneQueue a blocking queue of genes to be loaded into the database loads genes into the database
     */
    private void load( final BlockingQueue<Gene> geneQueue, NcbiGeneDomainObjectGenerator sdog,
            NcbiGeneConverter converter ) {
        Thread loadThread = ThreadUtils.newThread( new Runnable() {
            @Override
            public void run() {
                NcbiGeneLoader.this.doLoad( geneQueue );
            }

        }, "Loading" );
        loadThread.start();

        while ( !generatorDone.get() || !converterDone.get() || !loaderDone.get() ) {
            Throwable t = failure.get();
            if ( t != null ) {
                sdog.stop();
                converter.stop();
                loadThread.interrupt();
                this.logSummary();
                throw t instanceof RuntimeException ? ( RuntimeException ) t : new RuntimeException( t );
            }
            if ( loaderDone.get() && limitReached ) {
                // the loader stopped at the limit; the parser and converter may be blocked on full queues
                sdog.stop();
                converter.stop();
                break;
            }
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                sdog.stop();
                converter.stop();
                loadThread.interrupt();
                Thread.currentThread().interrupt();
                throw new RuntimeException( "Interrupted while waiting for the gene load to finish.", e );
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
        this.failure.set( null );
        this.limitReached = false;
        this.loadedGeneCount = 0;
        this.createdGenes.clear();
        this.createdGeneCount = 0;
        this.updatedGenes.clear();
        this.updatedGeneCount = 0;
        this.failedGenes.clear();
        this.failedGeneCount = 0;
        this.genesLosingProducts.clear();
        this.genesLosingProductsCount = 0;
        this.removedGeneProducts = 0;
        this.removedSequenceAssociations = 0;

        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator( supportedTaxa );
        sdog.setDoDownload( doDownload );
        sdog.setProducerDoneFlag( generatorDone );
        sdog.setStartingNcbiId( startingNcbiId );
        sdog.setFailure( failure );

        NcbiGeneConverter converter = new NcbiGeneConverter();
        converter.setSourceDoneFlag( generatorDone );
        converter.setProducerDoneFlag( converterDone );
        converter.setFailure( failure );

        // create queue for GeneInfo objects
        final BlockingQueue<NcbiGeneData> geneInfoQueue = new ArrayBlockingQueue<>( queueSize );
        final BlockingQueue<Gene> geneQueue = new ArrayBlockingQueue<>( queueSize );

        // Threaded producer - loading files into queue as GeneInfo objects
        if ( StringUtils.isEmpty( geneInfoFile ) ) {
            sdog.generate( geneInfoQueue );
        } else {
            sdog.generateLocal( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, geneInfoQueue );
        }

        // Threaded consumer/producer - consumes GeneInfo objects and generates
        // Gene/GeneProduct/DatabaseEntry entries
        converter.convert( geneInfoQueue, geneQueue );

        // Threaded consumer. Consumes Gene objects and persists them into
        // the database
        this.load( geneQueue, sdog, converter );
        this.logSummary();

        if ( dryRun ) {
            if ( failedGeneCount > 0 ) {
                throw new RuntimeException( "Dry run: " + failedGeneCount + " of " + loadedGeneCount
                        + " genes failed to load; see the errors above." );
            }
            return;
        }

        if ( limitReached ) {
            log.warn( "Stopped at the limit of " + limit + " genes, so no taxon was marked as having usable genes." );
            return;
        }

        // update taxon table to indicate that now there are genes loaded for that taxa.
        // all or nothing so that if fails for some taxa then no taxa will be updated.
        this.updateTaxaWithGenesUsable( sdog.getSupportedTaxaWithNCBIGenes() );
    }

    private void logSummary() {
        String prefix = dryRun ? "Dry run (every change rolled back): " : "";
        if ( sessionFactory == null || !sessionFactory.getStatistics().isStatisticsEnabled() ) {
            log.info( prefix + loadedGeneCount + " genes processed; no breakdown, Hibernate statistics are unavailable." );
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append( prefix ).append( loadedGeneCount ).append( " genes processed" ).append( "\n" );
        sb.append( "  new:     " ).append( createdGeneCount ).append( examples( createdGenes, createdGeneCount ) ).append( "\n" );
        sb.append( "  updated: " ).append( updatedGeneCount ).append( examples( updatedGenes, updatedGeneCount ) )
                .append( " (includes genes whose record did not change)\n" );
        if ( dryRun ) {
            sb.append( "  failed:  " ).append( failedGeneCount ).append( examples( failedGenes, failedGeneCount ) ).append( "\n" );
        }
        sb.append( "  gene products removed: " ).append( removedGeneProducts );
        if ( removedGeneProducts > 0 ) {
            sb.append( ", from " ).append( genesLosingProductsCount ).append( " genes" ).append( examples( genesLosingProducts, genesLosingProductsCount ) );
        }
        sb.append( "\n" );
        sb.append( "  sequence-to-gene-product associations removed with them: " ).append( removedSequenceAssociations ).append( "\n" );
        sb.append( "  genes held in Gemma but no longer listed by NCBI are not touched" );
        log.info( sb );
    }

    private static String examples( List<?> examples, int total ) {
        if ( examples.isEmpty() ) {
            return "";
        }
        return " (NCBI " + StringUtils.join( examples, ", " ) + ( total > examples.size() ? ", ..." : "" ) + ")";
    }
}

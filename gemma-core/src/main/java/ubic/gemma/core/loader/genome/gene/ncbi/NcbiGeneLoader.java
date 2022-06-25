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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * Load or update information about genes from the NCBI Gene database.
 *
 * @author jsantos, paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class NcbiGeneLoader {
    private static final int QUEUE_SIZE = 1000;
    private static final Log log = LogFactory.getLog( NcbiGeneConverter.class.getName() );
    private final Persister persisterHelper;
    private final TaxonService taxonService;

    // whether to fetch files from ncbi or use existing ones
    private boolean doDownload = true;
    private Integer startingNcbiId = null;

    public NcbiGeneLoader( Persister persisterHelper, TaxonService taxonService ) {
        this.persisterHelper = persisterHelper;
        this.taxonService = taxonService;
    }

    /**
     * download the gene_info and gene2accession files, then call load
     *
     * @param filterTaxa filter taxa
     * @return the number of loaded genes
     */
    public long load( boolean filterTaxa ) {
        return this.load( "", "", "", "", filterTaxa );
    }

    /**
     * @param geneInfoFile    the gene_info file
     * @param gene2AccFile    the gene2accession file
     * @param geneHistoryFile history file
     * @param geneEnsemblFile mapping file
     * @param filterTaxa      should we filter out taxa we're not supporting
     */
    public long load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile,
            boolean filterTaxa ) {
        Collection<Taxon> supportedTaxa = null;
        if ( filterTaxa ) {
            supportedTaxa = this.taxonService.loadAll();
        }
        return this.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, supportedTaxa );
    }

    public long load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile,
            Taxon t ) {
        Collection<Taxon> taxaToUse = new HashSet<>();
        taxaToUse.add( t );
        return this.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, taxaToUse );
    }

    public long load( Taxon t ) {
        return this.load( "", "", "", "", t );
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
                    taxon.setIsGenesUsable( true );
                    taxonService.update( taxon );
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

    /**
     * @param geneQueue a blocking queue of genes to be loaded into the database loads genes into the database
     * @param converterFuture a future used to determine if the conversion of genes into the queue is done
     * @return
     */
    private long load( final BlockingQueue<Gene> geneQueue, Future<?> converterFuture ) {
        StopWatch timer = StopWatch.createStarted();
        long loadedGeneCount = 0;
        while ( !converterFuture.isDone() ) {
            Gene gene = null;
            try {
                gene = geneQueue.poll( 100, TimeUnit.MILLISECONDS );
                if ( gene == null ) {
                    continue; // will check if the future is done before retrying
                }
                persisterHelper.persistOrUpdate( gene );
            } catch ( Exception e ) {
                if ( gene != null ) {
                    log.error( String.format( "Failed to persist %s. The converter will be cancelled.", gene ) );
                }
                if ( e instanceof InterruptedException )
                    Thread.currentThread().interrupt();
                // cancel the converter if anything unexpected happens
                converterFuture.cancel( true );
                throw new RuntimeException( e );
            } finally {
                if ( ++loadedGeneCount % 1000 == 0 || timer.getTime( TimeUnit.SECONDS ) > 30 ) {
                    NcbiGeneLoader.log.info( "Processed " + loadedGeneCount + " genes. Queue has " + geneQueue.size()
                            + " items; last gene: " + gene );
                    timer.reset();
                    timer.start();
                }
            }
        }
        NcbiGeneLoader.log.info( "Loaded " + loadedGeneCount + " genes. " );
        return loadedGeneCount;
    }

    private long load( String geneInfoFile, String gene2AccFile, String geneHistoryFile, String geneEnsemblFile,
            Collection<Taxon> supportedTaxa ) {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator( supportedTaxa );
        sdog.setDoDownload( doDownload );
        sdog.setStartingNcbiId( startingNcbiId );

        NcbiGeneConverter converter = new NcbiGeneConverter();

        // create queue for GeneInfo objects
        final BlockingQueue<NcbiGeneData> geneInfoQueue = new ArrayBlockingQueue<>( NcbiGeneLoader.QUEUE_SIZE );
        final BlockingQueue<Gene> geneQueue = new ArrayBlockingQueue<>( NcbiGeneLoader.QUEUE_SIZE );

        // Threaded producer - loading files into queue as GeneInfo objects
        Future<?> generatorFuture;
        if ( StringUtils.isEmpty( geneInfoFile ) || StringUtils.isEmpty( geneInfoFile ) ) {
            generatorFuture = sdog.generateAsync( geneInfoQueue );
        } else {
            generatorFuture = sdog.generateLocalAsync( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, geneInfoQueue );
        }

        // Threaded consumer/producer - consumes GeneInfo objects and generates
        // Gene/GeneProduct/DatabaseEntry entries
        Future<?> converterFuture = converter.convertAsync( geneInfoQueue, geneQueue, generatorFuture );

        // Consumes Gene objects and persists them into the database
        long loadedGeneCount = this.load( geneQueue, converterFuture );

        // update taxon table to indicate that now there are genes loaded for that taxa.
        // all or nothing so that if fails for some taxa then no taxa will be updated.
        this.updateTaxaWithGenesUsable( sdog.getSupportedTaxaWithNCBIGenes() );

        return loadedGeneCount;
    }

}

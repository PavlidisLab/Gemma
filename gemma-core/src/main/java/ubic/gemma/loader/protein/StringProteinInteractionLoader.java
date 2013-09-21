/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.protein.biomart.BiomartEnsemblNcbiObjectGenerator;
import ubic.gemma.loader.protein.biomart.model.Ensembl2NcbiValueObject;
import ubic.gemma.loader.protein.string.StringProteinProteinInteractionObjectGenerator;
import ubic.gemma.loader.protein.string.model.StringProteinProteinInteraction;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.Persister;

/**
 * Loader class for loading protein protein interactions into Gemma from STRING. Either use local files or retrieve
 * files using fetchers , those files are from String and biomart sites. Once these files are located parse them and
 * generate value objects, and load them into the database.
 * <p>
 * We use BioMart to get the mappings from Ensembl to NCBI. For biomart these value objects(EnsembleNcbiValueObject) are
 * grouped into a map keyed on ensembl peptide id. For string these value objects StringProteinProteinInteraction are
 * grouped into arrays held in a map keyed on taxon. Then one taxon at a time StringBiomartProteinConverter converts
 * them into gemma objects using the BioMartEnsembleNcbi map to find the perptide ids corresponding to the ncbi gene.
 * The generated gemma objects Gene2GeneProteinAssociation are then loaded. It is done taxon by taxon due to the risk of
 * GC memory errors.
 * 
 * @author ldonnison
 * @version $Id$
 */

public class StringProteinInteractionLoader {

    private static Log log = LogFactory.getLog( StringProteinInteractionLoader.class );

    private int loadedGeneCount = 0;

    protected Persister persisterHelper;

    protected GeneService geneService;

    protected ExternalDatabaseService externalDatabaseService;

    protected TaxonService taxonService;

    private static final int QUEUE_SIZE = 1000;

    private AtomicBoolean converterDone;
    private AtomicBoolean loaderDone;

    /**
     * Constructor ensure that the concurrent flags are set.
     */
    public StringProteinInteractionLoader() {
        converterDone = new AtomicBoolean( false );
        loaderDone = new AtomicBoolean( false );
    }

    /**
     * Main method to load string protein protein interactions. Can either be supplied with files to load from or do
     * remote download. After files have been located/fetched the files are parsed and converted into value objects.
     * These value objects are then converted into GEMMA Gene2GeneProteinInteractions. Which are then loaded into the
     * database. Can be run on all eligable TAXA in gemma or on a supplied taxon.
     * 
     * @param stringProteinFileNameLocal The name of the string file on the local system
     * @param stringProteinFileNameRemote The name of the string file on the remote system (just in case the string name
     *        proves to be too variable)
     * @param localEnsembl2EntrezMappingFile The name of the local biomart file
     * @param taxa taxa to load data for. List of taxon to process
     * @throws IOException
     */
    public void load( File stringProteinFileNameLocal, String stringProteinFileNameRemote,
            File localEnsembl2EntrezMappingFile, Collection<Taxon> taxa ) throws IOException {

        // very basic validation before any processing done
        validateLoadParameters( stringProteinFileNameLocal, stringProteinFileNameRemote,
                localEnsembl2EntrezMappingFile, taxa );

        // retrieve STRING protein protein interactions
        StringProteinProteinInteractionObjectGenerator stringProteinProteinInteractionObjectGenerator = new StringProteinProteinInteractionObjectGenerator(
                stringProteinFileNameLocal, stringProteinFileNameRemote );
        Map<Taxon, Collection<StringProteinProteinInteraction>> map = stringProteinProteinInteractionObjectGenerator
                .generate( taxa );

        /*
         * Get ENSEMBL to NCBI id mappings so we can store the STRING interactions
         */
        Map<String, Ensembl2NcbiValueObject> bioMartStringEntreGeneMapping = getIdMappings(
                localEnsembl2EntrezMappingFile, taxa );

        // To one taxon at a time to reduce memory use
        for ( Taxon key : map.keySet() ) {
            log.debug( "Loading for taxon " + key );
            Collection<StringProteinProteinInteraction> proteinInteractions = map.get( key );
            log.info( "Found " + proteinInteractions.size() + " STRING interactions for taxon" + key );
            loadOneTaxonAtATime( bioMartStringEntreGeneMapping, proteinInteractions );
        }

    }

    /**
     * @param ensembl2entrezMappingFile
     * @param taxa
     * @return map between Ensembl peptide IDs and NCBI gene ids understood by Gemma.
     * @throws IOException
     */
    private Map<String, Ensembl2NcbiValueObject> getIdMappings( File ensembl2entrezMappingFile, Collection<Taxon> taxa )
            throws IOException {
        // retrieve a map of biomart objects keyed on ensembl peptide id to use as map between entrez gene ids and
        // ensemble ids
        BiomartEnsemblNcbiObjectGenerator biomartEnsemblNcbiObjectGenerator = new BiomartEnsemblNcbiObjectGenerator();
        biomartEnsemblNcbiObjectGenerator.setBioMartFileName( ensembl2entrezMappingFile );
        Map<String, Ensembl2NcbiValueObject> bioMartStringEntreGeneMapping = biomartEnsemblNcbiObjectGenerator
                .generate( taxa );
        return bioMartStringEntreGeneMapping;
    }

    /**
     * Method to generate and load Gene2GeneProteinAssociation one taxon at a time
     * 
     * @param ensembl2ncbi Map of peptide ids to NCBI gene ids
     * @param proteinInteractionsOneTaxon The protein interactions representing one taxon
     */
    public void loadOneTaxonAtATime( Map<String, Ensembl2NcbiValueObject> ensembl2ncbi,
            Collection<StringProteinProteinInteraction> proteinInteractionsOneTaxon ) {
        long startTime = System.currentTimeMillis();
        converterDone.set( false );
        loaderDone.set( false );
        loadedGeneCount = 0;
        // generate gemma objects
        StringProteinProteinInteractionConverter converter = new StringProteinProteinInteractionConverter( ensembl2ncbi );
        converter.setStringExternalDatabase( this.getExternalDatabaseForString() );

        // create queue for String objects to be converted
        final BlockingQueue<Gene2GeneProteinAssociation> gene2GeneProteinAssociationQueue = new ArrayBlockingQueue<Gene2GeneProteinAssociation>(
                QUEUE_SIZE );
        converter.setProducerDoneFlag( converterDone );
        converter.convert( gene2GeneProteinAssociationQueue, proteinInteractionsOneTaxon );

        // Threaded consumer. Consumes Gene objects and persists them into the database
        this.load( gene2GeneProteinAssociationQueue );
        log.debug( "Time taken to load data in minutes is "
                + ( ( ( System.currentTimeMillis() / 1000 ) - ( startTime ) / 1000 ) ) / 60 );

    }

    /**
     * Validate input parameters before processing with parsing and fetching. Should have been done already but should
     * not rely on calling class. Ensure that there are some valid taxa and that all files are ready to be processed.
     * 
     * @param stringProteinFileNameLocal The name of the string file on the local system
     * @param stringProteinFileNameRemote The name of the string file on the remote system (just in case the string name
     *        proves to be too variable)
     * @param stringBiomartFile The name of the local biomart file FIXME this is actual the ensemble mapping?
     * @param taxa taxa to load data for. List of taxon to process
     */
    private void validateLoadParameters( File stringProteinFileNameLocal, String stringProteinFileNameRemote,
            File stringBiomartFile, Collection<Taxon> taxa ) {

        if ( taxa == null || taxa.isEmpty() ) {
            throw new RuntimeException( "No taxon found to process please provide some" );
        }

        if ( stringProteinFileNameLocal == null || !stringProteinFileNameLocal.canRead() ) {
            throw new RuntimeException( "Provided local string file is not readable: " + stringProteinFileNameLocal );
        }

        // this is apparently allowed to be null, according to the tests.
        // if ( stringBiomartFile == null || !stringBiomartFile.canRead() ) {
        // throw new RuntimeException( "Provided biomart file is not readable: " + stringBiomartFile );
        // }

        // this is apparently allowed to be null, according to the tests.
        // if ( StringUtils.isBlank( stringProteinFileNameRemote ) ) {
        // throw new RuntimeException( "Provided remote string file is invalid (blank) " );
        // }
    }

    /**
     * Thead to handle loading Gene2GeneProteinAssociation into db.
     * 
     * @param geneQueue a blocking queue of genes to be loaded into the database loads genes into the database
     */
    private void load( final BlockingQueue<Gene2GeneProteinAssociation> gene2GeneProteinAssociationQueue ) {
        final SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                SecurityContextHolder.setContext( context );
                doLoad( gene2GeneProteinAssociationQueue );
            }

        }, "Loading" );
        loadThread.start();

        while ( !converterDone.get() || !loaderDone.get() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Poll the queue to see if any Gene2GeneProteinAssociation to load into database. If so firstly check to see if the
     * genes are in the gemma db as these identifiers came from biomart If both genes found load.
     * 
     * @param geneQueue queue of Gene2GeneProteinAssociation to load
     */
    void doLoad( final BlockingQueue<Gene2GeneProteinAssociation> gene2GeneProteinAssociationQueue ) {
        log.info( "starting processing " );
        while ( !( converterDone.get() && gene2GeneProteinAssociationQueue.isEmpty() ) ) {

            try {
                Gene2GeneProteinAssociation gene2GeneProteinAssociation = gene2GeneProteinAssociationQueue.poll();
                if ( gene2GeneProteinAssociation == null ) {
                    continue;
                }
                // check they are genes gemma knows about
                Gene geneOne = geneService.findByNCBIId( gene2GeneProteinAssociation.getFirstGene().getNcbiGeneId() );
                Gene geneTwo = geneService.findByNCBIId( gene2GeneProteinAssociation.getSecondGene().getNcbiGeneId() );

                if ( geneOne == null ) {
                    log.warn( "Gene with NCBI id=" + gene2GeneProteinAssociation.getFirstGene().getNcbiGeneId()
                            + " not in Gemma" );
                    continue;
                }
                if ( geneTwo == null ) {
                    log.warn( "Gene with NCBI id=" + gene2GeneProteinAssociation.getSecondGene().getNcbiGeneId()
                            + " not in Gemma" );
                    continue;
                }

                FieldUtils.writeField( gene2GeneProteinAssociation, "firstGene", geneOne, true );
                FieldUtils.writeField( gene2GeneProteinAssociation, "secondGene", geneTwo, true );

                persisterHelper.persist( gene2GeneProteinAssociation );

                if ( ++loadedGeneCount % 1000 == 0 ) {
                    log.info( "Proceesed " + loadedGeneCount + " protein protein interactions. " + "Current queue has "
                            + gene2GeneProteinAssociationQueue.size() + " items." );
                }

            } catch ( Exception e ) {
                log.error( e, e );
                loaderDone.set( true );
                throw new RuntimeException( e );
            }
        }
        log.info( "Loaded " + loadedGeneCount + " protein protein interactions. " );
        loaderDone.set( true );
    }

    /**
     * External database entry representing the string db
     * 
     * @return
     */
    public ExternalDatabase getExternalDatabaseForString() {
        ExternalDatabase externalDatabase = externalDatabaseService.find( "STRING" );
        return externalDatabase;
    }

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;

    }

    /**
     * @return the persisterHelper
     */
    public Persister getPersisterHelper() {
        return persisterHelper;
    }

    /**
     * PersisterHelper bean.
     * 
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * Number of genes successfully loaded.
     * 
     * @return the loadedGeneCount
     */
    public int getLoadedGeneCount() {
        return loadedGeneCount;
    }

    /**
     * @return the geneService
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public boolean isLoaderDone() {
        return loaderDone.get();
    }

}

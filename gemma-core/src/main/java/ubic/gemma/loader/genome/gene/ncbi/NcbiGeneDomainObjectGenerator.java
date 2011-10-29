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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.genome.gene.ncbi.model.NcbiGeneHistory;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.genome.Taxon;

/**
 * Combines information from the gene2accession and gene_info files from NCBI Gene.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGeneDomainObjectGenerator {

    private static final String GENEINFO_FILE = "gene_info";
    private static final String GENE2ACCESSION_FILE = "gene2accession";
    private static final String GENEHISTORY_FILE = "gene_history";
    private static final String GENEENSEMBL_FILE = "gene2ensembl";

    static Log log = LogFactory.getLog( NcbiGeneDomainObjectGenerator.class.getName() );
    AtomicBoolean producerDone = new AtomicBoolean( false );
    AtomicBoolean infoProducerDone = new AtomicBoolean( false );
    private Map<Integer, Taxon> supportedTaxa = null;
    private Collection<Taxon> supportedTaxaWithNCBIGenes = null;
    private boolean filter = true;

    public NcbiGeneDomainObjectGenerator( Collection<Taxon> taxa ) {

        if ( taxa != null ) {
            this.supportedTaxa = new HashMap<Integer, Taxon>();
            for ( Taxon t : taxa ) {
                if ( t.getNcbiId() == null ) {
                    log.warn( "Can't support NCBI genes for " + t + ", it lacks an NCBI id" );
                    continue;
                }
                this.supportedTaxa.put( t.getNcbiId(), t );
                if ( t.getSecondaryNcbiId() != null ) {
                    this.supportedTaxa.put( t.getSecondaryNcbiId(), t );
                }
            }
        } else {
            this.filter = false;
        }
    }

    /**
     * @return a collection of NCBIGene2Accession
     * @see ubic.gemma.loader.loaderutils.SourceDomainObjectGenerator#generate(java.lang.String)
     */
    public Collection<NCBIGene2Accession> generate( final BlockingQueue<NcbiGeneData> queue ) {

        log.info( "Fetching..." );
        NCBIGeneFileFetcher fetcher = new NCBIGeneFileFetcher();
        LocalFile geneInfoFile = fetcher.fetch( GENEINFO_FILE ).iterator().next();
        LocalFile gene2AccessionFile = fetcher.fetch( GENE2ACCESSION_FILE ).iterator().next();
        LocalFile geneHistoryFile = fetcher.fetch( GENEHISTORY_FILE ).iterator().next();
        LocalFile geneEnsemblFile = fetcher.fetch( GENEENSEMBL_FILE ).iterator().next();

        return processLocalFiles( geneInfoFile, gene2AccessionFile, geneHistoryFile, geneEnsemblFile, queue );
    }

    /**
     * Primarily for testing.
     * 
     * @param geneInfoFilePath
     * @param gene2AccesionFilePath
     * @return
     */
    public Collection<NCBIGene2Accession> generateLocal( String geneInfoFilePath, String gene2AccesionFilePath,
            String geneHistoryFilePath, String geneEnsemblFilePath, BlockingQueue<NcbiGeneData> queue ) {

        assert gene2AccesionFilePath != null;

        try {
            URL geneInfoUrl = ( new File( geneInfoFilePath ) ).toURI().toURL();
            URL gene2AccesionUrl = ( new File( gene2AccesionFilePath ) ).toURI().toURL();
            URL geneHistoryUrl = ( new File( geneHistoryFilePath ) ).toURI().toURL();

            URL geneEnsemblUrl = null;
            if ( geneEnsemblFilePath != null ) geneEnsemblUrl = ( new File( geneEnsemblFilePath ) ).toURI().toURL();

            assert geneInfoUrl != null;
            assert gene2AccesionUrl != null;

            LocalFile geneInfoFile = LocalFile.Factory.newInstance();
            geneInfoFile.setLocalURL( geneInfoUrl );

            LocalFile gene2AccessionFile = LocalFile.Factory.newInstance();
            gene2AccessionFile.setLocalURL( gene2AccesionUrl );

            LocalFile geneHistoryFile = LocalFile.Factory.newInstance();
            geneHistoryFile.setLocalURL( geneHistoryUrl );

            LocalFile geneEnsemblFile = null;
            if ( geneEnsemblFilePath != null ) {
                geneEnsemblFile = LocalFile.Factory.newInstance();
                geneEnsemblFile.setLocalURL( geneEnsemblUrl );
            }

            return processLocalFiles( geneInfoFile, gene2AccessionFile, geneHistoryFile, geneEnsemblFile, queue );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public boolean isProducerDone() {
        return producerDone.get();
    }

    public void setProducerDoneFlag( AtomicBoolean flag ) {
        this.producerDone = flag;
    }

    /**
     * This is the main entry point
     * 
     * @param geneInfoFile
     * @param gene2AccessionFile
     * @param geneHistoryFile
     * @param geneDataQueue
     * @return
     */
    private Collection<NCBIGene2Accession> processLocalFiles( final LocalFile geneInfoFile,
            final LocalFile gene2AccessionFile, LocalFile geneHistoryFile, LocalFile geneEnsemblFile,
            final BlockingQueue<NcbiGeneData> geneDataQueue ) {

        final NcbiGeneInfoParser infoParser = new NcbiGeneInfoParser();
        infoParser.setFilter( this.filter );

        if ( this.filter ) {
            infoParser.setSupportedTaxa( supportedTaxa.keySet() );
        }

        final NcbiGeneEnsemblFileParser ensemblParser = new NcbiGeneEnsemblFileParser();

        final NcbiGene2AccessionParser accParser = new NcbiGene2AccessionParser();
        final File gene2accessionFileHandle = gene2AccessionFile.asFile();

        final NcbiGeneHistoryParser historyParser = new NcbiGeneHistoryParser();

        try {
            log.debug( "Parsing gene history" );
            historyParser.parse( geneHistoryFile.asFile() );

            if ( geneEnsemblFile != null ) {
                log.debug( "Parsing ensembl" );
                ensemblParser.parse( geneEnsemblFile.asFile() );
            }

            //
            log.debug( "Parsing GeneInfo =" + geneInfoFile.asFile().getAbsolutePath() );
            InputStream is = FileTools
                    .getInputStreamFromPlainOrCompressedFile( geneInfoFile.asFile().getAbsolutePath() );
            infoParser.parse( is );
            is.close();
        } catch ( IOException e ) {
            // infoProducerDone.set( true );
            throw new RuntimeException( e );
        }

        Collection<NCBIGeneInfo> geneInfoList = infoParser.getResults();

        // put into HashMap
        final Map<String, NCBIGeneInfo> geneInfoMap = new HashMap<String, NCBIGeneInfo>();
        Map<Integer, Integer> taxaCount = new HashMap<Integer, Integer>();

        for ( NCBIGeneInfo geneInfo : geneInfoList ) {

            NcbiGeneHistory history = historyParser.get( geneInfo.getGeneId() );
            geneInfo.setHistory( history );

            if ( geneEnsemblFile != null ) {
                String ensemblId = ensemblParser.get( geneInfo.getGeneId() );
                geneInfo.setEnsemblId( ensemblId );
            }

            int taxId = geneInfo.getTaxId();
            if ( !taxaCount.containsKey( taxId ) ) {
                taxaCount.put( new Integer( taxId ), new Integer( 0 ) );
            }
            taxaCount.put( taxId, taxaCount.get( taxId ) + 1 );
            geneInfoMap.put( geneInfo.getGeneId(), geneInfo );
        }
        supportedTaxaWithNCBIGenes = new HashSet<Taxon>();
        if ( supportedTaxa != null ) {
            for ( Integer taxId : taxaCount.keySet() ) {

                if ( taxaCount.get( taxId ) > 0 ) {
                    log.debug( "Taxon " + taxId + ": " + taxaCount.get( taxId ) + " genes" );
                    Taxon t = supportedTaxa.get( taxId );
                    supportedTaxaWithNCBIGenes.add( t );
                }
            }
        }

        // 1) use a producer-consumer model for Gene2Accession conversion
        // 1a) Parse Gene2Accession until the gene id changes. This means that
        // all accessions for the gene are done.
        // 1b) Create a Collection<Gene2Accession>, and push into BlockingQueue

        Thread parseThread = new Thread( new Runnable() {
            public void run() {
                try {
                    log.debug( "Parsing gene2accession=" + gene2AccessionFile.asFile().getAbsolutePath() );
                    accParser.parse( gene2accessionFileHandle, geneDataQueue, geneInfoMap );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
                log.debug( "Domain object generator done" );
                producerDone.set( true );
            }
        }, "gene2accession parser" );

        parseThread.start();

        // 1c) As elements get added to BlockingQueue, NCBIGeneConverter
        // consumes
        // and creates Gene/GeneProduct/DatabaseEntry objects.
        // 1d) Push Gene to another BlockingQueue genePersistence

        // 2) use producer-consumer model for Gene persistence
        // 2a) as elements get added to genePersistence, persist Gene and
        // associated entries.

        return null;
    }

    // not used at all
    public Collection<?> generate( @SuppressWarnings("unused") String accession ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Those taxa that are supported by GEMMA and have genes in NCBI.
     * 
     * @return Collection of taxa that are supported by the GEMMA and have genes held by NCBI.
     */
    public Collection<Taxon> getSupportedTaxaWithNCBIGenes() {
        return supportedTaxaWithNCBIGenes;
    }

}

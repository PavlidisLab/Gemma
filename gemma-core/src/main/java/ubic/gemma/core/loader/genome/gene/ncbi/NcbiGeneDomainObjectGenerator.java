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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NcbiGeneHistory;
import ubic.gemma.core.util.concurrent.ThreadUtils;
import ubic.gemma.model.genome.Taxon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Combines information from the gene2accession and gene_info files from NCBI Gene.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class NcbiGeneDomainObjectGenerator {

    private static final Log log = LogFactory.getLog( NcbiGeneDomainObjectGenerator.class.getName() );
    private static final String GENEINFO_FILE = "gene_info";
    private static final String GENE2ACCESSION_FILE = "gene2accession";
    private static final String GENEHISTORY_FILE = "gene_history";
    private static final String GENEENSEMBL_FILE = "gene2ensembl";
    private AtomicBoolean producerDone = new AtomicBoolean( false );
    private Map<Integer, Taxon> supportedTaxa = null;
    private Collection<Taxon> supportedTaxaWithNCBIGenes = null;
    private boolean filter = true;

    // whether to fetch files from ncbi or use existing ones
    private boolean doDownload = true;
    private Integer startingNcbiId = null;

    public NcbiGeneDomainObjectGenerator( Collection<Taxon> taxa ) {

        if ( taxa != null ) {
            this.supportedTaxa = new HashMap<>();
            for ( Taxon t : taxa ) {
                if ( t.getNcbiId() == null ) {
                    NcbiGeneDomainObjectGenerator.log
                            .warn( "Can't support NCBI genes for " + t + ", it lacks an NCBI id" );
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

    public boolean isDoDownload() {
        return doDownload;
    }

    public void setDoDownload( boolean doDownload ) {
        this.doDownload = doDownload;
    }

    public void generate( final BlockingQueue<NcbiGeneData> queue ) {
        NcbiGeneDomainObjectGenerator.log.info( "Fetching..." );
        NCBIGeneFileFetcher fetcher = new NCBIGeneFileFetcher();
        fetcher.setDoDownload( this.doDownload );
        File geneInfoFile = fetcher.fetch( NcbiGeneDomainObjectGenerator.GENEINFO_FILE ).iterator().next();
        File gene2AccessionFile = fetcher.fetch( NcbiGeneDomainObjectGenerator.GENE2ACCESSION_FILE ).iterator().next();
        File geneHistoryFile = fetcher.fetch( NcbiGeneDomainObjectGenerator.GENEHISTORY_FILE ).iterator().next();
        File geneEnsemblFile = fetcher.fetch( NcbiGeneDomainObjectGenerator.GENEENSEMBL_FILE ).iterator().next();
        this.processFiles( geneInfoFile, gene2AccessionFile, geneHistoryFile, geneEnsemblFile, queue );
    }

    public void generateLocal( String geneInfoFilePath, String gene2AccesionFilePath, String geneHistoryFilePath,
            String geneEnsemblFilePath, BlockingQueue<NcbiGeneData> queue ) {
        assert gene2AccesionFilePath != null;
        File geneInfoFile = new File( geneInfoFilePath );
        File gene2AccessionFile = new File( gene2AccesionFilePath );
        File geneHistoryFile = new File( geneHistoryFilePath );
        File geneEnsemblFile = null;
        if ( geneEnsemblFilePath != null ) {
            geneEnsemblFile = new File( geneEnsemblFilePath );
        }
        this.processFiles( geneInfoFile, gene2AccessionFile, geneHistoryFile, geneEnsemblFile, queue );
    }

    public boolean isProducerDone() {
        return producerDone.get();
    }

    public void setProducerDoneFlag( AtomicBoolean flag ) {
        this.producerDone = flag;
    }

    /**
     * Those taxa that are supported by GEMMA and have genes in NCBI.
     *
     * @return Collection of taxa that are supported by the GEMMA and have genes held by NCBI.
     */
    public Collection<Taxon> getSupportedTaxaWithNCBIGenes() {
        return supportedTaxaWithNCBIGenes;
    }

    public void setStartingNcbiId( Integer startingNcbiId ) {
        this.startingNcbiId = startingNcbiId;
    }

    private void processFiles( final File geneInfoFile, final File gene2AccessionFile,
            File geneHistoryFile, File geneEnsemblFile, final BlockingQueue<NcbiGeneData> geneDataQueue ) {

        final NcbiGeneInfoParser infoParser = new NcbiGeneInfoParser();
        infoParser.setFilter( this.filter );

        if ( this.filter ) {
            infoParser.setSupportedTaxa( supportedTaxa.keySet() );
        }

        final NcbiGeneEnsemblFileParser ensemblParser = new NcbiGeneEnsemblFileParser();

        final NcbiGene2AccessionParser accParser = new NcbiGene2AccessionParser();
        accParser.setStartingNbiId( startingNcbiId );

        final NcbiGeneHistoryParser historyParser = new NcbiGeneHistoryParser();

        try {
            NcbiGeneDomainObjectGenerator.log.debug( "Parsing gene history" );
            historyParser.parse( geneHistoryFile );

            if ( geneEnsemblFile != null ) {
                NcbiGeneDomainObjectGenerator.log.debug( "Parsing ensembl" );
                ensemblParser.parse( geneEnsemblFile );
            }

            //
            NcbiGeneDomainObjectGenerator.log.debug( "Parsing GeneInfo =" + geneInfoFile.getAbsolutePath() );
            try ( InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( geneInfoFile.getAbsolutePath() ) ) {
                infoParser.parse( is );
            }
        } catch ( IOException e ) {
            // infoProducerDone.set( true );
            throw new RuntimeException( e );
        }

        Collection<NCBIGeneInfo> geneInfoList = infoParser.getResults();

        // put into HashMap
        final Map<String, NCBIGeneInfo> geneInfoMap = new HashMap<>();
        Map<Integer, Integer> taxaCount = new HashMap<>();

        for ( NCBIGeneInfo geneInfo : geneInfoList ) {

            NcbiGeneHistory history = historyParser.get( geneInfo.getGeneId() );
            geneInfo.setHistory( history );

            if ( history == null ) {
                String discontinuedIdForGene = historyParser
                        .discontinuedIdForSymbol( geneInfo.getDefaultSymbol(), geneInfo.getTaxId() );
                geneInfo.setDiscontinuedId( discontinuedIdForGene );
            }

            if ( geneEnsemblFile != null ) {
                String ensemblId = ensemblParser.get( geneInfo.getGeneId() );
                geneInfo.setEnsemblId( ensemblId );
            }

            int taxId = geneInfo.getTaxId();
            if ( !taxaCount.containsKey( taxId ) ) {
                taxaCount.put( taxId, 0 );
            }
            taxaCount.put( taxId, taxaCount.get( taxId ) + 1 );
            geneInfoMap.put( geneInfo.getGeneId(), geneInfo );
        }
        supportedTaxaWithNCBIGenes = new HashSet<>();
        if ( supportedTaxa != null ) {
            for ( Integer taxId : taxaCount.keySet() ) {

                if ( taxaCount.get( taxId ) > 0 ) {
                    NcbiGeneDomainObjectGenerator.log
                            .debug( "Taxon " + taxId + ": " + taxaCount.get( taxId ) + " genes" );
                    Taxon t = supportedTaxa.get( taxId );
                    supportedTaxaWithNCBIGenes.add( t );
                }
            }
        }

        // 1) use a producer-consumer model for Gene2Accession conversion
        // 1a) Parse Gene2Accession until the gene id changes. This means that
        // all accessions for the gene are done.
        // 1b) Create a Collection<Gene2Accession>, and push into BlockingQueue

        Thread parseThread = ThreadUtils.newThread( new Runnable() {
            @Override
            public void run() {
                try {
                    NcbiGeneDomainObjectGenerator.log.debug( "Parsing gene2accession=" + gene2AccessionFile.getAbsolutePath() );
                    accParser.setStartingNbiId( startingNcbiId );
                    accParser.parse( gene2AccessionFile, geneDataQueue, geneInfoMap );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
                NcbiGeneDomainObjectGenerator.log.debug( "Domain object generator done" );
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
    }

}

/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.loader.genome.gene;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.AbstractCLIContextCLI;

import java.io.File;

/**
 * Command line interface to gene parsing and loading
 * 
 * @author joseph
 * @version $Id$
 */
public class NcbiGeneLoaderCLI extends AbstractCLIContextCLI {
    private NcbiGeneLoader loader;

    private String GENEINFO_FILE = "gene_info.gz";
    private String GENE2ACCESSION_FILE = "gene2accession.gz";
    private String GENEHISTORY_FILE = "gene_history.gz";
    private String GENE2ENSEMBL_FILE = "gene2ensembl.gz";

    private String filePath = null;

    private String taxonCommonName = null;

    private boolean skipDownload = false;

    private Integer startNcbiid = null;

    public NcbiGeneLoaderCLI() {
        super();
    }

    public static void main( String[] args ) {
        NcbiGeneLoaderCLI p = new NcbiGeneLoaderCLI();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "NcbiGeneLoaderCLI", args );
        if ( err != null ) return err;
        loader = new NcbiGeneLoader();
        TaxonService taxonService = this.getBean( TaxonService.class );
        loader.setTaxonService( taxonService );
        loader.setPersisterHelper( this.getPersisterHelper() );
        loader.setSkipDownload( this.skipDownload );
        loader.setStartingNcbiId( startNcbiid );

        Taxon t = null;
        if ( StringUtils.isNotBlank( taxonCommonName ) ) {
            t = taxonService.findByCommonName( this.taxonCommonName );
            if ( t == null ) {
                throw new IllegalArgumentException( "Unrecognized taxon: " + taxonCommonName );
            }
        }

        if ( filePath != null ) {
            String geneInfoFile = filePath + File.separatorChar + GENEINFO_FILE;
            String gene2AccFile = filePath + File.separatorChar + GENE2ACCESSION_FILE;
            String geneHistoryFile = filePath + File.separatorChar + GENEHISTORY_FILE;
            String geneEnsemblFile = filePath + File.separatorChar + GENE2ENSEMBL_FILE;

            if ( t != null ) {
                loader.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, t );
            } else {
                loader.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, true ); // do filtering of
                                                                                                   // taxa
            }
        } else { /* defaults to download files remotely. */
            if ( t != null ) {
                loader.load( t );
            } else {
                loader.load( true );
            }
        }

        return null;
    }

    /**
     * @return Returns the loader
     */
    public NcbiGeneLoader getLoader() {
        return this.loader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.AbstractSpringAwareCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option pathOption = OptionBuilder.hasArg().withArgName( "Input File Path" )
                .withDescription( "Optional path to the gene_info and gene2accession files" ).withLongOpt( "file" )
                .create( 'f' );

        addOption( pathOption );

        addOption( "taxon", true, "Specific taxon for which to update genes" );

        addOption( "nodownload", false, "Set to suppress NCBI file download" );

        addOption( "restart", true, "Enter the NCBI ID of the gene you want to start on (implies -nodownload, "
                + "and assumes you have the right -taxon option, if any)" );

        requireLogin();
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            filePath = getOptionValue( 'f' );
        }
        if ( hasOption( "taxon" ) ) {
            this.taxonCommonName = getOptionValue( "taxon" );
        }
        if ( hasOption( "restart" ) ) {
            this.startNcbiid = Integer.parseInt( getOptionValue( "restart" ) );
            log.info( "Will attempt to pick up at ncbi gene id=" + startNcbiid );
            this.skipDownload = true;
        }
        if ( hasOption( "nodownload" ) ) {
            this.skipDownload = true;
        }
    }

}

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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.File;

/**
 * Command line interface to gene parsing and loading
 *
 * @author joseph
 */
public class NcbiGeneLoaderCLI extends AbstractCLIContextCLI {
    private static final String GENE_INFO_FILE = "gene_info.gz";
    private static final String GENE2ACCESSION_FILE = "gene2accession.gz";
    private static final String GENE_HISTORY_FILE = "gene_history.gz";
    private static final String GENE2ENSEMBL_FILE = "gene2ensembl.gz";
    private NcbiGeneLoader loader;
    private String filePath = null;

    private String taxonCommonName = null;

    private boolean skipDownload = false;

    private Integer startNcbiId = null;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public NcbiGeneLoaderCLI() {
        super();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }

    /**
     * @return Returns the loader
     */
    @SuppressWarnings("unused") // Possible external use
    public NcbiGeneLoader getLoader() {
        return this.loader;
    }

    @Override
    public String getCommandName() {
        return "geneUpdate";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions( Options options ) {
        Option pathOption = Option.builder( "f" ).hasArg().argName( "Input File Path" )
                .desc( "Optional path to the gene_info and gene2accession files" ).longOpt( "file" )
                .build();

        options.addOption( pathOption );

        options.addOption( Option.builder( "taxon" ).longOpt( null ).desc( "Specific taxon for which to update genes" ).argName( "taxon" ).hasArg().build() );

        options.addOption( "nodownload", "Set to suppress NCBI file download" );

        options.addOption( Option.builder( "restart" ).longOpt( null ).desc( "Enter the NCBI ID of the gene you want to start on (implies -nodownload, "
                + "and assumes you have the right -taxon option, if any)" ).argName( "ncbi id" ).hasArg().build() );
    }

    @Override
    protected boolean requireLogin() {
        return true;
    }


    @Override
    protected void doWork() throws Exception {
        loader = new NcbiGeneLoader();
        TaxonService taxonService = this.getBean( TaxonService.class );
        loader.setTaxonService( taxonService );
        loader.setGenePersister( this.getPersisterHelper() );
        loader.setSkipDownload( this.skipDownload );
        loader.setStartingNcbiId( startNcbiId );

        Taxon t = null;
        if ( StringUtils.isNotBlank( taxonCommonName ) ) {
            t = taxonService.findByCommonName( this.taxonCommonName );
            if ( t == null ) {
                throw new IllegalArgumentException( "Unrecognized taxon: " + taxonCommonName );
            }
        }

        if ( filePath != null ) {
            String geneInfoFile = filePath + File.separatorChar + NcbiGeneLoaderCLI.GENE_INFO_FILE;
            String gene2AccFile = filePath + File.separatorChar + NcbiGeneLoaderCLI.GENE2ACCESSION_FILE;
            String geneHistoryFile = filePath + File.separatorChar + NcbiGeneLoaderCLI.GENE_HISTORY_FILE;
            String geneEnsemblFile = filePath + File.separatorChar + NcbiGeneLoaderCLI.GENE2ENSEMBL_FILE;

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
    }

    @Override
    public String getShortDesc() {
        return "Load/update gene information";
    }

    @Override

    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'f' ) ) {
            filePath = commandLine.getOptionValue( 'f' );
        }
        if ( commandLine.hasOption( "taxon" ) ) {
            this.taxonCommonName = commandLine.getOptionValue( "taxon" );
        }
        if ( commandLine.hasOption( "restart" ) ) {
            this.startNcbiId = Integer.parseInt( commandLine.getOptionValue( "restart" ) );
            AbstractCLI.log.info( "Will attempt to pick up at ncbi gene id=" + startNcbiId );
            this.skipDownload = true;
        }
        if ( commandLine.hasOption( "nodownload" ) ) {
            this.skipDownload = true;
        }
    }

}

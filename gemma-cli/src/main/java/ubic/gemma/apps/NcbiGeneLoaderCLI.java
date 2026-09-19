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
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import ubic.gemma.core.loader.genome.gene.ncbi.GeneProductChangeTsv;
import ubic.gemma.core.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import org.springframework.lang.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import static ubic.gemma.cli.util.EntityOptionsUtils.addTaxonOption;
import static ubic.gemma.cli.util.OptionsUtils.requires;
import static ubic.gemma.cli.util.OptionsUtils.toBeSet;

/**
 * Command line interface to gene parsing and loading
 *
 * @author joseph
 */
public class NcbiGeneLoaderCLI extends AbstractAuthenticatedCLI {
    private static final String GENE_INFO_FILE = "gene_info.gz";
    private static final String GENE2ACCESSION_FILE = "gene2accession.gz";
    private static final String GENE_HISTORY_FILE = "gene_history.gz";
    private static final String GENE2ENSEMBL_FILE = "gene2ensembl.gz";
    private static final String NO_REMOVE_OPTION = "noRemove";
    private static final String REPORT_OPTION = "report";

    @Autowired
    private TaxonService taxonService;
    @Autowired
    private GeneWriteService geneWriteService;
    @Autowired
    private ExternalDatabaseService externalDatabaseService;
    @Autowired
    private EntityLocator entityLocator;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private SessionFactory sessionFactory;

    private NcbiGeneLoader loader;
    private String filePath = null;
    @Nullable
    private String taxonCommonName = null;
    private boolean skipDownload = false;
    private Integer startNcbiId = null;
    @Nullable
    private Integer limit = null;
    private boolean dryRun = false;
    private boolean noRemove = false;
    @Nullable
    private Path reportFile = null;

    public NcbiGeneLoaderCLI() {
        setRequireLogin();
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

    @Override
    protected void buildOptions( Options options ) {
        Option pathOption = Option.builder( "f" ).hasArg().argName( "Input File Path" )
                .desc( "Optional path to the gene_info and gene2accession files" ).longOpt( "file" )
                .build();

        options.addOption( pathOption );

        addTaxonOption( options, "taxon", "taxon", "Specific taxon for which to update genes" );

        options.addOption( "nodownload", "Set to suppress NCBI file download" );

        options.addOption( Option.builder( "restart" ).longOpt( null ).desc( "Enter the NCBI ID of the gene you want to start on (implies -nodownload, "
                + "and assumes you have the right -taxon option, if any)" ).argName( "ncbi id" ).hasArg().build() );

        options.addOption( Option.builder( "limit" ).longOpt( "limit" ).hasArg().argName( "number of genes" ).type( Number.class )
                .desc( "Stop after this many genes. No taxon is marked as having usable genes and the GENE database's last-updated date is not changed." )
                .build() );

        options.addOption( "dryRun", "dry-run", false, "Load each gene as usual, flush it to the database, then roll it back; "
                + "files are still downloaded. Reports the genes that would be created or updated, the gene products that would "
                + "be removed, and every gene that fails. Combine with -limit to rehearse a few genes." );

        options.addOption( Option.builder( NO_REMOVE_OPTION ).longOpt( "no-remove" )
                .desc( "Add and update genes and gene products, but remove no gene product: a product NCBI no longer lists "
                        + "stays attached to its gene, with the probe alignments that map probes to that gene. Each removal "
                        + "not made is written to the -" + REPORT_OPTION + " file, from which replayGeneProductRemovals "
                        + "applies it later. Gene products moved from one gene to another are still moved. Requires -"
                        + REPORT_OPTION + "." )
                .build() );
        options.addOption( Option.builder( REPORT_OPTION ).longOpt( "report" ).hasArg().argName( "file" ).type( Path.class )
                .desc( "Write a TSV file with one row per gene product removed (or, with -" + NO_REMOVE_OPTION + ", not "
                        + "removed) and per gene product moved from one gene to another, with the number of BLAT and "
                        + "annotation associations it has and the platforms they are on. The file must not already exist." )
                .build() );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        if ( reportFile == null ) {
            load( null );
        } else {
            // CREATE_NEW: a report records removals and moves that a later run cannot reproduce, so it is never
            // overwritten
            try ( GeneProductChangeTsv report = new GeneProductChangeTsv(
                    Files.newBufferedWriter( reportFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE ), dryRun ) ) {
                load( report );
            }
        }

        if ( dryRun ) {
            log.info( "Dry run: nothing was written." + ( reportFile != null ? " " + reportFile
                    + " lists the gene product changes that were rolled back." : "" ) );
            return;
        }

        if ( limit != null && loader.getLoadedGeneCount() >= limit ) {
            log.warn( "Stopped at the limit of " + limit + " genes; the " + ExternalDatabases.GENE + " database's last-updated date was not changed." );
        } else {
            ExternalDatabase ed = externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.GENE );
            if ( ed != null ) {
                externalDatabaseService.updateReleaseLastUpdated( ed, null, new Date() );
            } else {
                log.warn( String.format( "No external database with name %s.", ExternalDatabases.GENE ) );
            }
        }

        if ( noRemove ) {
            log.warn( "Gene product removals were NOT applied (-" + NO_REMOVE_OPTION + "): " + loader.getKeptGeneProducts()
                    + " gene products NCBI no longer lists are still attached to their genes. They are listed in "
                    + reportFile + "; apply them with replayGeneProductRemovals -report " + reportFile + "." );
        }
        log.info( "Gene records changed, so the probe-to-gene tables built from them are now stale: run updateGene2Cs, "
                + "then makePlatformAnnotFiles." );
    }

    private void load( @Nullable GeneProductChangeTsv report ) {
        loader = new NcbiGeneLoader();
        loader.setTaxonService( taxonService );
        loader.setGeneWriteService( geneWriteService );
        loader.setSkipDownload( this.skipDownload );
        loader.setStartingNcbiId( startNcbiId );
        loader.setLimit( limit );
        loader.setDryRun( dryRun );
        loader.setRemoveProducts( !noRemove );
        loader.setChangeSink( report );
        loader.setTransactionManager( transactionManager );
        loader.setSessionFactory( sessionFactory );

        Taxon t = null;
        if ( taxonCommonName != null ) {
            t = entityLocator.locateTaxon( this.taxonCommonName );
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
        return "Load/update gene information from NCBI";
    }

    @Override

    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( 'f' ) ) {
            filePath = commandLine.getOptionValue( 'f' );
        }
        if ( commandLine.hasOption( "taxon" ) ) {
            this.taxonCommonName = commandLine.getOptionValue( "taxon" );
        }
        if ( commandLine.hasOption( "restart" ) ) {
            this.startNcbiId = Integer.parseInt( commandLine.getOptionValue( "restart" ) );
            log.info( "Will attempt to pick up at ncbi gene id=" + startNcbiId );
            this.skipDownload = true;
        }
        if ( commandLine.hasOption( "nodownload" ) ) {
            this.skipDownload = true;
        }
        if ( commandLine.hasOption( "limit" ) ) {
            this.limit = ( ( Number ) commandLine.getParsedOptionValue( "limit" ) ).intValue();
            if ( this.limit <= 0 ) {
                throw new ParseException( "-limit must be a positive number of genes." );
            }
        }
        this.dryRun = commandLine.hasOption( "dryRun" );
        this.noRemove = OptionsUtils.hasOption( commandLine, NO_REMOVE_OPTION, requires( toBeSet( REPORT_OPTION ) ) );
        if ( commandLine.hasOption( REPORT_OPTION ) ) {
            this.reportFile = commandLine.getParsedOptionValue( REPORT_OPTION );
        }
    }

}

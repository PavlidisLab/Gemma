/*
 * The Gemma project.
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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Given an array design creates a Gene Ontology Annotation file Given a batch file creates all the Annotation files for
 * the AD's specified in the batch file Given nothing creates annotation files for every AD that isn't subsumed or
 * merged into another AD.
 *
 * @author klc
 * @author paul
 */
public class ArrayDesignAnnotationFileCli extends ArrayDesignSequenceManipulatingCli {

    private static final String GENE_NAME_LIST_FILE_OPTION = "genefile";
    private static final String FILE_LOAD_DESC = "Use specified file for batch generating annotation files.  "
            + " File is a list of shortNames (one per line); Overrides -a,-t,-f options ";
    private static final String BATCH_LOAD_DESC = "Generates annotation files for all eligible Array Designs "
            + " Overrides other selection methods but can be combined with '--taxon' ";
    private static final String GENE_LIST_FILE_DESC = "Create from a file containing a list of gene symbols instead of probe ids";
    private static final String TAXON_DESC = "Taxon short name e.g. 'mouse' (use with --genefile, or alone to process all "
            + "known genes for the taxon, or with --batch to process all arrays for the taxon.";
    private static final String OVERWRITE_DESC = "If set will overwrite existing annotation files in the output directory";
    // file info
    private String batchFileName;

  //  private boolean overWrite = false;
    private boolean processAllADs = false;
    private ArrayDesignAnnotationService arrayDesignAnnotationService;
    // private String geneFileName;
    private GeneOntologyService goService;
    private String taxonName;
    private boolean notifiedAboutGOState = false;
    private TaxonService taxonService;

    private boolean useGO = true;
    private boolean deleteOtherFiles = true; // should other files that incorporate the annotations be deleted?

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );

        Option fileLoading = Option.builder( "l" ).desc( ArrayDesignAnnotationFileCli.FILE_LOAD_DESC ).hasArg()
                .argName( "file of short names" ).build();

        Option batchLoading = Option.builder( "b" ).longOpt( "batch" ).desc( ArrayDesignAnnotationFileCli.BATCH_LOAD_DESC ).build();

//        Option geneListFile = Option.builder( "g" ).longOpt( GENE_NAME_LIST_FILE_OPTION ).desc( ArrayDesignAnnotationFileCli.GENE_LIST_FILE_DESC )
//                .hasArg().argName( "File of gene symbols" ).build();

        Option taxonNameOption = Option.builder( "t" ).longOpt( "taxon" ).hasArg().argName( "taxon name" )
                .desc( ArrayDesignAnnotationFileCli.TAXON_DESC ).build();

     //   Option overWriteOption = Option.builder( "o" ).longOpt( "overwrite" ).desc( ArrayDesignAnnotationFileCli.OVERWRITE_DESC ).build();

        Option skipGOOption = Option.builder( "nogo" ).longOpt( "nogo" ).desc( "Skip GO annotations" ).build();

        Option dontDeleteOtherFilesOption = Option.builder( "k" ).longOpt( "dontDeleteOtherFiles" ).desc( "Keep other files associated" +
                "with the platform such as data set flat files and DEA results. Use this option if the annotations haven't changed; default is to delete them" ).build();

        options.addOption( fileLoading );
        options.addOption( batchLoading );
     //   options.addOption( overWriteOption );
        options.addOption( taxonNameOption );
        options.addOption( skipGOOption );
        options.addOption( dontDeleteOtherFilesOption );
        //    options.addOption( geneListFile );

    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {

        if ( isAutoSeek() ) {
            throw new IllegalArgumentException( "This CLI doesn't support the auto option" );
        }

        if ( commandLine.hasOption( 'l' ) ) {
            this.batchFileName = commandLine.getOptionValue( 'l' );
        }

        if ( commandLine.hasOption( 'b' ) ) {
            this.processAllADs = true;

            if ( commandLine.hasOption( 'a' ) ) {
                throw new IllegalArgumentException(
                        "--batch overrides -a to run all platforms. If you want to run like --batch but for selected platforms use -a " );
            }

        }

        if ( commandLine.hasOption( "nogo" ) ) {
            AbstractCLI.log.info( "GO annotations will be ommitted (there will be a blank column) and only one annotation file will be generated instead of three." );
            this.useGO = false;
        }

//        if ( commandLine.hasOption( ArrayDesignAnnotationFileCli.GENE_NAME_LIST_FILE_OPTION ) ) {
//            this.geneFileName = commandLine.getOptionValue( ArrayDesignAnnotationFileCli.GENE_NAME_LIST_FILE_OPTION );
//            if ( !commandLine.hasOption( "taxon" ) ) {
//                throw new IllegalArgumentException( "You must specify the taxon when using --genefile" );
//            }
//            this.taxonName = commandLine.getOptionValue( "taxon" );
//
//        }

        if ( commandLine.hasOption( "taxon" ) ) {
            this.taxonName = commandLine.getOptionValue( "taxon" );
            if ( commandLine.hasOption( 'b' ) ) {
                AbstractCLI.log.info( "Will batch process array designs for " + this.taxonName );
            } else {
                AbstractCLI.log.info( "Will make generic file for all genes for " + this.taxonName );
            }
        }

        if ( commandLine.hasOption( "dontDeleteOtherFiles" ) ) {
            log.warn( "Deletion of other files associated with the platform has been disabled, such as data files and DEA results files. " +
                    "If the annotations have changed since the files were last generated, you will have to delete the file manually." );
            deleteOtherFiles = false;
        }

//        if ( commandLine.hasOption( 'o' ) )
//            this.overWrite = true;

        super.processOptions( commandLine );

        this.arrayDesignAnnotationService = this.getBean( ArrayDesignAnnotationService.class );
        this.goService = this.getBean( GeneOntologyService.class );
    }

    @Override
    public String getShortDesc() {
        return "Generate annotation files for platforms.";
    }

    @Override
    public String getCommandName() {
        return "makePlatformAnnotFiles";
    }

    @Override
    protected void doWork() throws Exception {
        this.taxonService = this.getBean( TaxonService.class );

        if ( this.useGO ) {
            OntologyUtils.ensureInitialized( goService );
        }

        log.info( "***** Annotation file(s) will be written to " + ArrayDesignAnnotationService.ANNOT_DATA_DIR + " ******" );

//        if ( StringUtils.isNotBlank( geneFileName ) ) {
//            this.processGeneList();
//        } else
        if ( processAllADs ) {
            this.processAllADs(); // 'batch'
        } else if ( batchFileName != null ) {
            this.processFromListInFile(); // list of ADs to run
        } else if ( this.taxonName != null ) {
            this.processGenesForTaxon(); // more or less a generic annotation by gene symbol
        } else {
            if ( this.getArrayDesignsToProcess().isEmpty() ) {
                throw new IllegalArgumentException( "You must specify a platform, a taxon, or batch." );
            }
            for ( ArrayDesign arrayDesign : this.getArrayDesignsToProcess() ) {

                this.processAD( arrayDesign );

            }
        }
    }

    private void processAD( ArrayDesign arrayDesign ) throws IOException {

        ArrayDesign thawed = getArrayDesignService().thawLite( arrayDesign );

        if ( thawed.getCurationDetails().getTroubled() ) {
            AbstractCLI.log.warn( "Troubled, will not generate annotation file: " + arrayDesign );
            return;
        }

        if ( thawed.getAlternativeTo() != null ) {
            AbstractCLI.log.warn( "Alternative CDF, will not generate annotation file: " + arrayDesign );
            return;
        }

        if ( thawed.getTechnologyType().equals( TechnologyType.SEQUENCING ) ) {
            AbstractCLI.log.warn( "Raw sequencing platform, will not generate annotation file: " + arrayDesign );
            return;
        }

        arrayDesignAnnotationService.create( arrayDesign, useGO, deleteOtherFiles );

    }

    /**
     * Goes over all the AD's in the database (possibly limited by taxon and 'auto') and creates 3 annotation files for
     * each AD. Uses the short name (GPLxxxxx) as the base name of the annotation file(s).
     */
    private void processAllADs() {

        Collection<ArrayDesign> candidates;
        Collection<ArrayDesign> toDo = new ArrayList<>();
        int numChecked = 0;

        Taxon taxon;
        if ( this.taxonName != null ) {
            taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
            }
            candidates = this.getArrayDesignService().findByTaxon( taxon );

        } else {
            candidates = this.getArrayDesignService().loadAll();
        }

        if ( candidates.isEmpty() ) {
            log.warn( "No platforms found as candidates, check options" );
            return;
        }

        log.info( candidates.size() + " candidate platforms for processing" );

        int numTroubledOrAlt = 0;
        int numSkippedUnneeded = 0;
        for ( ArrayDesign ad : candidates ) {

            ad = getArrayDesignService().thawLite( ad );

            if ( ad.getTechnologyType().equals( TechnologyType.SEQUENCING )
                    || ( ad.getTechnologyType().equals( TechnologyType.GENELIST ) ) ) {
                // We don't make files for platforms that don't have sequences. 
                continue;
            }

            if ( ad.getCurationDetails().getTroubled() ) {
                AbstractCLI.log.debug( "Troubled, skipping: " + ad );
                numTroubledOrAlt++;
                continue;
            }

            if ( ad.getAlternativeTo() != null ) {
                AbstractCLI.log.debug( "Alternative CDF, skipping: " + ad );
                numTroubledOrAlt++;
                continue;
            }

            if ( ad.getTechnologyType().equals( TechnologyType.SEQUENCING ) ) {
                AbstractCLI.log.warn( "Raw sequencing platform, will not generate annotation file: " + ad );
                continue;
            }

            toDo.add( ad );

            if ( ++numChecked % 100 == 0 ) {
                log.info( "Checked for need to run: " + numChecked + " platforms" );
            }

        }

        log.info( "Checked for need to run: " + numChecked + " platforms" );

        if ( numTroubledOrAlt > 0 ) {
            log.info( numTroubledOrAlt + " platforms are troubled or alternative CDFs and will be skipped." );
        }
        if ( numSkippedUnneeded > 0 ) {
            log.info( numSkippedUnneeded + " platforms don't need to be run." );
        }

        if ( toDo.isEmpty() ) {
            log.warn( "No platforms were found to need processing" );
            return;
        }

        log.info( toDo.size() + " platforms will be processed." );

        for ( ArrayDesign ad : toDo ) {
            try {
                this.processOneAD( ad );
            } catch ( Exception e ) {
                addErrorObject( ad, e );
            }
        }
    }

    /**
     * @throws IOException used for batch processing
     */
    private void processFromListInFile() throws IOException {

        AbstractCLI.log.info( "Loading platforms to annotate from " + this.batchFileName );
        InputStream is = new FileInputStream( this.batchFileName );
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {

            String line;
            int lineNumber = 0;
            while ( ( line = br.readLine() ) != null ) {
                lineNumber++;
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }

                String[] arguments = StringUtils.split( line, ',' );

                String accession = arguments[0];

                // Check the syntax of the given line
                if ( ( accession == null ) || StringUtils.isBlank( accession ) ) {
                    AbstractCLI.log.warn( "Incorrect line format in Batch Annotation file: Line " + lineNumber
                            + "Platform is required: " + line );
                    AbstractCLI.log.warn( "Unable to process that line. Skipping to next." );
                    continue;
                }

                ArrayDesign arrayDesign = this.locateArrayDesign( accession );

                try {
                    this.processAD( arrayDesign );
                } catch ( Exception e ) {
                    addErrorObject( arrayDesign, e );
                }

            }
        }
    }
//
//    private void processGeneList() throws IOException {
//        AbstractCLI.log.info( "Loading genes to annotate from " + geneFileName );
//        InputStream is = new FileInputStream( geneFileName );
//        try ( BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {
//            String line;
//            GeneService geneService = this.getBean( GeneService.class );
//            Taxon taxon = taxonService.findByCommonName( taxonName );
//            if ( taxon == null ) {
//                throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
//            }
//            Collection<Gene> genes = new HashSet<>();
//            while ( ( line = br.readLine() ) != null ) {
//                if ( StringUtils.isBlank( line ) ) {
//                    continue;
//                }
//                String[] arguments = StringUtils.split( line, '\t' );
//                String gene = arguments[0];
//                Gene g = geneService.findByOfficialSymbol( gene, taxon );
//                if ( g == null ) {
//                    AbstractCLI.log.info( "Gene: " + gene + " not found." );
//                    continue;
//                }
//                genes.add( g );
//            }
//            AbstractCLI.log.info( "File contained " + genes.size() + " potential gene symbols" );
//
//            int numProcessed = arrayDesignAnnotationService
//                    .generateAnnotationFile( new PrintWriter( System.out ), genes );
//            AbstractCLI.log.info( "Processed " + numProcessed + " genes that were found" );
//        }
//    }

    private void processGenesForTaxon() {
        GeneService geneService = this.getBean( GeneService.class );
        Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
        }
        AbstractCLI.log.info( "Processing all genes for " + taxon );
        Collection<Gene> genes = geneService.loadAll( taxon );
        AbstractCLI.log.info( "Taxon has " + genes.size() + " 'known' genes" );
        int numProcessed = arrayDesignAnnotationService
                .generateAnnotationFile( new PrintWriter( System.out ), genes, useGO );
        AbstractCLI.log.info( "Processed " + numProcessed + " genes that were found" );
    }

    private void processOneAD( ArrayDesign inputAd ) throws IOException {
        this.arrayDesignAnnotationService.create( inputAd, useGO, deleteOtherFiles );
        addSuccessObject( inputAd );

    }

}
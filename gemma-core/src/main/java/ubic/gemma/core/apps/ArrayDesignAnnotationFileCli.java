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

import gemma.gsec.SecurityService;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl.OutputType;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Given an array design creates a Gene Ontology Annotation file
 * Given a batch file creates all the Annotation files for the AD's specified in the batch file
 * Given nothing creates annotation files for every AD that isn't subsumed or merged into another AD.
 *
 * @author klc
 */
public class ArrayDesignAnnotationFileCli extends ArrayDesignSequenceManipulatingCli {

    private static final String GENE_NAME_LIST_FILE_OPTION = "genefile";
    private static final String ANNOT_DESC = "The name of the Annotation file to be generated [Default = Accession number]";
    private static final String ANNOT_TYPE_DESC =
            "Which GO terms to add to the annotation file:  short, long, or bioprocess; 'all' to generate all 3 "
                    + "[Default=short (no parents)]. If you select bioprocess, parents are not included.";
    private static final String FILE_LOAD_DESC = "Use specified file for batch generating annotation files.  "
            + "specified File format (per line): shortName,outputFileName,[short|long|biologicalProcess] Note:  Overrides -a,-t,-f command line options ";
    private static final String BATCH_LOAD_DESC =
            "Generates annotation files for all Array Designs (omits ones that are subsumed or merged) uses accession as annotation file name."
                    + "Creates 3 zip files for each AD, no parents, parents, biological process. Overrides all other settings except '--taxon'.";
    private static final String GENE_LIST_FILE_DESC = "Create from a file containing a list of gene symbols instead of probe ids";
    private static final String TAXON_DESC =
            "Taxon short name e.g. 'mouse' (use with --genefile, or alone to process all "
                    + "known genes for the taxon, or with --batch to process all arrays for the taxon.";
    private static final String OVERWRITE_DESC = "If set will overwrite existing annotation files in the output directory";
    // file info
    private String batchFileName;
    private String fileName = null;
    /**
     * Clobber existing file, if any.
     */
    private boolean overWrite = false;
    private boolean processAllADs = false;
    private OutputType type = OutputType.SHORT;
    private ArrayDesignAnnotationService arrayDesignAnnotationService;
    private CompositeSequenceService compositeSequenceService;
    private boolean doAllTypes = false;
    private String geneFileName;
    private GeneOntologyService goService;
    private String taxonName;

    public static void main( String[] args ) {
        ArrayDesignAnnotationFileCli p = new ArrayDesignAnnotationFileCli();
        AbstractCLIContextCLI.tryDoWork( p, args );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @SuppressWarnings("static-access") // This is a much more readable syntax in this case
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option annotationFileOption = OptionBuilder.hasArg().withArgName( "Annotation file name" )
                .withDescription( ArrayDesignAnnotationFileCli.ANNOT_DESC ).withLongOpt( "annotation" ).create( 'f' );

        Option annotationType = OptionBuilder.hasArg().withArgName( "Type of annotation file" )
                .withDescription( ArrayDesignAnnotationFileCli.ANNOT_TYPE_DESC ).withLongOpt( "type" ).create( 't' );

        Option fileLoading = OptionBuilder.hasArg().withArgName( "Batch Generating of annotation files" )
                .withDescription( ArrayDesignAnnotationFileCli.FILE_LOAD_DESC ).withLongOpt( "load" ).create( 'l' );

        Option batchLoading = OptionBuilder.withArgName( "Generating all annotation files" )
                .withDescription( ArrayDesignAnnotationFileCli.BATCH_LOAD_DESC ).withLongOpt( "batch" ).create( 'b' );

        Option geneListFile = OptionBuilder.hasArg().withDescription( ArrayDesignAnnotationFileCli.GENE_LIST_FILE_DESC )
                .create( ArrayDesignAnnotationFileCli.GENE_NAME_LIST_FILE_OPTION );
        this.addOption( geneListFile );

        Option taxonNameOption = OptionBuilder.hasArg().withDescription( ArrayDesignAnnotationFileCli.TAXON_DESC )
                .create( "taxon" );
        this.addOption( taxonNameOption );

        Option overWriteOption = OptionBuilder.withArgName( "Overwrites existing files" )
                .withDescription( ArrayDesignAnnotationFileCli.OVERWRITE_DESC ).withLongOpt( "overwrite" )
                .create( 'o' );

        this.addOption( annotationFileOption );
        this.addOption( annotationType );
        this.addOption( fileLoading );
        this.addOption( batchLoading );
        this.addOption( overWriteOption );
    }

    @Override
    protected void processOptions() {

        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }

        if ( this.hasOption( 't' ) ) {
            if ( this.getOptionValue( 't' ).equalsIgnoreCase( "all" ) ) {
                this.doAllTypes = true;
            } else {
                this.type = OutputType.valueOf( this.getOptionValue( 't' ).toUpperCase() );
            }
        }

        if ( this.hasOption( 'l' ) ) {
            this.batchFileName = this.getOptionValue( 'l' );
        }

        if ( this.hasOption( 'b' ) ) {
            this.processAllADs = true;

            if ( this.hasOption( 'a' ) ) {
                throw new IllegalArgumentException(
                        "--batch overrides -a to run all platforms. If you want to run like --batch but for selected platforms use -a with -t all" );
            }

        }

        if ( this.hasOption( ArrayDesignAnnotationFileCli.GENE_NAME_LIST_FILE_OPTION ) ) {
            this.geneFileName = this.getOptionValue( ArrayDesignAnnotationFileCli.GENE_NAME_LIST_FILE_OPTION );
            if ( !this.hasOption( "taxon" ) ) {
                throw new IllegalArgumentException( "You must specify the taxon when using --genefile" );
            }
            this.taxonName = this.getOptionValue( "taxon" );

        }

        if ( this.hasOption( "taxon" ) ) {
            this.taxonName = this.getOptionValue( "taxon" );
            if ( this.hasOption( 'b' ) ) {
                AbstractCLI.log.info( "Will batch process array designs for " + this.taxonName );
            }
        }

        if ( this.hasOption( 'o' ) )
            this.overWrite = true;

        super.processOptions();

        this.arrayDesignAnnotationService = this.getBean( ArrayDesignAnnotationService.class );
        this.goService = this.getBean( GeneOntologyService.class );
        this.compositeSequenceService = this.getBean( CompositeSequenceService.class );
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
    protected Exception doWork( String[] args ) {
        Exception err = this.processCommandLine( args );
        if ( err != null )
            return err;

        try {
            this.goService.init( true );
            this.waitForGeneOntologyReady();

            if ( StringUtils.isNotBlank( geneFileName ) ) {
                this.processGeneList();
            } else if ( processAllADs ) {
                this.processAllADs();
            } else if ( batchFileName != null ) {
                this.processBatchFile();
            } else if ( this.taxonName != null ) {
                this.processGenesForTaxon();
            } else {
                if ( this.arrayDesignsToProcess.isEmpty() ) {
                    throw new IllegalArgumentException( "You must specify a platform, a taxon, gene file, or batch." );
                }
                for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {
                    if ( doAllTypes ) {
                        // make all three
                        this.processOneAD( arrayDesign );
                    } else {
                        this.processAD( arrayDesign, this.fileName, type );
                    }
                }
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    private void processAD( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType ) throws IOException {

        AbstractCLI.log.info( "Loading gene information for " + arrayDesign );
        ArrayDesign thawed = this.thaw( arrayDesign );

        if ( thawed.getCurationDetails().getTroubled() ) {
            AbstractCLI.log.warn( "Troubled: " + arrayDesign );
            return;
        }

        Collection<CompositeSequence> compositeSequences = thawed.getCompositeSequences();

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity = compositeSequenceService
                .getGenesWithSpecificity( compositeSequences );

        boolean hasAtLeastOneGene = false;
        for ( CompositeSequence c : genesWithSpecificity.keySet() ) {
            if ( genesWithSpecificity.get( c ).isEmpty() ) {
                continue;
            }
            hasAtLeastOneGene = true;
            break;
        }

        if ( !hasAtLeastOneGene ) {
            AbstractCLI.log.warn( "No genes: " + thawed );
            return;
        }

        AbstractCLI.log.info( "Preparing file" );
        this.processCompositeSequences( thawed, fileBaseName, outputType, genesWithSpecificity );

    }

    /**
     * Goes over all the AD's in the database (possibly limited by taxon) and creates annotation 3 annotation files for
     * each AD that is not merged into or subsumed by another AD. Uses the Accession ID (GPL???) for the name of the
     * annotation file. Appends noParents, bioProcess, allParents to the file name.
     */
    private void processAllADs() throws IOException {

        Collection<ArrayDesign> allADs = this.arrayDesignService.loadAll();

        for ( ArrayDesign ad : allADs ) {

            ad = arrayDesignService.thawLite( ad );
            if ( ad.getCurationDetails().getTroubled() ) {
                AbstractCLI.log.warn( "Troubled: " + ad + " (skipping)" );
                continue;
            }

            Taxon taxon = null;
            if ( this.taxonName != null ) {
                TaxonService taxonService = this.getBean( TaxonService.class );
                taxon = taxonService.findByCommonName( taxonName );
                if ( taxon == null ) {
                    throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
                }
            }

            Collection<Taxon> adTaxa = arrayDesignService.getTaxa( ad.getId() );

            /*
             * If using taxon, check it.
             */
            if ( taxon != null && !adTaxa.contains( taxon ) ) {
                continue;
            }
            this.processOneAD( ad );

        }

    }

    /**
     * @throws IOException used for batch processing
     */
    private void processBatchFile() throws IOException {

        AbstractCLI.log.info( "Loading platforms to annotate from " + this.batchFileName );
        InputStream is = new FileInputStream( this.batchFileName );
        try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {

            String line;
            int lineNumber = 0;
            while ( ( line = br.readLine() ) != null ) {
                lineNumber++;
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }

                String[] arguments = StringUtils.split( line, ',' );

                String accession = arguments[0];
                String annotationFileName = arguments[1];
                String type = arguments[2];

                // Check the syntax of the given line
                if ( ( accession == null ) || StringUtils.isBlank( accession ) ) {
                    AbstractCLI.log.warn( "Incorrect line format in Batch Annotation file: Line " + lineNumber
                            + "Platform is required: " + line );
                    AbstractCLI.log.warn( "Unable to process that line. Skipping to next." );
                    continue;
                }
                if ( ( annotationFileName == null ) || StringUtils.isBlank( annotationFileName ) ) {
                    annotationFileName = accession;
                    AbstractCLI.log.warn( "No annotation file name specified on line: " + lineNumber
                            + " Using platform name as default annotation file name" );
                }
                if ( StringUtils.isBlank( type ) ) {
                    this.type = OutputType.SHORT;
                    AbstractCLI.log.warn( "No type specified for line: " + lineNumber + " Defaulting to short" );
                } else {
                    this.type = OutputType.valueOf( type.toUpperCase() );
                }

                // need to set these so processing ad works correctly
                // TODO: make process type take all 3 parameter
                ArrayDesign arrayDesign = this.locateArrayDesign( accession, arrayDesignService );

                try {
                    this.processAD( arrayDesign, annotationFileName, this.type );
                } catch ( Exception e ) {
                    AbstractCLI.log.error( "**** Exception while processing " + arrayDesign + ": " + e.getMessage()
                            + " ********" );
                    AbstractCLI.log.error( e, e );
                    errorObjects.add( arrayDesign + ": " + e.getMessage() );
                }

            }
        }

        this.summarizeProcessing();

    }

    private void audit( ArrayDesign arrayDesign, String note ) {

        SecurityService ss = this.getBean( SecurityService.class );
        if ( !ss.isEditable( arrayDesign ) )
            return;

        AuditEventType eventType = ArrayDesignAnnotationFileEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    private void processCompositeSequences( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity ) throws IOException {

        if ( genesWithSpecificity.size() == 0 ) {
            AbstractCLI.log.info( "No sequence information for " + arrayDesign + ", skipping" );
            return;
        }

        try (Writer writer = arrayDesignAnnotationService.initOutputFile( arrayDesign, fileBaseName, this.overWrite )) {

            // if no writer then we should abort (this could happen in case where we don't want to overwrite files)
            if ( writer == null ) {
                AbstractCLI.log.info( arrayDesign.getName() + " annotation file already exits.  Skipping. " );
                return;
            }

            AbstractCLI.log
                    .info( arrayDesign.getName() + " has " + genesWithSpecificity.size() + " composite sequences" );

            int numProcessed = arrayDesignAnnotationService
                    .generateAnnotationFile( writer, genesWithSpecificity, outputType );

            AbstractCLI.log.info( "Finished processing platform: " + arrayDesign.getName() );

            successObjects.add( String.format( "%s (%s)", arrayDesign.getName(), arrayDesign.getShortName() ) );

            if ( StringUtils.isBlank( fileBaseName ) ) {
                AbstractCLI.log.info( "Processed " + numProcessed + " composite sequences" );
                this.audit( arrayDesign, "Processed " + numProcessed + " composite sequences" );
            } else {
                String filename = fileBaseName + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
                AbstractCLI.log.info( "Created file:  " + filename + " with " + numProcessed + " values" );
                this.audit( arrayDesign, "Created file: " + filename + " with " + numProcessed + " values" );
            }
        }
    }

    private void processGeneList() throws IOException {
        AbstractCLI.log.info( "Loading genes to annotate from " + geneFileName );
        InputStream is = new FileInputStream( geneFileName );
        try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
            String line;
            GeneService geneService = this.getBean( GeneService.class );
            TaxonService taxonService = this.getBean( TaxonService.class );
            Taxon taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
            }
            Collection<Gene> genes = new HashSet<>();
            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }
                String[] arguments = StringUtils.split( line, '\t' );
                String gene = arguments[0];
                Gene g = geneService.findByOfficialSymbol( gene, taxon );
                if ( g == null ) {
                    AbstractCLI.log.info( "Gene: " + gene + " not found." );
                    continue;
                }
                genes.add( g );
            }
            AbstractCLI.log.info( "File contained " + genes.size() + " potential gene symbols" );
            int numProcessed = arrayDesignAnnotationService
                    .generateAnnotationFile( new PrintWriter( System.out ), genes, OutputType.SHORT );
            AbstractCLI.log.info( "Processed " + numProcessed + " genes that were found" );
        }
    }

    private void processGenesForTaxon() {
        GeneService geneService = this.getBean( GeneService.class );
        TaxonService taxonService = this.getBean( TaxonService.class );
        Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
        }
        AbstractCLI.log.info( "Processing all genes for " + taxon );
        Collection<Gene> genes = geneService.loadAll( taxon );
        AbstractCLI.log.info( "Taxon has " + genes.size() + " 'known' genes" );
        int numProcessed = arrayDesignAnnotationService
                .generateAnnotationFile( new PrintWriter( System.out ), genes, type );
        AbstractCLI.log.info( "Processed " + numProcessed + " genes that were found" );
    }

    private void processOneAD( ArrayDesign inputAd ) throws IOException {
        ArrayDesign ad = this.thaw( inputAd );

        AbstractCLI.log.info( "Processing AD: " + ad.getName() );

        String shortFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        File sf = ArrayDesignAnnotationServiceImpl.getFileName( shortFileBaseName );
        String bioFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        File bf = ArrayDesignAnnotationServiceImpl.getFileName( bioFileBaseName );
        String allParFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        File af = ArrayDesignAnnotationServiceImpl.getFileName( allParFileBaseName );

        if ( !overWrite && sf.exists() && bf.exists() && af.exists() ) {
            AbstractCLI.log.info( "Files exist already, will not overwrite (use --overwrite option to override)" );
            return;
        }

        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        AbstractCLI.log.info( "Starting getting probe specificity" );

        // lmd test

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity = compositeSequenceService
                .getGenesWithSpecificity( compositeSequences );

        AbstractCLI.log.info( "Done getting probe specificity" );

        boolean hasAtLeastOneGene = false;
        for ( CompositeSequence c : genesWithSpecificity.keySet() ) {
            if ( genesWithSpecificity.get( c ).isEmpty() ) {
                continue;
            }
            hasAtLeastOneGene = true;
            break;
        }

        if ( !hasAtLeastOneGene ) {
            AbstractCLI.log.warn( "No genes: " + ad + ", skipping" );
            return;
        }

        if ( overWrite || !sf.exists() ) {
            this.processCompositeSequences( ad, shortFileBaseName, OutputType.SHORT, genesWithSpecificity );
        } else {
            AbstractCLI.log.info( sf + " exists, will not overwrite" );
        }

        if ( overWrite || !bf.exists() ) {
            this.processCompositeSequences( ad, bioFileBaseName, OutputType.BIOPROCESS, genesWithSpecificity );
        } else {
            AbstractCLI.log.info( bf + " exists, will not overwrite" );
        }

        if ( overWrite || !af.exists() ) {
            this.processCompositeSequences( ad, allParFileBaseName, OutputType.LONG, genesWithSpecificity );
        } else {
            AbstractCLI.log.info( af + " exists, will not overwrite" );
        }
    }

    private void waitForGeneOntologyReady() {
        while ( !goService.isReady() ) {
            try {
                Thread.sleep( 10000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
            AbstractCLI.log.info( "Waiting for Gene Ontology to load ..." );
        }
        AbstractCLI.log.info( "GO is ready." );
    }
}
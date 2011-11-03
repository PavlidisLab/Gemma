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
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.analysis.service.ArrayDesignAnnotationService.OutputType;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.providers.GeneOntologyService;

/**
 * Given an array design creates a Gene Ontology Annotation file
 * <p>
 * Given a batch file creates all the Annotation files for the AD's specified in the batch file
 * <p>
 * AGiven nothing creates annotation files for every AD that isn't subsumed or merged into another AD.
 * 
 * @author klc
 * @version $Id$
 */
public class ArrayDesignAnnotationFileCli extends ArrayDesignSequenceManipulatingCli {

    private static final String GENENAME_LISTFILE_OPTION = "genefile";

    public static void main( String[] args ) {
        ArrayDesignAnnotationFileCli p = new ArrayDesignAnnotationFileCli();
        try {

            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            System.exit( 0 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    ArrayDesignAnnotationService arrayDesignAnnotationService;

    CompositeSequenceService compositeSequenceService;

    GeneOntologyService goService;

    // file info
    String batchFileName;

    boolean processAllADs = false;

    String fileName = null;

    /**
     * Include predicted genes and probe aligned regions in the output
     */
    boolean includePredictedGenes = false;

    /**
     * Clobber existing file, if any.
     */
    boolean overWrite = false;

    OutputType type = OutputType.SHORT;

    private String geneFileName;

    private String taxonName;

    private boolean doAllTypes = false;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option annotationFileOption = OptionBuilder.hasArg().withArgName( "Annotation file name" )
                .withDescription( "The name of the Annotation file to be generated [Default = Accession number]" )
                .withLongOpt( "annotation" ).create( 'f' );

        Option genesIncludedOption = OptionBuilder
                .hasArg()
                .withArgName( "Genes to include" )
                .withDescription(
                        "The type of genes that will be included: all or standard."
                                + " All includes predicted genes and probe aligned regions. "
                                + "Standard mode only includes known genes [Default = standard]" )
                .withLongOpt( "genes" ).create( 'g' );

        Option annotationType = OptionBuilder
                .hasArg()
                .withArgName( "Type of annotation file" )
                .withDescription(
                        "Which GO terms to add to the annotation file:  short, long, or bioprocess; 'all' to generate all 3 "
                                + "[Default=short (no parents)]. If you select bioprocess, parents are not included." )
                .withLongOpt( "type" ).create( 't' );

        Option fileLoading = OptionBuilder
                .hasArg()
                .withArgName( "Batch Generating of annotation files" )
                .withDescription(
                        "Use specified file for batch generating annotation files.  "
                                + "specified File format (per line): shortName,outputFileName,[short|long|biologicalprocess] Note:  Overrides -a,-t,-f command line options " )
                .withLongOpt( "load" ).create( 'l' );

        Option batchLoading = OptionBuilder
                .withArgName( "Generating all annotation files" )
                .withDescription(
                        "Generates annotation files for all Array Designs (omits ones that are subsumed or merged) uses accession as annotation file name."
                                + "Creates 3 zip files for each AD, no parents, parents, biological process. Overrides all other settings except '--taxon'." )
                .withLongOpt( "batch" ).create( 'b' );

        Option geneListFile = OptionBuilder.hasArg()
                .withDescription( "Create from a file containing a list of gene symbols instead of probe ids" )
                .create( GENENAME_LISTFILE_OPTION );
        addOption( geneListFile );

        Option taxonNameOption = OptionBuilder
                .hasArg()
                .withDescription(
                        "Taxon short name e.g. 'mouse' (use with --genefile, or alone to process all "
                                + "known genes for the taxon, or with --all-arrays to process all arrays for the taxon." )
                .create( "taxon" );
        addOption( taxonNameOption );

        Option overWriteOption = OptionBuilder.withArgName( "Overwrites existing files" )
                .withDescription( "If set will overwrite existing annotation files in the output directory" )
                .withLongOpt( "overwrite" ).create( 'o' );

        addOption( annotationFileOption );
        addOption( annotationType );
        addOption( fileLoading );
        addOption( genesIncludedOption );
        addOption( batchLoading );
        addOption( overWriteOption );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design probe ontology annotation ", args );
        if ( err != null ) return err;

        try {
            this.goService.init( true );
            waitForGeneOntologyReady();

            if ( StringUtils.isNotBlank( geneFileName ) ) {
                processGeneList();
            } else if ( processAllADs ) {
                processAllADs();
            } else if ( batchFileName != null ) {
                processBatchFile();
            } else if ( this.taxonName != null ) {
                processGenesForTaxon();
            } else {
                if ( this.arrayDesignsToProcess.isEmpty() ) {
                    throw new IllegalArgumentException(
                            "You must specify an array design, a taxon, gene file, or batch." );
                }
                for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {
                    if ( doAllTypes ) {
                        // make all three
                        processOneAD( arrayDesign );
                    } else {
                        processAD( arrayDesign, this.fileName, type );
                    }
                }
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /**
     * @param outputType
     * @throws IOException
     */
    protected boolean processAD( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType )
            throws IOException {

        log.info( "Loading gene information for " + arrayDesign );

        /*
         * FIXME: first Check if file exists.
         */

        if ( arrayDesign.getStatus().getTroubled() ) {
            log.warn( "Troubled: " + arrayDesign );
            return false;
        }

        ArrayDesign thawed = unlazifyArrayDesign( arrayDesign );

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
            log.warn( "No genes: " + arrayDesign );
            return false;
        }

        log.info( "Preparing file" );
        return processCompositeSequences( thawed, fileBaseName, outputType, genesWithSpecificity );

    }

    /**
     * Goes over all the AD's in the database (possibly limited by taxon) and creates annotation 3 annotation files for
     * each AD that is not merged into or subsumed by another AD. Uses the Accession ID (GPL???) for the name of the
     * annotation file. Appends noparents, bioProcess, allParents to the file name.
     * 
     * @throws IOException
     */
    protected void processAllADs() throws IOException {

        Collection<ArrayDesign> allADs = this.arrayDesignService.loadAll();

        this.includePredictedGenes = false;

        for ( ArrayDesign ad : allADs ) {

            if ( ad.getStatus().getTroubled() ) {
                log.warn( "Troubled: " + ad + " (skipping)" );
                continue;
            }

            Taxon taxon = null;
            if ( this.taxonName != null ) {
                TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
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
            processOneAD( ad );

        }

    }

    /**
     * @param fileName
     * @throws IOException used for batch processing
     */
    protected void processBatchFile() throws IOException {

        log.info( "Loading platforms to annotate from " + this.batchFileName );
        InputStream is = new FileInputStream( this.batchFileName );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        int lineNumber = 0;
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;
            if ( StringUtils.isBlank( line ) ) {
                continue;
            }

            String[] arguments = StringUtils.split( line, ',' );

            String accession = arguments[0];
            String annotationFileName = arguments[1];
            String typeo = arguments[2];

            // Check the syntax of the given line
            if ( ( accession == null ) || StringUtils.isBlank( accession ) ) {
                log.warn( "Incorrect line format in Batch Annotation file: Line " + lineNumber
                        + "Platform is required: " + line );
                log.warn( "Unable to process that line. Skipping to next." );
                continue;
            }
            if ( ( annotationFileName == null ) || StringUtils.isBlank( annotationFileName ) ) {
                annotationFileName = accession;
                log.warn( "No annotation file name specified on line: " + lineNumber
                        + " Using platform name as default annotation file name" );
            }
            if ( StringUtils.isBlank( typeo ) ) {
                this.type = OutputType.SHORT;
                log.warn( "No type specifed for line: " + lineNumber + " Defaulting to short" );
            } else {
                type = OutputType.valueOf( typeo.toUpperCase() );
            }

            // need to set these so processing ad works correctly (todo: make
            // processtype take all 3 parameter)
            ArrayDesign arrayDesign = locateArrayDesign( accession );

            try {
                processAD( arrayDesign, annotationFileName, type );
            } catch ( Exception e ) {
                log.error( "**** Exception while processing " + arrayDesign + ": " + e.getMessage() + " ********" );
                log.error( e, e );
                cacheException( e );
                errorObjects.add( arrayDesign + ": " + e.getMessage() );
                continue;
            }

        }

        summarizeProcessing();

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
        }

        if ( this.hasOption( GENENAME_LISTFILE_OPTION ) ) {
            this.geneFileName = this.getOptionValue( GENENAME_LISTFILE_OPTION );
            if ( !this.hasOption( "taxon" ) ) {
                throw new IllegalArgumentException( "You must specify the taxon when using --genefile" );
            }
            this.taxonName = this.getOptionValue( "taxon" );

        }

        if ( this.hasOption( "taxon" ) ) {
            this.taxonName = this.getOptionValue( "taxon" );
            if ( this.hasOption( 'b' ) ) {
                log.info( "Will batch process array designs for " + this.taxonName );
            }
        }

        if ( this.hasOption( 'g' ) ) processGenesIncluded( this.getOptionValue( 'g' ) );

        if ( this.hasOption( 'o' ) ) this.overWrite = true;

        super.processOptions();

        this.arrayDesignAnnotationService = ( ArrayDesignAnnotationService ) getBean( "arrayDesignAnnotationService" );
        this.goService = ( GeneOntologyService ) getBean( "geneOntologyService" );
        this.compositeSequenceService = ( CompositeSequenceService ) getBean( "compositeSequenceService" );
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignAnnotationFileEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    /**
     * @param arrayDesign
     * @param fileBaseName
     * @param outputType
     * @param writer
     * @param genesWithSpecificity
     * @return true if the file was made.
     * @throws IOException
     */
    private boolean processCompositeSequences( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity ) throws IOException {

        if ( genesWithSpecificity.size() == 0 ) {
            log.info( "No sequence information for " + arrayDesign + ", skipping" );
            return false;
        }

        Writer writer = arrayDesignAnnotationService.initOutputFile( arrayDesign, fileBaseName, this.overWrite );

        // if no writer then we should abort (this could happen in case where we don't want to overwrite files)
        if ( writer == null ) {
            log.info( arrayDesign.getName() + " annotation file already exits.  Skipping. " );
            return false;
        }

        log.info( arrayDesign.getName() + " has " + genesWithSpecificity.size() + " composite sequences" );

        int numProcessed = arrayDesignAnnotationService.generateAnnotationFile( writer, genesWithSpecificity,
                outputType, !this.includePredictedGenes );

        log.info( "Finished processing platform: " + arrayDesign.getName() );

        successObjects.add( String.format( "%s (%s)", arrayDesign.getName(), arrayDesign.getShortName() ) );

        if ( StringUtils.isBlank( fileBaseName ) ) {
            log.info( "Processed " + numProcessed + " composite sequences" );
            audit( arrayDesign, "Processed " + numProcessed + " composite sequences" );
        } else {
            String filename = fileBaseName + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
            log.info( "Created file:  " + filename + " with " + numProcessed + " values" );
            audit( arrayDesign, "Created file: " + filename + " with " + numProcessed + " values" );
        }
        return true;
    }

    private void processGeneList() throws IOException {
        log.info( "Loading genes to annotate from " + geneFileName );
        InputStream is = new FileInputStream( geneFileName );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        String line = null;
        GeneService geneService = ( GeneService ) getBean( "geneService" );
        TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
        Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
        }
        Collection<Gene> genes = new HashSet<Gene>();
        while ( ( line = br.readLine() ) != null ) {
            if ( StringUtils.isBlank( line ) ) {
                continue;
            }
            String[] arguments = StringUtils.split( line, '\t' );
            String gene = arguments[0];
            Gene g = geneService.findByOfficialSymbol( gene, taxon );
            if ( g == null ) {
                log.info( "Gene: " + gene + " not found." );
                continue;
            }
            genes.add( g );
        }
        log.info( "File contained " + genes.size() + " potential gene symbols" );
        int numProcessed = arrayDesignAnnotationService.generateAnnotationFile( new PrintWriter( System.out ), genes,
                OutputType.SHORT );
        log.info( "Processed " + numProcessed + " genes that were found" );
    }

    /**
     * 
     */
    private void processGenesForTaxon() {
        GeneService geneService = ( GeneService ) getBean( "geneService" );
        TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
        Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
        }
        Collection<Gene> genes = geneService.loadKnownGenes( taxon );
        log.info( "Taxon has " + genes.size() + " 'known' genes" );
        int numProcessed = arrayDesignAnnotationService.generateAnnotationFile( new PrintWriter( System.out ), genes,
                type );
        log.info( "Processed " + numProcessed + " genes that were found" );
    }

    /**
     * @param genesToInclude
     */
    private void processGenesIncluded( String genesToInclude ) {
        includePredictedGenes = false;

        if ( genesToInclude.equalsIgnoreCase( "all" ) ) includePredictedGenes = true;

    }

    /**
     * @param inputAd
     * @return
     * @throws IOException
     */
    private boolean processOneAD( ArrayDesign inputAd ) throws IOException {
        ArrayDesign ad = unlazifyArrayDesign( inputAd );

        log.info( "Processing AD: " + ad.getName() );

        String shortFileBaseName = ArrayDesignAnnotationService.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        File sf = ArrayDesignAnnotationService.getFileName( shortFileBaseName );
        String biocFileBaseName = ArrayDesignAnnotationService.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        File bf = ArrayDesignAnnotationService.getFileName( biocFileBaseName );
        String allparFileBaseName = ArrayDesignAnnotationService.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        File af = ArrayDesignAnnotationService.getFileName( allparFileBaseName );

        if ( !overWrite && sf.exists() && bf.exists() && af.exists() ) {
            log.info( "Files exist already, will not overwrite (use --overwrite option to override)" );
            return false;
        }

        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        log.info( "Starting getting probe specificity" );

        // lmd test

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity = compositeSequenceService
                .getGenesWithSpecificity( compositeSequences );

        log.info( "Done getting probe specificity" );

        boolean hasAtLeastOneGene = false;
        for ( CompositeSequence c : genesWithSpecificity.keySet() ) {
            if ( genesWithSpecificity.get( c ).isEmpty() ) {
                continue;
            }
            hasAtLeastOneGene = true;
            break;
        }

        if ( !hasAtLeastOneGene ) {
            log.warn( "No genes: " + ad + ", skipping" );
            return false;
        }

        boolean didAnything = false;
        if ( overWrite || !sf.exists() ) {
            processCompositeSequences( ad, shortFileBaseName, OutputType.SHORT, genesWithSpecificity );
            didAnything = true;
        } else {
            log.info( sf + " exists, will not overwrite" );
        }

        if ( overWrite || !bf.exists() ) {
            processCompositeSequences( ad, biocFileBaseName, OutputType.BIOPROCESS, genesWithSpecificity );
            didAnything = true;
        } else {
            log.info( bf + " exists, will not overwrite" );
        }

        if ( overWrite || !af.exists() ) {
            processCompositeSequences( ad, allparFileBaseName, OutputType.LONG, genesWithSpecificity );
            didAnything = true;
        } else {
            log.info( af + " exists, will not overwrite" );
        }
        return didAnything;
    }

    /**
     * @param n
     * @throws InterruptedException
     */
    private void waitForGeneOntologyReady() throws InterruptedException {
        int n = 0;
        log.info( "Waiting for Gene Ontology to load" );
        while ( !goService.isReady() ) {
            Thread.sleep( 500 );
            if ( ++n % 100 == 0 ) {
                log.info( "Waiting ..." );
            }
        }
    }
}
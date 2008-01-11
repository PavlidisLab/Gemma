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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.analysis.service.ArrayDesignAnnotationService.OutputType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.ontology.GeneOntologyService;

/**
 * Given an array design creates a Gene Ontology Annotation file
 * <p>
 * Given a batch file creates all the Annotation files for the AD's specified in the batch file
 * <p>
 * Given nothing creates annotation files for every AD that isn't subsumed or merged into another AD.
 * 
 * @author klc
 * @versio $Id$
 */
public class ArrayDesignGOAnnotationGeneratorCli extends ArrayDesignSequenceManipulatingCli {

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

    OutputType type;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option annotationFileOption = OptionBuilder.hasArg().withArgName( "Annotation file name" ).withDescription(
                "The name of the Annotation file to be generated [Default = Accession number]" ).withLongOpt(
                "annotation" ).create( 'f' );

        Option genesIncludedOption = OptionBuilder.hasArg().withArgName( "Genes to include" ).withDescription(
                "The type of genes that will be included: all or standard."
                        + " All includes predicted genes and probe aligned regions. "
                        + "Standard mode only includes known genes [Default = standard]" ).withLongOpt( "genes" )
                .create( 'g' );

        Option annotationType = OptionBuilder.hasArg().withArgName( "Type of annotation file" ).withDescription(
                "Which GO terms to add to the annotation file:  short, long, or bioprocess "
                        + "[Default=short (no parents)]. If you select bioprocess, parents are not included." )
                .withLongOpt( "type" ).create( 't' );

        Option fileLoading = OptionBuilder
                .hasArg()
                .withArgName( "Batch Generating of annotation files" )
                .withDescription(
                        "Use specified file for batch generating annotation files.  "
                                + "specified File format (per line): GPL,outputFileName,[short|long|biologicalprocess] Note:  Overrides -a,-t,-f command line options " )
                .withLongOpt( "load" ).create( 'l' );

        Option batchLoading = OptionBuilder
                .withArgName( "Generating all annotation files" )
                .withDescription(
                        "Generates annotation files for all Array Designs (omits ones that are subsumed or merged) uses accession as annotation file name."
                                + "Creates 3 zip files for each AD, no parents, parents, biological process. Overrides all other settings." )
                .withLongOpt( "batch" ).create( 'b' );

        Option overWrite = OptionBuilder.withArgName( "Overwrites existing files" ).withDescription(
                "If set will overwrite existing annotation files in the output directory" ).withLongOpt( "overwrite" )
                .create( 'o' );

        addOption( annotationFileOption );
        addOption( annotationType );
        addOption( fileLoading );
        addOption( genesIncludedOption );
        addOption( batchLoading );
        addOption( overWrite );

    }

    public static void main( String[] args ) {
        ArrayDesignGOAnnotationGeneratorCli p = new ArrayDesignGOAnnotationGeneratorCli();
        try {

            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
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

        int n = 0;
        try {
            log.info( "Waiting for Gene Ontology to load" );
            while ( !goService.isReady() ) {
                Thread.sleep( 500 );
                if ( ( n++ % 100 ) == 0 ) {
                    log.info( "Waiting ..." );
                }
            }

            if ( processAllADs ) {
                processAllADs();

            } else if ( batchFileName != null ) {
                processBatchFile( this.batchFileName );

            } else {
                ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );
                processAD( arrayDesign, this.fileName, type );
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /**
     * Goes over all the AD's in the database and creates annotation 3 annotation files for each AD that is not merged
     * into or subsumed by another AD. Uses the Accession ID (GPL???) for the name of the annotation file. Appends
     * noparents, bioProcess, allParents to the file name.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected void processAllADs() throws IOException {

        Collection<ArrayDesign> allADs = this.arrayDesignService.loadAll();

        this.includePredictedGenes = false;

        for ( ArrayDesign ad : allADs ) {

            if ( ad.getSubsumingArrayDesign() != null ) {
                log.info( "Skipping  " + ad.getName() + "  because it is subsumed by "
                        + ad.getSubsumingArrayDesign().getName() );
                continue;
            }

            if ( ad.getMergedInto() != null ) {
                log.info( "Skipping  " + ad.getName() + "  because it was merged into " + ad.getMergedInto().getName() );
                continue;
            }

            log.info( "Processing AD: " + ad.getName() );

            unlazifyArrayDesign( ad );
            Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
            Map<CompositeSequence, Map<PhysicalLocation, Collection<BlatAssociation>>> genesWithSpecificity = compositeSequenceService
                    .getGenesWithSpecificity( compositeSequences );

            processCompositeSequences( ad, ad.getShortName() + "_NoParents", OutputType.SHORT, genesWithSpecificity );

            processCompositeSequences( ad, ad.getShortName() + "_bioProcess", OutputType.BIOPROCESS,
                    genesWithSpecificity );

            processCompositeSequences( ad, ad.getShortName() + "_allParents", OutputType.LONG, genesWithSpecificity );

        }

    }

    /**
     * @param outputType
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected void processAD( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType ) throws IOException {

        log.info( "Loading gene information for " + arrayDesign );
        unlazifyArrayDesign( arrayDesign );

        Collection<CompositeSequence> compositeSequences = arrayDesign.getCompositeSequences();

        Map<CompositeSequence, Map<PhysicalLocation, Collection<BlatAssociation>>> genesWithSpecificity = compositeSequenceService
                .getGenesWithSpecificity( compositeSequences );

        log.info( "Preparing file" );
        processCompositeSequences( arrayDesign, fileBaseName, outputType, genesWithSpecificity );
    }

    /**
     * @param arrayDesign
     * @param fileBaseName
     * @param outputType
     * @param writer
     * @param genesWithSpecificity
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void processCompositeSequences( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType,
            Map<CompositeSequence, Map<PhysicalLocation, Collection<BlatAssociation>>> genesWithSpecificity )
            throws IOException {

        Writer writer = arrayDesignAnnotationService.initOutputFile( fileBaseName, this.overWrite );

        // if no writer then we should abort (this could happen in case where we don't want to overwrite files)
        if ( writer == null ) {
            log.info( arrayDesign.getName() + " annotation file already exits.  Skipping. " );
            return;
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
            log.info( "Created file:  " + fileBaseName + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX + " with "
                    + numProcessed + " values" );
            audit( arrayDesign, "Created file: " + fileBaseName + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX
                    + " with " + numProcessed + " values" );
        }
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignAnnotationFileEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    /**
     * @param fileName
     * @throws IOException used for batch processing
     */
    protected void processBatchFile( String fileName ) throws IOException {

        log.info( "Loading platforms to annotate from " + fileName );
        InputStream is = new FileInputStream( fileName );
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
            this.arrayDesignName = accession;
            ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

            try {
                processAD( arrayDesign, annotationFileName, type );
            } catch ( Exception e ) {
                log.error( "**** Exception while processing " + arrayDesignName + ": " + e.getMessage() + " ********" );
                log.error( e, e );
                cacheException( e );
                errorObjects.add( arrayDesignName + ": " + e.getMessage() );
                continue;
            }

        }

        summarizeProcessing();

    }

    /**
     * @param genesToInclude
     */
    private void processGenesIncluded( String genesToInclude ) {
        includePredictedGenes = false;

        if ( genesToInclude.equalsIgnoreCase( "all" ) ) includePredictedGenes = true;

    }

    @Override
    protected void processOptions() {

        // Turn on ontology loading.
        this.setOntologiesOn( true );

        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }

        if ( this.hasOption( 't' ) ) {
            this.type = OutputType.valueOf( this.getOptionValue( 't' ).toUpperCase() );
        }

        if ( this.hasOption( 'l' ) ) {
            this.batchFileName = this.getOptionValue( 'l' );
        }

        if ( this.hasOption( 'b' ) ) {
            this.processAllADs = true;
        }

        if ( this.hasOption( 'g' ) ) processGenesIncluded( this.getOptionValue( 'g' ) );

        if ( this.hasOption( 'o' ) ) this.overWrite = true;

        super.processOptions();

        this.arrayDesignAnnotationService = ( ArrayDesignAnnotationService ) getBean( "arrayDesignAnnotationService" );
        this.goService = ( GeneOntologyService ) getBean( "geneOntologyService" );
        this.compositeSequenceService = ( CompositeSequenceService ) getBean( "compositeSequenceService" );
    }
}
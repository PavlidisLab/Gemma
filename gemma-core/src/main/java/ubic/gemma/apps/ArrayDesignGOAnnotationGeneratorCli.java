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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.genome.CompositeSequenceGeneMapperService;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Given an array design creates a Gene Ontology Annotation file
 * <p>
 * 
 * @author klc
 */
public class ArrayDesignGOAnnotationGeneratorCli extends ArrayDesignSequenceManipulatingCli {

    // constants
    final String SHORT = "short";
    final String LONG = "long";
    final String BIOPROCESS = "biologicalprocess";
    final String BIOPROCESS_ACCESSION = "GO:0008150";
   

    // services
    Gene2GOAssociationService gene2GoAssociationService;
    CompositeSequenceGeneMapperService compositeSequenceGeneMapperService;
    GeneService geneService;
    OntologyEntryService oeService;

 
    Collection<Exception> exceptions;

    // file info
    String batchFileName;
    Writer writer;
    String fileName;

    // types
    boolean shortAnnotations;
    boolean longAnnotations;
    boolean biologicalProcessAnnotations;

    // summary info
    long linesWritten;
    long genesSkipped;

    
    
     public void initForTests(String[] args){
          processCommandLine( "Test", args );
          processOptions();               
    }
    
    
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
                "Optional: The name of the Annotation file to be generated" ).withLongOpt( "annotation" ).create( 'f' );

        Option annotationType = OptionBuilder.hasArg().withArgName( "Type of annotation file" ).withDescription(
                "Optional: Which go terms to add to the annotation file:  short, long, biologicalprocess" )
                .withLongOpt( "type" ).create( 't' );

        Option fileLoading = OptionBuilder
                .hasArg()
                .withArgName( "Batch Generating of annotation files" )
                .withDescription(
                        "Optional: uses specified file for batch generating annotation files. file format: GPL,outputFileName,[short|long|biologicalprocess] Note:  Overrides -a,-t,-f command line options " )
                .withLongOpt( "load" ).create( 'l' );

        addOption( annotationFileOption );
        addOption( annotationType );
        addOption( fileLoading );

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

        try {
            if ( batchFileName == null )
                processAD();
            else
                processBatchFile( this.batchFileName );

        } catch ( IOException e ) {
            log.error( "Error writing to file " + e );
            store( e );
        }

        return null;
    }

    protected void processAD() throws IOException {

        ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );
        unlazifyArrayDesign( arrayDesign );

        Collection<CompositeSequence> cs = arrayDesign.getCompositeSequences();

        log.info( arrayDesignName + " has " + cs.size() + " composite sequences" );

        initFile( fileName );

        generateAnnotationFile( cs );

        writer.flush();
        writer.close();

        log.info( "Finished processing platform: " + arrayDesignName );
        log.info( "Created file:  " + fileName + " with " + linesWritten + " lines" );

    }

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

            String gpl = arguments[0];
            String annotationFileName = arguments[1];
            String type = arguments[2];

            // Check the syntax of the given line
            if ( ( gpl == null ) || StringUtils.isBlank( gpl ) ) {
                log.warn( "Incorrect line format in Batch Annotation file: Line " + lineNumber
                        + "Platform is required: " + line );
                log.warn( "Unable to process that line. Skipping to next." );
                continue;
            }
            if ( ( annotationFileName == null ) || StringUtils.isBlank( annotationFileName ) ) {
                annotationFileName = gpl;
                log.warn( "No annotation file name specified on line: " + lineNumber
                        + " Using platform name as default annotation file name" );
            }
            if ( ( type == null ) || StringUtils.isBlank( type ) ) {
                type = SHORT;
                log.warn( "No type specifed for line: " + lineNumber + " Defaulting to short" );
            }

            // need to set these so processing ad works correctly (todo: make processtype take all 3 parameter)
            this.arrayDesignName = gpl;
            this.fileName = annotationFileName;
            processType( type );

            try {
                processAD();
            } catch ( Exception e ) {
                log.warn( "Error processing platform. " + e );
                store( e );
                continue;
            }

        }

    }

    protected void initFile( String adName ) throws IOException {
        // write into file
        log.info( "Creating new annotation file " + adName + " \n" );

        File f = new File( adName + ".an.txt" );
        f.delete();
        f.createNewFile();
        writer = new FileWriter( f );
        writer.write( "Probe ID \t Gene \t Description \t GO Terms \n" );

    }

    protected void generateAnnotationFile( Collection<CompositeSequence> cs ) throws IOException {

        linesWritten = 0;

        for ( CompositeSequence sequence : cs ) {

            Collection<Gene> genes = compositeSequenceGeneMapperService.getGenesForCompositeSequence( sequence );

            if ( ( genes == null ) || ( genes.isEmpty() ) ) {
                generateAnnotationFileLine( sequence.getName(), "", "", null );
                continue;
            }

            // actually the collection gotten back is a collection of proxies which causes issues. Need to reload the
            // genes from the db.
            Collection<Long> geneIds = new ArrayList<Long>();
            for ( Gene g : genes )
                geneIds.add( g.getId() );

            genes = geneService.load( geneIds );

            String geneNames = "";
            String geneDescriptions = "";
            Collection<OntologyEntry> goTerms = new ArrayList<OntologyEntry>();

            // Might be mulitple genes for a given cs. Need to hash it into one.
            for ( Iterator iter = genes.iterator(); iter.hasNext(); ) {

                Gene gene = ( Gene ) iter.next();

                if ( gene == null ) continue;

                // Don't add gemmaGene info to annotation file
                if ( ( gene instanceof ProbeAlignedRegion ) || ( gene instanceof PredictedGene ) ) {
                    log.info( "Gene:  " + gene.getOfficialSymbol()
                            + "  not included in annotations because it is a probeAligedRegion or predictedGene" );
                    continue;
                }

                Collection<OntologyEntry> terms = getGoTerms( gene );
                if ( ( terms != null ) && !( terms.isEmpty() ) ) goTerms.addAll( terms );

                if ( gene.getOfficialSymbol() != null ) geneNames += gene.getOfficialSymbol();

                if ( gene.getOfficialName() != null ) geneDescriptions += gene.getOfficialName();

                if ( iter.hasNext() ) {
                    if ( gene.getOfficialSymbol() != null ) geneNames += "|";
                    if ( gene.getOfficialName() != null ) geneDescriptions += "|";
                }

            }

            generateAnnotationFileLine( sequence.getName(), geneNames, geneDescriptions, goTerms );

        }
    }

    protected void generateAnnotationFileLine( String probeId, String gene, String description,
            Collection<OntologyEntry> goTerms ) throws IOException {

        linesWritten++;
        log.info( "Generating line for annotation file  \n" );

        writer.write( probeId + "\t" + gene + "\t" + description + "\t" );

        if ( ( goTerms == null ) || goTerms.isEmpty() ) {
            writer.write( "\n" );
            writer.flush();
            return;
        }

        for ( Iterator iter = goTerms.iterator(); iter.hasNext(); ) {
            OntologyEntry oe = ( OntologyEntry ) iter.next();
            writer.write( oe.getAccession() );

            if ( iter.hasNext() ) writer.write( "|" );
        }

        writer.write( "\n" );
        writer.flush();

    }

    protected Collection<OntologyEntry> getGoTerms( Gene gene ) {

        Collection<OntologyEntry> ontos = gene2GoAssociationService.findByGene( gene );

        if ( ( ontos == null ) || ( ontos.size() == 0 ) ) return ontos;

        if ( this.shortAnnotations ) return ontos;

        Map<OntologyEntry, Collection> ontoMap = oeService.getAllParents( ontos );

        if ( this.longAnnotations ) {
            for ( Collection<OntologyEntry> oes : ontoMap.values() )
                ontos.addAll( oes );

            return ontos;
        }

        if ( this.biologicalProcessAnnotations ) {

            OntologyEntry bioprocess = oeService.findByAccession( BIOPROCESS_ACCESSION );

            ontos = new ArrayList<OntologyEntry>();

            for ( OntologyEntry key : ontoMap.keySet() ) {
                Collection<OntologyEntry> values = ontoMap.get( key );

                if ( !values.contains( bioprocess ) ) continue;

                values.remove( bioprocess ); // we don't want the root term in all the go terms
                ontos.addAll( values );
                ontos.add( key );
            }
            return ontos;

        }

        return null;

    }

//     Map<OntologyEntry, Collection> getParents( Collection<OntologyEntry> children ) {
//
//        if ( ( children == null ) || ( children.isEmpty() ) ) return null;
//
//        Map<OntologyEntry, Collection> parents = new HashMap<OntologyEntry, Collection>();
//        Collection<OntologyEntry> notCached = new ArrayList<OntologyEntry>();
//
//        for ( OntologyEntry oe : children ) {
//
//            if ( ontologyTreeCache.containsKey( oe ) )
//                parents.put( oe, ontologyTreeCache.get( oe ) );
//
//            else
//                notCached.add( oe );
//
//        }
//
//        if ( notCached.isEmpty() ) return parents;
//
//        Map<OntologyEntry, Collection> newlyFound = oeService.getParents( notCached );
//
//        cache( newlyFound );
//        parents.putAll( newlyFound );
//
//        return parents;
//    }



    private void processType( String type ) {

        shortAnnotations = false;
        longAnnotations = false;
        biologicalProcessAnnotations = false;

        if ( type.equalsIgnoreCase( LONG ) )
            longAnnotations = true;
        else if ( type.equalsIgnoreCase( BIOPROCESS ) )
            biologicalProcessAnnotations = true;
        else
            // ( type.equalsIgnoreCase( SHORT ) )
            shortAnnotations = true;

    }

    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }

        if ( this.hasOption( 't' ) ) {
            processType( this.getOptionValue( 't' ) );
        }

        if ( this.hasOption( 'l' ) ) {
            this.batchFileName = this.getOptionValue( 'l' );
        }
      
        gene2GoAssociationService = ( Gene2GOAssociationService ) this.getBean( "gene2GOAssociationService" );

        compositeSequenceGeneMapperService = ( CompositeSequenceGeneMapperService ) this
                .getBean( "compositeSequenceGeneMapperService" );
        geneService = ( GeneService ) this.getBean( "geneService" );

        oeService = ( OntologyEntryService ) this.getBean( "ontologyEntryService" );
       
        exceptions = new ArrayList<Exception>();

    }

    protected void store( Exception e ) {
        exceptions.add( e );
    }

}
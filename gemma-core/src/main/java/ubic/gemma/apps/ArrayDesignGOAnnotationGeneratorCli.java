package ubic.gemma.apps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.genome.CompositeSequenceGeneMapperService;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.OntologyEntry;
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

    Gene2GOAssociationService gene2GoAssociationService;
    CompositeSequenceGeneMapperService compositeSequenceGeneMapperService;
    GeneService geneService;

    Writer writer;
    String fileName;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option annotationFileOption = OptionBuilder.hasArg().isRequired().withArgName( "Annotation file name" )
                .withDescription( "The name of the Annotation file to be generated" ).withLongOpt( "annotation" )
                .create( 'f' );

        addOption( annotationFileOption );

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

        ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );
        unlazifyArrayDesign( arrayDesign );

        Collection<CompositeSequence> cs = arrayDesign.getCompositeSequences();

        try {

            initFile( fileName );

            generateAnnotationFile( cs );

            writer.flush();
            writer.close();

        } catch ( IOException e ) {
            log.error( "Error writing to file " + e );
            return e;
        }

        return null;
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

        for ( CompositeSequence sequence : cs ) {

            Collection<Gene> genes = compositeSequenceGeneMapperService.getGenesForCompositeSequence( sequence );
            if ( ( genes == null ) || ( genes.isEmpty() ) ) continue;

            // actually the collection gotten back is a collection of proxies which causes issues. Need to reload the
            // genes from the db.
            Collection<Long> geneIds = new HashSet<Long>();
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

                goTerms.addAll( gene2GoAssociationService.findByGene( gene ) );

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

        log.info( "Generating line for annotation file  \n" );

        writer.write( probeId + "\t" + gene + "\t" + description + "\t" );

        for ( Iterator iter = goTerms.iterator(); iter.hasNext(); ) {
            OntologyEntry oe = ( OntologyEntry ) iter.next();
            writer.write( oe.getAccession() );

            if ( iter.hasNext() ) writer.write( "|" );
        }

        writer.write( "\n" );
        writer.flush();
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }

        gene2GoAssociationService = ( Gene2GOAssociationService ) this.getBean( "gene2GOAssociationService" );

        compositeSequenceGeneMapperService = ( CompositeSequenceGeneMapperService ) this
                .getBean( "compositeSequenceGeneMapperService" );
        geneService = ( GeneService ) this.getBean( "geneService" );

    }

}

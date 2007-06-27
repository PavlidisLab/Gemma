package ubic.gemma.apps;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.GenePair;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Calculate the effect size
 * 
 * @author xwan
 */
public class EffectSizeCalculationCli extends AbstractSpringAwareCLI {
    private static final int MINIMUM_SAMPLE_SIZE = 3;

    private ExpressionExperimentService eeService;

    private GeneService geneService;

    private EffectSizeService effectSizeService;

    private String geneList = null;

    private String outputFile = null;

    private String matrixFile = null;

    private int stringency = 3;

    private Taxon taxon;

    private CorrelationEffectMetaAnalysis metaAnalysis = new CorrelationEffectMetaAnalysis( true, false );

    private ByteArrayConverter bac = new ByteArrayConverter();

    public EffectSizeCalculationCli() {
        super();
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        effectSizeService = ( EffectSizeService ) this.getBean( "effectSizeService" );
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "geneFile" ).withDescription(
                "Short names of the genes to analyze" ).withLongOpt( "geneFile" ).create( 'g' );
        addOption( geneFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes to analyze" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        // Option matrixFile = OptionBuilder.hasArg().withArgName( "Bit
        // Matrixfile" ).isRequired().withDescription(
        // "The file for saving bit matrix" ).withLongOpt( "matrixfile"
        // ).create( 'm' );
        // addOption( matrixFile );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "File for saving the correlation data" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outputFileOption );
        Option stringencyFileOption = OptionBuilder.hasArg().withArgName( "stringency" ).withDescription(
                "The stringency for the number of co-expression link(Default 3)" ).withLongOpt( "stringency" ).create(
                's' );
        addOption( stringencyFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.geneList = getOptionValue( 'g' );
        }
        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            taxon = getTaxon( taxonName );
        }
        if ( hasOption( 'o' ) ) {
            this.outputFile = getOptionValue( 'o' );
        }
        if ( hasOption( 's' ) ) {
            this.stringency = Integer.parseInt( getOptionValue( 's' ) );
        }
        if ( hasOption( 'm' ) ) {
            this.matrixFile = getOptionValue( 'm' );
        }
    }

    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "No Taxon found!" );
        }
        return taxon;
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exc = processCommandLine( "EffectSizeCalculation ", args );
        if ( exc != null ) {
            return exc;
        }
        StopWatch watch = new StopWatch();
        watch.start();
        // try {
        // linkFinder.fromFile( this.matrixFile, null );
        // } catch ( IOException e ) {
        // log.info( "Couldn't load the data from the files " );
        // return e;
        // }
        // watch.stop();
        // log.info( "Spend " + watch.getTime() / 1000 + " to load the data
        // matrix" );

        // First time using the following function to save candidate gene pair
        // into a file
        // saveGenePairs("genepairs.txt");
        Collection<GenePair> genePairs;
        try {
            // genePairs = readGenePairs( geneList );
            genePairs = effectSizeService.readGenePairsByID( geneList, stringency );
        } catch ( IOException e ) {
            return e;
        }
        // Map<Long, Gene> geneMap = getGeneMap( genePairs.getGeneIDs() );
        Collection<ExpressionExperiment> EEs = eeService.findByTaxon( taxon );
        effectSizeService.calculateEffectSize( EEs, genePairs );
        return null;
    }

}

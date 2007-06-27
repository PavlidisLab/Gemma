package ubic.gemma.apps;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.GenePair;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class NoCorrelationAnalysisCLI extends AbstractSpringAwareCLI {
    private String geneListFile;
    private String outFile;
    private Taxon taxon;
    private EffectSizeService effectSizeService;
    private ExpressionExperimentService eeService;

    public NoCorrelationAnalysisCLI() {
        super();
        effectSizeService = ( EffectSizeService ) this.getBean( "effectSizeService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
    }

    @Override
    protected void buildOptions() {
        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "geneFile" ).withDescription(
                "File containing list of gene pair offical symbols" ).withLongOpt( "geneFile" ).create( 'g' );
        addOption( geneFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "File for saving the correlation data" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outputFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.geneListFile = getOptionValue( 'g' );
        }
        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            taxon = Taxon.Factory.newInstance();
            taxon.setCommonName( taxonName );
            TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
            taxon = taxonService.find( taxon );
            if ( taxon == null ) {
                log.info( "No Taxon found!" );
            }
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exc = processCommandLine( "NoCorrelationAnalysis", args );
        if ( exc != null ) {
            return exc;
        }
        Collection<GenePair> genePairs;
        try {
            genePairs = effectSizeService.readGenePairsByOfficialSymbol( geneListFile );
        } catch ( IOException e ) {
            return e;
        }
        Collection<ExpressionExperiment> EEs = eeService.findByTaxon( taxon );
        effectSizeService.calculateCorrelations( EEs, genePairs );
        for (GenePair genePair: genePairs) {
            if (genePair.getMaxCorrelation() < 0.2) {
                System.out.println(genePair.getFirstId() + "\t" + genePair.getSecondId() + "\t" + genePair.getMaxCorrelation());
            }
        }
        return null;
    }

    public static void main( String[] args ) {
        NoCorrelationAnalysisCLI analysis = new NoCorrelationAnalysisCLI();
        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting No Correlation Analysis" );
        Exception exc = analysis.doWork( args );
        if ( exc != null ) {
            log.error( exc.getMessage() );
        }
        log.info( "Finished analysis in " + watch.getTime() / 1000 + " seconds" );
    }
}
